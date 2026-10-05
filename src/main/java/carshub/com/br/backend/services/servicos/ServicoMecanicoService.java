package carshub.com.br.backend.services.servicos;

import carshub.com.br.backend.exceptions.PerfilIncompativelException;
import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.exceptions.RegraDeNegocioException;
import carshub.com.br.backend.models.dtos.geocodificacao.LocalizacaoGeocodificadaDTO;
import carshub.com.br.backend.models.dtos.servicos.PropostaServicoDTO;
import carshub.com.br.backend.models.dtos.servicos.ServicoDisponivelDTO;
import carshub.com.br.backend.models.dtos.servicos.ServicoMecanicoRegistroDTO;
import carshub.com.br.backend.models.dtos.servicos.ServicoRespostaDTO;
import carshub.com.br.backend.models.entities.Carro;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.Mecanico;
import carshub.com.br.backend.models.entities.PrestadorServico;
import carshub.com.br.backend.models.entities.servicos.ServicoMecanico;
import carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados;
import carshub.com.br.backend.models.enums.servicos.StatusServico;
import carshub.com.br.backend.models.enums.servicos.TipoServico;
import carshub.com.br.backend.repositories.CarroRepository;
import carshub.com.br.backend.repositories.EntusiastaRepository;
import carshub.com.br.backend.repositories.PrestadorServicoRepository;
import carshub.com.br.backend.repositories.servicos.ServicoMecanicoRepository;
import carshub.com.br.backend.services.geocodificacao.GeocodificacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Maquina de estados do servico mecanico.
 *
 * Nenhuma transicao e feita com "le, confere, salva": o estado esperado vai no
 * WHERE do UPDATE (repositorio) e, se nenhuma linha for afetada, o metodo lanca
 * RegraDeNegocioException (422). Assim dois mecanicos nao conseguem propor para o
 * mesmo chamado nem o chamado muda de estado pelas costas de ninguem.
 *
 * GEOLOCALIZACAO: coordenada do dispositivo (HTML5 Geolocation) tem prioridade e
 * NAO gasta Geocoding API; endereco digitado e traduzido pelo back (bloqueante);
 * o endereco legivel da ocorrencia e preenchido por geocodificacao reversa em
 * modo best-effort (nunca derruba a solicitacao).
 */
@Service
@RequiredArgsConstructor
public class ServicoMecanicoService {

    /** Raio padrao (km) do painel quando o app nao informa outro. */
    private static final double raioPadraoKm = 50.0;

    /** 1 grau de latitude ~= 111,32 km: base do pre-filtro (bounding box). */
    private static final double quilometrosPorGrau = 111.32;

    private final ServicoMecanicoRepository repositorioServicoMecanico;
    private final EntusiastaRepository repositorioEntusiasta;
    private final PrestadorServicoRepository repositorioPrestadorServico;
    private final CarroRepository repositorioCarro;
    private final GeocodificacaoService servicoGeocodificacao;

    // ==================================================================
    // LADO DO COLECIONADOR (solicitante)
    // ==================================================================

    @Transactional
    public ServicoRespostaDTO solicitarServico(UUID idEntusiasta, ServicoMecanicoRegistroDTO dados) {
        Entusiasta colecionador = buscarColecionadorOuLancarExcecao(idEntusiasta);
        Carro carro = buscarCarroDoColecionador(dados.idCarro(), idEntusiasta);
        validarServicosSolicitados(dados.servicosOfertados());

        LocalizacaoGeocodificadaDTO localizacao = resolverLocalizacao(
                dados.latitude(), dados.longitude(), dados.endereco(),
                colecionador.getLatitude(), colecionador.getLongitude(),
                "Informe a localização da ocorrência (latitude e longitude do GPS ou o endereço) — ou atualize a localização do seu perfil.");

        ServicoMecanico servico = new ServicoMecanico();
        servico.setEntusiasta(colecionador);
        servico.setCarro(carro);
        servico.setServicos(dados.servicosOfertados());
        servico.setLatitudeAtualEntusiasta(localizacao.latitude());
        servico.setLongitudeAtualEntusiasta(localizacao.longitude());
        servico.setEnderecoAtualEntusiasta(descobrirEnderecoDaOcorrencia(localizacao));
        servico.setStatus(StatusServico.SOLICITADO);

        return mapearParaResposta(repositorioServicoMecanico.save(servico));
    }

