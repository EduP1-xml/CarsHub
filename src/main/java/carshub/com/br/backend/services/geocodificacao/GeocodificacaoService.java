package carshub.com.br.backend.services.geocodificacao;

import carshub.com.br.backend.exceptions.ChaveGeocodificacaoException;
import carshub.com.br.backend.exceptions.EnderecoNaoEncontradoException;
import carshub.com.br.backend.exceptions.FalhaGeocodificacaoException;
import carshub.com.br.backend.exceptions.TempoLimiteGeocodificacaoException;
import carshub.com.br.backend.models.dtos.geocodificacao.LocalizacaoGeocodificadaDTO;
import carshub.com.br.backend.models.dtos.geocodificacao.RespostaGeocodificacaoDTO;
import carshub.com.br.backend.models.dtos.geocodificacao.ResultadoGeocodificacaoDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.List;
import java.util.Locale;

/**
 * Integracao com o Google Geocoding API — a chamada e SEMPRE do back, porque a
 * chave nao pode ir para o front (o JavaScript API do front so desenha o mapa).
 *
 * Dois caminhos, com regras de falha DIFERENTES de proposito:
 *
 *   geocodificarEndereco  (direto: endereco -> coordenadas)
 *       BLOQUEANTE. Sem coordenada nao existe ocorrencia. Falha vira
 *       502 (chave), 504 (timeout), 503 (resto) ou 422 (endereco inexistente).
 *
 *   buscarEnderecoFormatado (reverso: coordenadas -> endereco)
 *       BEST-EFFORT. Serve apenas para o prestador LER onde e o chamado. Nunca
 *       lanca excecao: devolve null e loga WARN, para que uma queda do Google
 *       nao derrube um pedido de SOS.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GeocodificacaoService {

    private static final String statusOk = "OK";
    private static final String statusSemResultado = "ZERO_RESULTS";
    private static final String statusChaveRecusada = "REQUEST_DENIED";
    private static final String statusLimiteConsulta = "OVER_QUERY_LIMIT";
    private static final String statusLimiteDiario = "OVER_DAILY_LIMIT";

    private static final String parametroEndereco = "address";
    private static final String parametroCoordenadas = "latlng";

    private final RestClient clienteGeocodificacao;

    @Value("${app.geocodificacao.chave:}")
    private String chaveGeocodificacao;

    // ==================================================================
    // ENDERECO -> COORDENADAS (bloqueante)
    // ==================================================================

    public LocalizacaoGeocodificadaDTO geocodificarEndereco(String endereco) {
        if (endereco == null || endereco.isBlank()) {
            throw new IllegalArgumentException("Informe o endereço para localizar o ponto no mapa.");
        }

        validarChaveConfigurada();

        RespostaGeocodificacaoDTO resposta = consultarApi(parametroEndereco, endereco);
        validarStatus(resposta);
        return extrairPrimeiraLocalizacao(resposta);
    }

    // ==================================================================
    // COORDENADAS -> ENDERECO (best-effort, nunca lanca excecao)
    // ==================================================================

    public String buscarEnderecoFormatado(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return null;
        }

        if (chaveNaoConfigurada()) {
            log.debug("Geocodificação reversa ignorada: app.geocodificacao.chave não configurada.");
            return null;
        }

        try {
            String coordenadas = latitude + "," + longitude;
            RespostaGeocodificacaoDTO resposta = consultarApi(parametroCoordenadas, coordenadas);

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

            return resultados.get(0).formattedAddress();
        }
        catch (RuntimeException erro) {
            // Um endereco legivel e conveniencia, nao requisito: o chamado segue valido.
            log.warn("Geocodificação reversa indisponível para ({}, {}): {}", latitude, longitude, erro.getMessage());
            return null;
        }
    }

    // ==================================================================
    // CHAMADA HTTP
    // ==================================================================

    private RespostaGeocodificacaoDTO consultarApi(String parametro, String valor) {
        try {
            return clienteGeocodificacao.get()
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

    // ==================================================================
    // TRADUCAO DE FALHAS (mesmo padrao do EmailService)
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

        return new FalhaGeocodificacaoException(
                "Não foi possível conectar ao serviço de geocodificação.", erro);
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

        // Provedores alternativos podem omitir o campo: nesse caso vale o conteudo.
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
                primeiro.geometry().location().lat(),
                primeiro.geometry().location().lng(),
                primeiro.formattedAddress());
    }
}