package carshub.com.br.backend.services.servicos;

import carshub.com.br.backend.exceptions.PerfilIncompativelException;
import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.exceptions.RegraDeNegocioException;
import carshub.com.br.backend.models.dtos.geocodificacao.LocalizacaoGeocodificadaDTO;
import carshub.com.br.backend.models.dtos.servicos.PropostaServicoDTO;
import carshub.com.br.backend.models.dtos.servicos.ServicoDisponivelDTO;
import carshub.com.br.backend.models.dtos.servicos.ServicoGuinchoRegistroDTO;
import carshub.com.br.backend.models.dtos.servicos.ServicoRespostaDTO;
import carshub.com.br.backend.models.entities.Carro;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.Guincho;
import carshub.com.br.backend.models.entities.servicos.ServicoGuincho;
import carshub.com.br.backend.models.enums.servicos.StatusServico;
import carshub.com.br.backend.models.enums.servicos.TipoServico;
import carshub.com.br.backend.repositories.CarroRepository;
import carshub.com.br.backend.repositories.EntusiastaRepository;
import carshub.com.br.backend.repositories.GuinchoRepository;
import carshub.com.br.backend.repositories.servicos.ServicoGuinchoRepository;
import carshub.com.br.backend.services.geocodificacao.GeocodificacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Maquina de estados do servico de guincho. Mesmas regras do servico mecanico:
 * o estado esperado vai no WHERE do UPDATE e a rejeicao/expiracao devolve o
 * chamado para a rede com guincho e valor nulos.
 *
 * O guincho e ordenado ESTRITAMENTE pela menor distancia (Haversine calculada no
 * banco), porque aqui nao existe compatibilidade de servicos como no mecanico.
 */
@Service
@RequiredArgsConstructor
public class ServicoGuinchoService {

    /** Raio padrao (km) do painel quando o app nao informa outro. */
    private static final double raioPadraoKm = 50.0;

    /** 1 grau de latitude ~= 111,32 km: base do pre-filtro (bounding box). */
    private static final double quilometrosPorGrau = 111.32;

    private final ServicoGuinchoRepository repositorioServicoGuincho;
    private final EntusiastaRepository repositorioEntusiasta;
    private final GuinchoRepository repositorioGuincho;
    private final CarroRepository repositorioCarro;
    private final GeocodificacaoService servicoGeocodificacao;

    // ==================================================================
    // LADO DO COLECIONADOR (solicitante)
    // ==================================================================

    @Transactional
    public ServicoRespostaDTO solicitarServico(UUID idEntusiasta, ServicoGuinchoRegistroDTO dados) {
        Entusiasta colecionador = buscarColecionadorOuLancarExcecao(idEntusiasta);
        Carro carro = buscarCarroDoColecionador(dados.idCarro(), idEntusiasta);

        LocalizacaoGeocodificadaDTO localizacao = resolverLocalizacao(
                dados.latitude(), dados.longitude(), dados.endereco(),
                colecionador.getLatitude(), colecionador.getLongitude(),
                "Informe a localização da ocorrência (latitude e longitude do GPS ou o endereço) — ou atualize a localização do seu perfil.");

        ServicoGuincho servico = new ServicoGuincho();
        servico.setEntusiasta(colecionador);
        servico.setCarro(carro);
        servico.setLatitudeAtualEntusiasta(localizacao.latitude());
        servico.setLongitudeAtualEntusiasta(localizacao.longitude());
        servico.setEnderecoAtualEntusiasta(descobrirEnderecoDaOcorrencia(localizacao));
        servico.setStatus(StatusServico.SOLICITADO);

        return mapearParaResposta(repositorioServicoGuincho.save(servico));
    }

    @Transactional(readOnly = true)
    public List<ServicoRespostaDTO> listarMeusServicos(UUID idEntusiasta, boolean somenteAbertos) {
        buscarEntusiastaOuLancarExcecao(idEntusiasta);

        List<ServicoGuincho> servicos = somenteAbertos
                ? repositorioServicoGuincho.findAllByEntusiasta_IdAndStatusNotOrderByDataHoraSolicitacaoDesc(idEntusiasta, StatusServico.FINALIZADO)
                : repositorioServicoGuincho.findAllByEntusiasta_IdOrderByDataHoraSolicitacaoDesc(idEntusiasta);

        return servicos.stream().map(this::mapearParaResposta).toList();
    }