    @Transactional(readOnly = true)
    public List<ServicoRespostaDTO> listarMeusServicos(UUID idEntusiasta, boolean somenteAbertos) {
        buscarEntusiastaOuLancarExcecao(idEntusiasta);

        List<ServicoMecanico> servicos = somenteAbertos
                ? repositorioServicoMecanico.findAllByEntusiasta_IdAndStatusNotOrderByDataHoraSolicitacaoDesc(idEntusiasta, StatusServico.FINALIZADO)
                : repositorioServicoMecanico.findAllByEntusiasta_IdOrderByDataHoraSolicitacaoDesc(idEntusiasta);

        return servicos.stream().map(this::mapearParaResposta).toList();
    }

    @Transactional(readOnly = true)
    public ServicoRespostaDTO buscarServico(UUID idServico, UUID idUsuario) {
        return mapearParaResposta(buscarServicoVisivelPara(idServico, idUsuario));
    }

    @Transactional
    public ServicoRespostaDTO aceitarProposta(UUID idServico, UUID idEntusiasta) {
        ServicoMecanico servico = buscarServicoDoEntusiasta(idServico, idEntusiasta);

        if (servico.getStatus() != StatusServico.PROPOSTA) {
            throw new RegraDeNegocioException("Não há proposta aguardando decisão nesta solicitação.");
        }

        if (repositorioServicoMecanico.alterarStatus(idServico, StatusServico.PENDENTE, StatusServico.PROPOSTA) == 0) {
            throw new RegraDeNegocioException("A solicitação mudou de estado enquanto você decidia. Atualize a tela e tente novamente.");
        }

        return mapearParaResposta(buscarServicoPorId(idServico));
    }

    @Transactional
    public ServicoRespostaDTO rejeitarProposta(UUID idServico, UUID idEntusiasta) {
        ServicoMecanico servico = buscarServicoDoEntusiasta(idServico, idEntusiasta);

        if (servico.getStatus() != StatusServico.PROPOSTA) {
            throw new RegraDeNegocioException("Não há proposta para recusar nesta solicitação.");
        }

        if (repositorioServicoMecanico.devolverParaRede(idServico, StatusServico.SOLICITADO, StatusServico.PROPOSTA) == 0) {
            throw new RegraDeNegocioException("A solicitação mudou de estado enquanto você decidia. Atualize a tela e tente novamente.");
        }

        return mapearParaResposta(buscarServicoPorId(idServico));
    }

    @Transactional
    public ServicoRespostaDTO confirmarConclusaoPeloEntusiasta(UUID idServico, UUID idEntusiasta) {
        ServicoMecanico servico = buscarServicoDoEntusiasta(idServico, idEntusiasta);

        validarServicoEmExecucao(servico);

        if (Boolean.TRUE.equals(servico.getConfirmacaoConclusaoEntusiasta())) {
            throw new RegraDeNegocioException("Você já confirmou a conclusão deste serviço. Aguardando a confirmação do mecânico.");
        }

        if (repositorioServicoMecanico.confirmarConclusaoEntusiasta(idServico, StatusServico.PENDENTE) == 0) {
            throw new RegraDeNegocioException("Não foi possível registrar a confirmação: o serviço mudou de estado. Atualize a tela.");
        }

        repositorioServicoMecanico.finalizarSeAmbosConfirmaram(idServico, LocalDateTime.now(), StatusServico.FINALIZADO, StatusServico.PENDENTE);

        return mapearParaResposta(buscarServicoPorId(idServico));
    }

    /**
     * Cancelamento = remocao fisica. Um servico em execucao deixaria o mecanico
     * na rua sem contrapartida e um servico finalizado e o historico do veiculo,
     * entao os dois casos sao bloqueados.
     */
    @Transactional
    public void cancelarSolicitacao(UUID idServico, UUID idEntusiasta) {
        ServicoMecanico servico = buscarServicoDoEntusiasta(idServico, idEntusiasta);

        if (servico.getStatus() == StatusServico.PENDENTE || servico.getStatus() == StatusServico.FINALIZADO) {
            throw new RegraDeNegocioException("Um serviço em execução ou já finalizado não pode ser cancelado. Use a confirmação de conclusão.");
        }

        repositorioServicoMecanico.delete(servico);
    }

    // ==================================================================
    // LADO DO MECANICO (prestador)
    // ==================================================================

