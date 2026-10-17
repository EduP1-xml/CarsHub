package carshub.com.br.backend.services.geocodificacao;

import carshub.com.br.backend.config.ClientesGeocodificacao;
import carshub.com.br.backend.exceptions.ChaveGeocodificacaoException;
import carshub.com.br.backend.exceptions.EnderecoNaoEncontradoException;
import carshub.com.br.backend.exceptions.FalhaGeocodificacaoException;
import carshub.com.br.backend.exceptions.TempoLimiteGeocodificacaoException;
import carshub.com.br.backend.models.dtos.geocodificacao.ComponenteEnderecoDTO;
import carshub.com.br.backend.models.dtos.geocodificacao.LocalizacaoGeocodificadaDTO;
import carshub.com.br.backend.models.dtos.geocodificacao.RespostaGeocodificacaoDTO;
import carshub.com.br.backend.models.dtos.geocodificacao.ResultadoGeocodificacaoDTO;
import carshub.com.br.backend.models.dtos.servicos.EnderecoOcorrenciaDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * DOIS CAMINHOS, com regras de falha diferentes de proposito:
 *   geocodificarEndereco        -> BLOQUEANTE  (endereco -> coordenadas);
 *   buscarEnderecoDasCoordenadas-> BEST-EFFORT (coordenadas -> endereco), devolve
 *                                  null quando nao consegue e nunca lanca.
 *
 * 2 clientes: 3 s direto / 800 ms reverso),
 * retry com backoff + disjuntor
 * cota mensal persistida,
 * endereco estruturado + placeId e normalizacao WGS84 de 7 casas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeocodificacaoService {

    /** Casas decimais do WGS84 usado pelo Maps (7 casas ~= 1,1 cm). */
    private static final int casasDecimaisWgs84 = 7;

    private static final String statusOk = "OK";
    private static final String statusSemResultado = "ZERO_RESULTS";
    private static final String statusChaveRecusada = "REQUEST_DENIED";
    private static final String statusLimiteConsulta = "OVER_QUERY_LIMIT";
    private static final String statusLimiteDiario = "OVER_DAILY_LIMIT";

    private static final String parametroEndereco = "address";
    private static final String parametroCoordenadas = "latlng";

    private final ClientesGeocodificacao clientesGeocodificacao;
    private final ContadorCotaGeocodificacao contadorCota;

    @Value("${app.geocodificacao.chave:}")
    private String chaveGeocodificacao;

    @Value("${app.geocodificacao.tentativasExtras:2}")
    private int tentativasExtras;

    @Value("${app.geocodificacao.esperaEntreTentativasMs:250}")
    private long esperaEntreTentativasMs;

    @Value("${app.geocodificacao.falhasParaAbrirCircuito:5}")
    private int falhasParaAbrirCircuito;

    @Value("${app.geocodificacao.segundosCircuitoAberto:60}")
    private long segundosCircuitoAberto;

    // Estado do disjuntor (A4) — em memoria, uma instancia.
    private final AtomicInteger falhasConsecutivas = new AtomicInteger();
    private final AtomicLong circuitoAbertoAte = new AtomicLong();

    // ==================================================================
    // ENDERECO -> COORDENADAS (bloqueante)
    // ==================================================================

    public LocalizacaoGeocodificadaDTO geocodificarEndereco(String endereco) {
        if (endereco == null || endereco.isBlank()) {
            throw new IllegalArgumentException("Informe o endereço para localizar o ponto no mapa.");
        }

        validarChaveConfigurada();

        RespostaGeocodificacaoDTO resposta = consultarComResiliencia(parametroEndereco, endereco, true);
        validarStatus(resposta);
        return extrairPrimeiraLocalizacao(resposta);
    }

    // ==================================================================
    // COORDENADAS -> ENDERECO (best-effort)
    // ==================================================================

    /**
     * Devolve a localizacao completa (coordenada + endereco formatado + endereco
     * estruturado) ou NULL quando a geocodificacao reversa nao esta disponivel.
     * NUNCA lanca: o chamado tem de continuar valido sem endereco legivel.
     */
    public LocalizacaoGeocodificadaDTO buscarEnderecoDasCoordenadas(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }

        if (chaveNaoConfigurada()) {
            log.debug("Geocodificação reversa ignorada: app.geocodificacao.chave não configurada.");
            return null;
        }

        try {
            String coordenadas = latitude + "," + longitude;
            RespostaGeocodificacaoDTO resposta = consultarComResiliencia(parametroCoordenadas, coordenadas, false);

            if (resposta == null) {
                return null;
            }

            if (resposta.status() != null && !statusOk.equals(resposta.status())) {
                log.debug("Geocodificação reversa devolveu o status {} para ({}, {}).",
                        resposta.status(), latitude, longitude);
                return null;
            }

            List<ResultadoGeocodificacaoDTO> resultados = resposta.results();
            if (resultados == null || resultados.isEmpty()) {
                return null;
            }

            ResultadoGeocodificacaoDTO primeiro = resultados.get(0);

            return new LocalizacaoGeocodificadaDTO(
                    normalizarCoordenada(latitude),
                    normalizarCoordenada(longitude),
                    primeiro.formattedAddress(),
                    montarEnderecoEstruturado(primeiro));
        }
        catch (RuntimeException erro) {
            log.warn("Geocodificação reversa indisponível para ({}, {}): {}", latitude, longitude, erro.getMessage());
            return null;
        }
    }

    // ==================================================================
    // CHAMADA HTTP COM RETRY E DISJUNTOR (A4)
    // ==================================================================

    private RespostaGeocodificacaoDTO consultarComResiliencia(String parametro, String valor, boolean direto) {
        verificarCircuito();

        int tentativa = 0;

        while (true) {
            try {
                contadorCota.registrarRequisicao();
                RespostaGeocodificacaoDTO resposta = executarChamada(parametro, valor, direto);
                registrarSucesso();
                return resposta;
            }
            catch (ChaveGeocodificacaoException erro) {
                // Erro de CONFIGURACAO: repetir nao resolve e nao pode abrir o disjuntor.
                throw erro;
            }
            catch (FalhaGeocodificacaoException erro) {
                // Cobre TempoLimiteGeocodificacaoException (subclasse): um catch so,
                // porque multi-catch com super e subclasse nao compila.
                registrarFalha();

                if (tentativa >= tentativasExtras) {
                    throw erro;
                }

                tentativa++;
                log.warn("Geocodificação falhou (tentativa {} de {}): {}. Repetindo...",
                        tentativa, tentativasExtras + 1, erro.getMessage());
                esperarAntesDeRepetir(tentativa);
            }
        }
    }

    private RespostaGeocodificacaoDTO executarChamada(String parametro, String valor, boolean direto) {
        RestClient cliente = direto
                ? clientesGeocodificacao.clienteDireto()
                : clientesGeocodificacao.clienteReverso();

        try {
            return cliente.get()
                    .uri(construtor -> construtor
                            .queryParam(parametro, valor)
                            .queryParam("key", chaveGeocodificacao)
                            .queryParam("language", "pt-BR")
                            .queryParam("region", "br")
                            .build())
                    .retrieve()
                    .body(RespostaGeocodificacaoDTO.class);
        }
        catch (RestClientResponseException erro) {
            throw traduzirRespostaHttp(erro);
        }
        catch (ResourceAccessException erro) {
            throw traduzirFalhaDeRede(erro);
        }
        catch (RestClientException erro) {
            throw new FalhaGeocodificacaoException(
                    "Falha ao consultar o serviço de geocodificação: " + erro.getMessage(), erro);
        }
    }

    private void esperarAntesDeRepetir(int tentativa) {
        try {
            Thread.sleep(esperaEntreTentativasMs * tentativa);
        }
        catch (InterruptedException interrompida) {
            Thread.currentThread().interrupt();
            throw new FalhaGeocodificacaoException("A consulta de geocodificação foi interrompida.", interrompida);
        }
    }

    private void verificarCircuito() {
        long abertoAte = circuitoAbertoAte.get();

        if (abertoAte > System.currentTimeMillis()) {
            long segundosRestantes = (abertoAte - System.currentTimeMillis()) / 1000 + 1;
            throw new FalhaGeocodificacaoException(
                    "O serviço de mapas está temporariamente fora do ar. Tente novamente em "
                            + segundosRestantes + " segundos ou envie latitude e longitude do GPS.");
        }
    }

    private void registrarFalha() {
        int falhas = falhasConsecutivas.incrementAndGet();

        if (falhas >= falhasParaAbrirCircuito) {
            circuitoAbertoAte.set(System.currentTimeMillis() + (segundosCircuitoAberto * 1000));
            falhasConsecutivas.set(0);
            log.error("Geocodificação: {} falhas consecutivas — disjuntor ABERTO por {} segundos.",
                    falhas, segundosCircuitoAberto);
        }
    }

    private void registrarSucesso() {
        falhasConsecutivas.set(0);
        circuitoAbertoAte.set(0);
    }

    // ==================================================================
    // TRADUCAO DE FALHAS
    // ==================================================================

    private RuntimeException traduzirRespostaHttp(RestClientResponseException erro) {
        int status = erro.getStatusCode().value();

        if (status == HttpStatus.UNAUTHORIZED.value() || status == HttpStatus.FORBIDDEN.value()) {
            return new ChaveGeocodificacaoException(
                    "O serviço de geocodificação recusou a chave de API (HTTP " + status + ").", erro);
        }

        return new FalhaGeocodificacaoException(
                "O serviço de geocodificação respondeu com o HTTP " + status + ".", erro);
    }

    private RuntimeException traduzirFalhaDeRede(ResourceAccessException erro) {
        Throwable causa = erro.getMostSpecificCause();

        if (causa instanceof HttpTimeoutException || causa instanceof SocketTimeoutException
                || mensagemIndicaTempoLimite(causa)) {
            return new TempoLimiteGeocodificacaoException(
                    "O serviço de geocodificação não respondeu dentro do tempo limite configurado.", erro);
        }

        return new FalhaGeocodificacaoException("Não foi possível conectar ao serviço de geocodificação.", erro);
    }

    private boolean mensagemIndicaTempoLimite(Throwable causa) {
        if (causa == null || causa.getMessage() == null) {
            return false;
        }

        String mensagem = causa.getMessage().toLowerCase(Locale.ROOT);
        return mensagem.contains("timed out") || mensagem.contains("timeout");
    }

    // ==================================================================
    // VALIDACOES DA RESPOSTA
    // ==================================================================

    private void validarChaveConfigurada() {
        if (chaveNaoConfigurada()) {
            throw new ChaveGeocodificacaoException(
                    "A chave do Google Geocoding não está configurada (app.geocodificacao.chave). "
                            + "Enquanto isso, envie latitude e longitude do dispositivo.");
        }
    }

    private boolean chaveNaoConfigurada() {
        return chaveGeocodificacao == null || chaveGeocodificacao.isBlank();
    }

    private void validarStatus(RespostaGeocodificacaoDTO resposta) {
        if (resposta == null) {
            throw new FalhaGeocodificacaoException("O serviço de geocodificação não devolveu conteúdo.");
        }

        String status = resposta.status();

        if (status == null || statusOk.equals(status)) {
            return;
        }

        if (statusSemResultado.equals(status)) {
            throw new EnderecoNaoEncontradoException(
                    "Não encontramos esse endereço no mapa. Revise o texto informado ou envie latitude e longitude do GPS.");
        }

        if (statusChaveRecusada.equals(status)) {
            throw new ChaveGeocodificacaoException("O serviço de geocodificação recusou a chave de API configurada.");
        }

        if (statusLimiteConsulta.equals(status) || statusLimiteDiario.equals(status)) {
            throw new FalhaGeocodificacaoException("O serviço de geocodificação está sem cota disponível no momento.");
        }

        throw new FalhaGeocodificacaoException("A geocodificação falhou com o status " + status + ".");
    }

    private LocalizacaoGeocodificadaDTO extrairPrimeiraLocalizacao(RespostaGeocodificacaoDTO resposta) {
        List<ResultadoGeocodificacaoDTO> resultados = resposta.results();

        if (resultados == null || resultados.isEmpty()) {
            throw new EnderecoNaoEncontradoException(
                    "Não encontramos esse endereço no mapa. Revise o texto informado ou envie latitude e longitude do GPS.");
        }

        ResultadoGeocodificacaoDTO primeiro = resultados.get(0);

        if (primeiro.geometry() == null || primeiro.geometry().location() == null
                || primeiro.geometry().location().lat() == null || primeiro.geometry().location().lng() == null) {
            throw new FalhaGeocodificacaoException("O serviço de geocodificação devolveu um resultado sem coordenadas.");
        }

        return new LocalizacaoGeocodificadaDTO(
                normalizarCoordenada(primeiro.geometry().location().lat()),
                normalizarCoordenada(primeiro.geometry().location().lng()),
                primeiro.formattedAddress(),
                montarEnderecoEstruturado(primeiro));
    }

    // ==================================================================
    // ENDERECO ESTRUTURADO (A6) + PRECISAO WGS84
    // ==================================================================

    /** Normaliza para 7 casas decimais (WGS84), eliminando ruido de ponto flutuante. */
    public static Double normalizarCoordenada(Double coordenada) {
        if (coordenada == null) {
            return null;
        }

        return BigDecimal.valueOf(coordenada)
                .setScale(casasDecimaisWgs84, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private EnderecoOcorrenciaDTO montarEnderecoEstruturado(ResultadoGeocodificacaoDTO resultado) {
        List<ComponenteEnderecoDTO> componentes = resultado.addressComponents();

        return new EnderecoOcorrenciaDTO(
                buscarComponente(componentes, "route"),
                buscarComponente(componentes, "street_number"),
                buscarComponente(componentes, "sublocality_level_1", "sublocality", "neighborhood"),
                buscarComponente(componentes, "administrative_area_level_2", "locality"),
                buscarComponente(componentes, "administrative_area_level_1"),
                buscarComponente(componentes, "postal_code"),
                resultado.placeId());
    }

    /** O Google devolve mais de um tipo por componente: tentamos na ordem dada. */
    private String buscarComponente(List<ComponenteEnderecoDTO> componentes, String... tiposProcurados) {
        if (componentes == null) {
            return null;
        }

        for (String tipo : tiposProcurados) {
            for (ComponenteEnderecoDTO componente : componentes) {
                if (componente.types() != null && componente.types().contains(tipo)) {
                    return (componente.longName() != null) ? componente.longName() : componente.shortName();
                }
            }
        }

        return null;
    }
}