    @Transactional(readOnly = true)
    public ServicoRespostaDTO buscarServico(UUID idServico, UUID idUsuario) {
        return mapearParaResposta(buscarServicoVisivelPara(idServico, idUsuario));
    }

    @Transactional
    public ServicoRespostaDTO aceitarProposta(UUID idServico, UUID idEntusiasta) {
        ServicoGuincho servico = buscarServicoDoEntusiasta(idServico, idEntusiasta);

        if (servico.getStatus() != StatusServico.PROPOSTA) {
            throw new RegraDeNegocioException("Não há proposta aguardando decisão nesta solicitação.");
        }

        if (repositorioServicoGuincho.alterarStatus(idServico, StatusServico.PENDENTE, StatusServico.PROPOSTA) == 0) {
            throw new RegraDeNegocioException("A solicitação mudou de estado enquanto você decidia. Atualize a tela e tente novamente.");
        }

        return mapearParaResposta(buscarServicoPorId(idServico));
    }

    @Transactional
    public ServicoRespostaDTO rejeitarProposta(UUID idServico, UUID idEntusiasta) {
        ServicoGuincho servico = buscarServicoDoEntusiasta(idServico, idEntusiasta);

        if (servico.getStatus() != StatusServico.PROPOSTA) {
            throw new RegraDeNegocioException("Não há proposta para recusar nesta solicitação.");
        }

        if (repositorioServicoGuincho.devolverParaRede(idServico, StatusServico.SOLICITADO, StatusServico.PROPOSTA) == 0) {
            throw new RegraDeNegocioException("A solicitação mudou de estado enquanto você decidia. Atualize a tela e tente novamente.");
        }

        return mapearParaResposta(buscarServicoPorId(idServico));
    }

    @Transactional
    public ServicoRespostaDTO confirmarConclusaoPeloEntusiasta(UUID idServico, UUID idEntusiasta) {
        ServicoGuincho servico = buscarServicoDoEntusiasta(idServico, idEntusiasta);

        validarServicoEmExecucao(servico);

        if (Boolean.TRUE.equals(servico.getConfirmacaoConclusaoEntusiasta())) {
            throw new RegraDeNegocioException("Você já confirmou a conclusão deste serviço. Aguardando a confirmação do guincho.");
        }

        if (repositorioServicoGuincho.confirmarConclusaoEntusiasta(idServico, StatusServico.PENDENTE) == 0) {
            throw new RegraDeNegocioException("Não foi possível registrar a confirmação: o serviço mudou de estado. Atualize a tela.");
        }

        repositorioServicoGuincho.finalizarSeAmbosConfirmaram(idServico, LocalDateTime.now(), StatusServico.FINALIZADO, StatusServico.PENDENTE);

        return mapearParaResposta(buscarServicoPorId(idServico));
    }

    @Transactional
    public void cancelarSolicitacao(UUID idServico, UUID idEntusiasta) {
        ServicoGuincho servico = buscarServicoDoEntusiasta(idServico, idEntusiasta);

        if (servico.getStatus() == StatusServico.PENDENTE || servico.getStatus() == StatusServico.FINALIZADO) {
            throw new RegraDeNegocioException("Um serviço em execução ou já finalizado não pode ser cancelado. Use a confirmação de conclusão.");
        }

        repositorioServicoGuincho.delete(servico);
    }

    // ==================================================================
    // LADO DO GUINCHO (prestador)
    // ==================================================================