    @Transactional(readOnly = true)
    public List<ServicoDisponivelDTO> listarServicosDisponiveis(UUID idMecanico, Double raioKm,
                                                                Double latitudeInformada, Double longitudeInformada,
                                                                String enderecoInformado) {
        Mecanico mecanico = buscarEValidarMecanico(idMecanico);
        double raio = resolverRaio(raioKm);

        LocalizacaoGeocodificadaDTO origem = resolverLocalizacao(
                latitudeInformada, longitudeInformada, enderecoInformado,
                mecanico.getLatitude(), mecanico.getLongitude(),
                "Cadastre a sua localização (PUT /api/carshub/mecanicos/localizacao) ou informe latitude/longitude ou o endereço na requisição para ver os chamados da sua região.");

        double alcanceLatitude = raio / quilometrosPorGrau;
        double grausPorLongitude = quilometrosPorGrau * Math.cos(Math.toRadians(origem.latitude()));
        double alcanceLongitude = (grausPorLongitude <= 0.0) ? 180.0 : raio / grausPorLongitude;

        List<Object[]> linhas = repositorioServicoMecanico.buscarDisponiveisNaRegiao(
                origem.latitude(),
                origem.longitude(),
                Math.max(-90.0, origem.latitude() - alcanceLatitude),
                Math.min(90.0, origem.latitude() + alcanceLatitude),
                Math.max(-180.0, origem.longitude() - alcanceLongitude),
                Math.min(180.0, origem.longitude() + alcanceLongitude),
                StatusServico.SOLICITADO);

        return linhas.stream()
                .map(linha -> new Object[]{linha[0], extrairDistancia(linha[1])})
                .filter(linha -> distanciaDentroDoRaio((Double) linha[1], raio))
                .map(linha -> mapearParaDisponivel((ServicoMecanico) linha[0], (Double) linha[1]))
                .sorted(Comparator.comparing(ServicoDisponivelDTO::distanciaKm, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServicoRespostaDTO> listarMinhaFilaDeTrabalho(UUID idMecanico) {
        buscarEValidarMecanico(idMecanico);

        return repositorioServicoMecanico.findAllByMecanico_IdOrderByDataHoraSolicitacaoDesc(idMecanico)
                .stream()
                .map(this::mapearParaResposta)
                .toList();
    }

    @Transactional
    public ServicoRespostaDTO enviarProposta(UUID idServico, UUID idMecanico, PropostaServicoDTO dados) {
        Mecanico mecanico = buscarEValidarMecanico(idMecanico);
        ServicoMecanico servico = buscarServicoPorId(idServico);

        if (servico.getStatus() != StatusServico.SOLICITADO) {
            throw new RegraDeNegocioException("Este chamado já está em negociação com outro mecânico ou já foi atendido.");
        }

        LocalizacaoGeocodificadaDTO localizacao = resolverLocalizacao(
                dados.latitude(), dados.longitude(), null,
                mecanico.getLatitude(), mecanico.getLongitude(),
                "Atualize a sua localização (ou informe latitude e longitude na requisição) para enviar propostas.");

        int linhasAfetadas = repositorioServicoMecanico.registrarProposta(
                idServico,
                mecanico,
                dados.valorProposto(),
                localizacao.latitude(),
                localizacao.longitude(),
                LocalDateTime.now(),
                StatusServico.PROPOSTA,
                StatusServico.SOLICITADO);

        if (linhasAfetadas == 0) {
            throw new RegraDeNegocioException("Outro mecânico enviou uma proposta primeiro. Atualize a lista de chamados.");
        }

        return mapearParaResposta(buscarServicoPorId(idServico));
    }

    @Transactional
    public ServicoRespostaDTO confirmarConclusaoPeloMecanico(UUID idServico, UUID idMecanico) {
        Mecanico mecanico = buscarEValidarMecanico(idMecanico);
        ServicoMecanico servico = buscarServicoPorId(idServico);

        exigirMecanicoResponsavel(servico, mecanico.getId());
        validarServicoEmExecucao(servico);

        if (Boolean.TRUE.equals(servico.getConfirmacaoConclusaoMecanico())) {
            throw new RegraDeNegocioException("Você já confirmou a conclusão deste serviço. Aguardando a confirmação do colecionador.");
        }

        if (repositorioServicoMecanico.confirmarConclusaoMecanico(idServico, StatusServico.PENDENTE) == 0) {
            throw new RegraDeNegocioException("Não foi possível registrar a confirmação: o serviço mudou de estado. Atualize a tela.");
        }

        repositorioServicoMecanico.finalizarSeAmbosConfirmaram(idServico, LocalDateTime.now(), StatusServico.FINALIZADO, StatusServico.PENDENTE);

        return mapearParaResposta(buscarServicoPorId(idServico));
    }

    // ==================================================================
    // USADO PELA TASK DE EXPIRACAO
    // ==================================================================

    @Transactional
    public int liberarPropostasExpiradas(int minutosExpiracao) {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(minutosExpiracao);
        return repositorioServicoMecanico.liberarPropostasExpiradas(limite, StatusServico.SOLICITADO, StatusServico.PROPOSTA);
    }

    // ==================================================================
    // AUXILIARES DE PERMISSAO E ESTADO
    // ==================================================================

    private Entusiasta buscarColecionadorOuLancarExcecao(UUID idEntusiasta) {
        Entusiasta entusiasta = buscarEntusiastaOuLancarExcecao(idEntusiasta);

        if (entusiasta.getColecionador() == null) {
            throw new RegraDeNegocioException("Apenas colecionadores podem solicitar serviços. Cadastre um veículo na sua garagem para começar.");
        }

        return entusiasta;
    }

    private Entusiasta buscarEntusiastaOuLancarExcecao(UUID idEntusiasta) {
        return repositorioEntusiasta.findById(idEntusiasta)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão inválida: Entusiasta não encontrado."));
    }

    private Carro buscarCarroDoColecionador(UUID idCarro, UUID idEntusiasta) {
        if (idCarro == null) {
            throw new IllegalArgumentException("O ID do carro é obrigatório para solicitar o serviço.");
        }

        Carro carro = repositorioCarro.findById(idCarro)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carro não encontrado."));

        if (!carro.getColecionador().getId().equals(idEntusiasta)) {
            throw new PerfilIncompativelException("Acesso Negado: Este veículo não pertence à sua coleção.");
        }

        return carro;
    }

    private void validarServicosSolicitados(List<ServicosOfertados> servicos) {
        if (servicos == null || servicos.isEmpty()) {
            throw new IllegalArgumentException("Informe pelo menos um serviço a ser realizado no veículo.");
        }
    }

    private Mecanico buscarEValidarMecanico(UUID id) {
        PrestadorServico prestador = repositorioPrestadorServico.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão inválida: Prestador não encontrado."));

        if (!(prestador instanceof Mecanico mecanico)) {
            throw new PerfilIncompativelException("Acesso Negado: O perfil selecionado não é de um Mecânico.");
        }

        return mecanico;
    }

    private ServicoMecanico buscarServicoPorId(UUID idServico) {
        return repositorioServicoMecanico.findById(idServico)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação de serviço não encontrada."));
    }

    private ServicoMecanico buscarServicoDoEntusiasta(UUID idServico, UUID idEntusiasta) {
        ServicoMecanico servico = buscarServicoPorId(idServico);

        if (!servico.getEntusiasta().getId().equals(idEntusiasta)) {
            throw new PerfilIncompativelException("Acesso Negado: Esta solicitação não pertence a você.");
        }

        return servico;
    }

    private ServicoMecanico buscarServicoVisivelPara(UUID idServico, UUID idUsuario) {
        ServicoMecanico servico = buscarServicoPorId(idServico);

        boolean ehSolicitante = servico.getEntusiasta().getId().equals(idUsuario);
        boolean ehMecanicoResponsavel = servico.getMecanico() != null && servico.getMecanico().getId().equals(idUsuario);

        if (!ehSolicitante && !ehMecanicoResponsavel) {
            throw new PerfilIncompativelException("Acesso Negado: Você não participa desta solicitação de serviço.");
        }

        return servico;
    }

    private void validarServicoEmExecucao(ServicoMecanico servico) {
        if (servico.getStatus() != StatusServico.PENDENTE) {
            throw new RegraDeNegocioException("Só é possível confirmar a conclusão de um serviço que está em execução (PENDENTE).");
        }
    }

    private void exigirMecanicoResponsavel(ServicoMecanico servico, UUID idMecanico) {
        if (servico.getMecanico() == null || !servico.getMecanico().getId().equals(idMecanico)) {
            throw new PerfilIncompativelException("Acesso Negado: Este serviço está vinculado a outro mecânico.");
        }
    }

    // ==================================================================
    // AUXILIARES DE GEOLOCALIZACAO
    // ==================================================================

    private double resolverRaio(Double raioKm) {
        double raio = (raioKm == null) ? raioPadraoKm : raioKm;

        if (raio <= 0.0) {
            throw new IllegalArgumentException("O raio de busca deve ser maior que zero.");
        }

        return raio;
    }

    /**
     * Ordem de precedencia da localizacao:
     *   1. latitude/longitude informadas (HTML5 Geolocation) - nao chama a API;
     *   2. endereco digitado - forward geocoding, BLOQUEANTE (502/503/504/422);
     *   3. ultima localizacao do perfil;
     *   4. nada disso -> 422, porque sem coordenada nao existe proximidade.
     */
    private LocalizacaoGeocodificadaDTO resolverLocalizacao(Double latitudeInformada, Double longitudeInformada,
                                                            String enderecoInformado,
                                                            Double latitudePerfil, Double longitudePerfil,
                                                            String mensagemSemLocalizacao) {
        boolean informouLatitude = latitudeInformada != null;
        boolean informouLongitude = longitudeInformada != null;

        if (informouLatitude != informouLongitude) {
            throw new IllegalArgumentException("Informe latitude e longitude juntas: o par de coordenadas é obrigatório.");
        }

        if (informouLatitude) {
            validarCoordenadas(latitudeInformada, longitudeInformada);
            return new LocalizacaoGeocodificadaDTO(latitudeInformada, longitudeInformada, null);
        }

        if (enderecoInformado != null && !enderecoInformado.isBlank()) {
            return servicoGeocodificacao.geocodificarEndereco(enderecoInformado);
        }

        if (latitudePerfil != null && longitudePerfil != null) {
            validarCoordenadas(latitudePerfil, longitudePerfil);
            return new LocalizacaoGeocodificadaDTO(latitudePerfil, longitudePerfil, null);
        }

        throw new RegraDeNegocioException(mensagemSemLocalizacao);
    }

    /**
     * O endereco legivel so e obrigatorio para EXIBIR o chamado no painel do
     * prestador. Quando a localizacao veio do GPS ou do perfil, tentamos a
     * geocodificacao reversa; se o servico externo falhar, o chamado continua
     * valido com endereco nulo (best-effort).
     */
    private String descobrirEnderecoDaOcorrencia(LocalizacaoGeocodificadaDTO localizacao) {
        if (localizacao.enderecoFormatado() != null) {
            return localizacao.enderecoFormatado();
        }

        return servicoGeocodificacao.buscarEnderecoFormatado(localizacao.latitude(), localizacao.longitude());
    }

    private void validarCoordenadas(Double latitude, Double longitude) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("A latitude deve estar entre -90 e 90.");
        }

        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("A longitude deve estar entre -180 e 180.");
        }
    }

    private Double extrairDistancia(Object valor) {
        return (valor instanceof Number numero) ? numero.doubleValue() : null;
    }

    private boolean distanciaDentroDoRaio(Double distanciaKm, double raioKm) {
        return distanciaKm != null && distanciaKm <= raioKm;
    }

    // ==================================================================
    // MAPEAMENTO
    // ==================================================================

    private ServicoRespostaDTO mapearParaResposta(ServicoMecanico servico) {
        Mecanico mecanico = servico.getMecanico();
        boolean contatoLiberado = servico.getStatus() == StatusServico.PENDENTE || servico.getStatus() == StatusServico.FINALIZADO;

        return new ServicoRespostaDTO(
                servico.getId(),
                TipoServico.MECANICO,
                servico.getStatus(),
                servico.getValorProposto(),
                servico.getEntusiasta().getId(),
                servico.getEntusiasta().getNome(),
                contatoLiberado ? servico.getEntusiasta().getTelefone() : null,
                mecanico != null ? mecanico.getId() : null,
                mecanico != null ? mecanico.getNome() : null,
                (contatoLiberado && mecanico != null) ? mecanico.getTelefone() : null,
                servico.getCarro().getId(),
                descreverCarro(servico.getCarro()),
                new ArrayList<>(servico.getServicos()),
                servico.getLatitudeAtualEntusiasta(),
                servico.getLongitudeAtualEntusiasta(),
                servico.getEnderecoAtualEntusiasta(),
                servico.getLatitudeAtualMecanico(),
                servico.getLongitudeAtualMecanico(),
                servico.getConfirmacaoConclusaoEntusiasta(),
                servico.getConfirmacaoConclusaoMecanico(),
                servico.getDataHoraSolicitacao(),
                servico.getDataHoraProposta(),
                servico.getDataHoraFinalizacao());
    }

    private ServicoDisponivelDTO mapearParaDisponivel(ServicoMecanico servico, Double distanciaKm) {
        return new ServicoDisponivelDTO(
                servico.getId(),
                TipoServico.MECANICO,
                servico.getEntusiasta().getNome(),
                descreverCarro(servico.getCarro()),
                new ArrayList<>(servico.getServicos()),
                servico.getEnderecoAtualEntusiasta(),
                servico.getLatitudeAtualEntusiasta(),
                servico.getLongitudeAtualEntusiasta(),
                distanciaKm,
                servico.getDataHoraSolicitacao());
    }

    private String descreverCarro(Carro carro) {
        return carro.getMarca() + " " + carro.getModelo() + " (" + carro.getPlaca() + ")";
    }
}