    @Transactional(readOnly = true)
    public List<ServicoDisponivelDTO> listarServicosDisponiveis(UUID idGuincho, Double raioKm,
                                                                Double latitudeInformada, Double longitudeInformada,
                                                                String enderecoInformado) {
        Guincho guincho = buscarEValidarGuincho(idGuincho);
        double raio = resolverRaio(raioKm);

        LocalizacaoGeocodificadaDTO origem = resolverLocalizacao(
                latitudeInformada, longitudeInformada, enderecoInformado,
                guincho.getLatitude(), guincho.getLongitude(),
                "Cadastre a sua localização (PUT /api/carshub/guinchos/localizacao) ou informe latitude/longitude ou o endereço na requisição para ver os chamados da sua região.");

        double alcanceLatitude = raio / quilometrosPorGrau;
        double grausPorLongitude = quilometrosPorGrau * Math.cos(Math.toRadians(origem.latitude()));
        double alcanceLongitude = (grausPorLongitude <= 0.0) ? 180.0 : raio / grausPorLongitude;

        List<Object[]> linhas = repositorioServicoGuincho.buscarDisponiveisNaRegiao(
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
                .map(linha -> mapearParaDisponivel((ServicoGuincho) linha[0], (Double) linha[1]))
                .sorted(Comparator.comparing(ServicoDisponivelDTO::distanciaKm, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ServicoRespostaDTO> listarMinhaFilaDeTrabalho(UUID idGuincho) {
        buscarEValidarGuincho(idGuincho);

        return repositorioServicoGuincho.findAllByGuincho_IdOrderByDataHoraSolicitacaoDesc(idGuincho)
                .stream()
                .map(this::mapearParaResposta)
                .toList();
    }

    @Transactional
    public ServicoRespostaDTO enviarProposta(UUID idServico, UUID idGuincho, PropostaServicoDTO dados) {
        Guincho guincho = buscarEValidarGuincho(idGuincho);
        ServicoGuincho servico = buscarServicoPorId(idServico);

        if (servico.getStatus() != StatusServico.SOLICITADO) {
            throw new RegraDeNegocioException("Este chamado já está em negociação com outro guincho ou já foi atendido.");
        }

        LocalizacaoGeocodificadaDTO localizacao = resolverLocalizacao(
                dados.latitude(), dados.longitude(), null,
                guincho.getLatitude(), guincho.getLongitude(),
                "Atualize a sua localização (ou informe latitude e longitude na requisição) para enviar propostas.");

        int linhasAfetadas = repositorioServicoGuincho.registrarProposta(
                idServico,
                guincho,
                dados.valorProposto(),
                localizacao.latitude(),
                localizacao.longitude(),
                LocalDateTime.now(),
                StatusServico.PROPOSTA,
                StatusServico.SOLICITADO);

        if (linhasAfetadas == 0) {
            throw new RegraDeNegocioException("Outro guincho enviou uma proposta primeiro. Atualize a lista de chamados.");
        }

        return mapearParaResposta(buscarServicoPorId(idServico));
    }

    @Transactional
    public ServicoRespostaDTO confirmarConclusaoPeloGuincho(UUID idServico, UUID idGuincho) {
        Guincho guincho = buscarEValidarGuincho(idGuincho);
        ServicoGuincho servico = buscarServicoPorId(idServico);

        exigirGuinchoResponsavel(servico, guincho.getId());
        validarServicoEmExecucao(servico);

        if (Boolean.TRUE.equals(servico.getConfirmacaoConclusaoGuincho())) {
            throw new RegraDeNegocioException("Você já confirmou a conclusão deste serviço. Aguardando a confirmação do colecionador.");
        }

        if (repositorioServicoGuincho.confirmarConclusaoGuincho(idServico, StatusServico.PENDENTE) == 0) {
            throw new RegraDeNegocioException("Não foi possível registrar a confirmação: o serviço mudou de estado. Atualize a tela.");
        }

        repositorioServicoGuincho.finalizarSeAmbosConfirmaram(idServico, LocalDateTime.now(), StatusServico.FINALIZADO, StatusServico.PENDENTE);

        return mapearParaResposta(buscarServicoPorId(idServico));
    }

    // ==================================================================
    // USADO PELA TASK DE EXPIRACAO
    // ==================================================================

    @Transactional
    public int liberarPropostasExpiradas(int minutosExpiracao) {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(minutosExpiracao);
        return repositorioServicoGuincho.liberarPropostasExpiradas(limite, StatusServico.SOLICITADO, StatusServico.PROPOSTA);
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

    private Guincho buscarEValidarGuincho(UUID id) {
        return repositorioGuincho.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão inválida: Guincho não encontrado."));
    }

    private ServicoGuincho buscarServicoPorId(UUID idServico) {
        return repositorioServicoGuincho.findById(idServico)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Solicitação de guincho não encontrada."));
    }

    private ServicoGuincho buscarServicoDoEntusiasta(UUID idServico, UUID idEntusiasta) {
        ServicoGuincho servico = buscarServicoPorId(idServico);

        if (!servico.getEntusiasta().getId().equals(idEntusiasta)) {
            throw new PerfilIncompativelException("Acesso Negado: Esta solicitação não pertence a você.");
        }

        return servico;
    }

    private ServicoGuincho buscarServicoVisivelPara(UUID idServico, UUID idUsuario) {
        ServicoGuincho servico = buscarServicoPorId(idServico);

        boolean ehSolicitante = servico.getEntusiasta().getId().equals(idUsuario);
        boolean ehGuinchoResponsavel = servico.getGuincho() != null && servico.getGuincho().getId().equals(idUsuario);

        if (!ehSolicitante && !ehGuinchoResponsavel) {
            throw new PerfilIncompativelException("Acesso Negado: Você não participa desta solicitação de serviço.");
        }

        return servico;
    }

    private void validarServicoEmExecucao(ServicoGuincho servico) {
        if (servico.getStatus() != StatusServico.PENDENTE) {
            throw new RegraDeNegocioException("Só é possível confirmar a conclusão de um serviço que está em execução (PENDENTE).");
        }
    }

    private void exigirGuinchoResponsavel(ServicoGuincho servico, UUID idGuincho) {
        if (servico.getGuincho() == null || !servico.getGuincho().getId().equals(idGuincho)) {
            throw new PerfilIncompativelException("Acesso Negado: Este serviço está vinculado a outro guincho.");
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

    /** Mesma prioridade do servico mecanico: GPS > endereco digitado > perfil. */
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

    /** Best-effort: sem endereco legivel o chamado continua valido. */
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

    private ServicoRespostaDTO mapearParaResposta(ServicoGuincho servico) {
        Guincho guincho = servico.getGuincho();
        boolean contatoLiberado = servico.getStatus() == StatusServico.PENDENTE || servico.getStatus() == StatusServico.FINALIZADO;

        return new ServicoRespostaDTO(
                servico.getId(),
                TipoServico.GUINCHO,
                servico.getStatus(),
                servico.getValorProposto(),
                servico.getEntusiasta().getId(),
                servico.getEntusiasta().getNome(),
                contatoLiberado ? servico.getEntusiasta().getTelefone() : null,
                guincho != null ? guincho.getId() : null,
                guincho != null ? guincho.getNome() : null,
                (contatoLiberado && guincho != null) ? guincho.getTelefone() : null,
                servico.getCarro().getId(),
                descreverCarro(servico.getCarro()),
                List.of(),
                servico.getLatitudeAtualEntusiasta(),
                servico.getLongitudeAtualEntusiasta(),
                servico.getEnderecoAtualEntusiasta(),
                servico.getLatitudeAtualGuincho(),
                servico.getLongitudeAtualGuincho(),
                servico.getConfirmacaoConclusaoEntusiasta(),
                servico.getConfirmacaoConclusaoGuincho(),
                servico.getDataHoraSolicitacao(),
                servico.getDataHoraProposta(),
                servico.getDataHoraFinalizacao());
    }

    private ServicoDisponivelDTO mapearParaDisponivel(ServicoGuincho servico, Double distanciaKm) {
        return new ServicoDisponivelDTO(
                servico.getId(),
                TipoServico.GUINCHO,
                servico.getEntusiasta().getNome(),
                descreverCarro(servico.getCarro()),
                List.of(),
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