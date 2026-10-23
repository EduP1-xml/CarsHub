package carshub.com.br.backend.services.servicos;

import carshub.com.br.backend.exceptions.PerfilIncompativelException;
import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.exceptions.RegraDeNegocioException;
import carshub.com.br.backend.exceptions.SolicitacaoDuplicadaException;
import carshub.com.br.backend.models.dtos.geocodificacao.LocalizacaoGeocodificadaDTO;
import carshub.com.br.backend.models.dtos.servicos.AvaliacaoServicoDTO;
import carshub.com.br.backend.models.dtos.servicos.EnderecoOcorrenciaDTO;
import carshub.com.br.backend.models.dtos.servicos.PropostaServicoDTO;
import carshub.com.br.backend.models.dtos.servicos.PrestadorCompativelDTO;
import carshub.com.br.backend.models.dtos.servicos.ServicoDisponivelDTO;
import carshub.com.br.backend.models.dtos.servicos.ServicoMecanicoRegistroDTO;
import carshub.com.br.backend.models.dtos.servicos.ServicoRespostaDTO;
import carshub.com.br.backend.models.entities.Carro;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.Mecanico;
import carshub.com.br.backend.models.entities.servicos.EnderecoOcorrencia;
import carshub.com.br.backend.models.entities.servicos.ServicoMecanico;
import carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados;
import carshub.com.br.backend.models.enums.servicos.StatusServico;
import carshub.com.br.backend.models.enums.tiposUsuarios.TipoPrestadorServico;
import carshub.com.br.backend.models.enums.servicos.TipoServico;
import carshub.com.br.backend.repositories.CarroRepository;
import carshub.com.br.backend.repositories.EntusiastaRepository;
import carshub.com.br.backend.repositories.MecanicoRepository;
import carshub.com.br.backend.repositories.PrestadorServicoRepository;
import carshub.com.br.backend.repositories.servicos.ServicoMecanicoRepository;
import carshub.com.br.backend.services.file.FileStorageService;
import carshub.com.br.backend.services.geocodificacao.GeocodificacaoService;
import carshub.com.br.backend.utils.DistanciaGeografica;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Maquina de estados do servico mecanico:
 * SOLICITADO -> PROPOSTA -> PENDENTE -> CHEGOU -> FINALIZADO.
 * REGRAS DESTA CLASSE:
 *  - transicoes criticas por UPDATE condicional (guarda de estado atomica);
 *  - ELEGIBILIDADE: na criacao o sistema resolve QUAIS mecanicos ofertam todos os
 *    servicos pedidos; o painel filtra por essa lista e a proposta apenas confere
 *    pertencimento (nao ha revalidacao de compatibilidade);
 *  - idempotencia: chave unica por colecionador+carro+servicos, liberada ao finalizar;
 *  - rate limiting: 5 min entre solicitacoes do colecionador, 1 min entre propostas;
 *  - cota: no maximo 2 servicos ATIVOS (PENDENTE ou CHEGOU); proposta nao ocupa
 *    cota, e ao fechar a cota as propostas abertas voltam para a rede;
 *  - evidencias: foto inicial, chegada confirmada pelos dois, foto final de cada
 *    lado, avaliacao mutua e conclusao confirmada pelos dois;
 *  - cancelamento: SOLICITADO/PROPOSTA na hora; PENDENTE somente apos 2 horas.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServicoMecanicoService {

    /** Raio padrao (km) do painel quando o app nao informa outro. */
    private static final double raioPadraoKm = 50.0;

    /** Horas em PENDENTE antes de o colecionador poder cancelar. */
    private static final long horasParaLiberarCancelamento = 2;

    /** Maximo de servicos que o mecanico pode manter em andamento ao mesmo tempo. */
    private static final long maximoServicosAtivos = 2;

    /** MATCH: teto de propostas por chamado ao longo da vida (regra N14). */
    private static final int limitePropostasPorServico = 10;

    /**
     * Status que OCUPAM a cota de servicos ativos: o colecionador ja escolheu o
     * mecanico, que esta a caminho (PENDENTE) ou no local (CHEGOU).
     * PROPOSTA nao ocupa cota — o mecanico pode ter varias aguardando resposta;
     * quando a cota fecha, as excedentes voltam para a rede.
     */

    private static final List<StatusServico> statusDeServicoAtivo =
            List.of(StatusServico.PENDENTE, StatusServico.CHEGOU);

    private final ServicoMecanicoRepository repositorioServicoMecanico;
    private final EntusiastaRepository repositorioEntusiasta;
    private final PrestadorServicoRepository repositorioPrestadorServico;
    private final MecanicoRepository repositorioMecanico;
    private final CarroRepository repositorioCarro;
    private final GeocodificacaoService servicoGeocodificacao;
    private final FileStorageService servicoArmazenamentoArquivo;
    private final ControleCooldown controleCooldown;

    // ==================================================================
    // LADO DO COLECIONADOR
    // ==================================================================

    @Transactional
    public ServicoRespostaDTO solicitarServico(UUID idEntusiasta, ServicoMecanicoRegistroDTO dados) {
        Entusiasta colecionador = buscarColecionadorOuLancarExcecao(idEntusiasta);
        Carro carro = buscarCarroDoColecionador(dados.idCarro(), idEntusiasta);
        validarServicosSolicitados(dados.servicosOfertados());

        String chaveIdempotencia = GeradorChaveIdempotencia.gerar(
                idEntusiasta, dados.idCarro(), TipoServico.MECANICO, dados.servicosOfertados());

        if (repositorioServicoMecanico.existsByChaveIdempotencia(chaveIdempotencia)) {
            throw new SolicitacaoDuplicadaException(
                    "Você já tem uma solicitação ativa idêntica (mesmo veículo e mesmos serviços). "
                            + "Finalize ou cancele a anterior para abrir outra.");
        }

        controleCooldown.validarSolicitacaoDoColecionador(colecionador);

        LocalizacaoGeocodificadaDTO localizacao = resolverLocalizacao(
                dados.latitude(), dados.longitude(), dados.endereco(),
                colecionador.getLatitude(), colecionador.getLongitude(),
                "Informe a localização da ocorrência (latitude e longitude do GPS ou o endereço) — ou atualize a localização do seu perfil.");

        ServicoMecanico servico = new ServicoMecanico();
        servico.setEntusiasta(colecionador);
        servico.setCarro(carro);
        servico.setServicos(dados.servicosOfertados());
        servico.setChaveIdempotencia(chaveIdempotencia);
        servico.setMecanicosElegiveis(resolverMecanicosElegiveis(dados.servicosOfertados()));
        aplicarLocalizacao(servico, localizacao);
        servico.setStatus(StatusServico.SOLICITADO);

        ServicoMecanico servicoSalvo = salvarComTratamentoDeDuplicidade(servico);
        controleCooldown.registrarSolicitacaoDoColecionador(colecionador);

        return mapearParaResposta(servicoSalvo);
    }

    @Transactional(readOnly = true)
    public List<ServicoRespostaDTO> listarMeusServicos(UUID idEntusiasta, boolean somenteAbertos) {
        buscarEntusiastaOuLancarExcecao(idEntusiasta);

        List<ServicoMecanico> servicos = somenteAbertos
                ? repositorioServicoMecanico.findAllByEntusiasta_IdAndStatusNotOrderByDataHoraSolicitacaoDesc(idEntusiasta, StatusServico.FINALIZADO)
                : repositorioServicoMecanico.findAllByEntusiasta_IdOrderByDataHoraSolicitacaoDesc(idEntusiasta);

        Map<UUID, Long> ativosPorMecanico = contarAtivosDosMecanicos(servicos);

        return servicos.stream()
                .map(servico -> mapearParaResposta(servico, ativosPorMecanico))
                .toList();
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

        if (repositorioServicoMecanico.aceitarProposta(idServico, LocalDateTime.now(),
                StatusServico.PENDENTE, StatusServico.PROPOSTA) == 0) {
            throw new RegraDeNegocioException("A solicitação mudou de estado enquanto você decidia. Atualize a tela e tente novamente.");
        }

        liberarPropostasExcedentes(servico.getMecanico());

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

    /**
     * Cancelamento = remocao fisica, so pelo colecionador:
     *   SOLICITADO / PROPOSTA -> liberado na hora;
     *   PENDENTE              -> so depois de 2 horas contadas de dataHoraPendente
     *                            (evita esperar para sempre um mecanico que nao vem);
     *   CHEGOU / FINALIZADO   -> bloqueado (use a confirmacao de conclusao).
     */
    @Transactional
    public void cancelarSolicitacao(UUID idServico, UUID idEntusiasta) {
        ServicoMecanico servico = buscarServicoDoEntusiasta(idServico, idEntusiasta);

        if (servico.getStatus() == StatusServico.CHEGOU || servico.getStatus() == StatusServico.FINALIZADO) {
            throw new RegraDeNegocioException(
                    "Um serviço que já chegou ou foi finalizado não pode ser cancelado. Use a confirmação de conclusão.");
        }

        if (servico.getStatus() == StatusServico.PENDENTE) {
            validarTempoMinimoParaCancelar(servico.getDataHoraPendente());
        }

        repositorioServicoMecanico.delete(servico);
    }

    // ==================================================================
    // EVIDENCIAS: FOTO INICIAL
    // ==================================================================

    /** Foto do veiculo antes do servico. Pode ser substituida enquanto PENDENTE. */
    @Transactional
    public ServicoRespostaDTO enviarFotoInicial(UUID idServico, UUID idEntusiasta, MultipartFile arquivo) {
        ServicoMecanico servico = buscarServicoDoEntusiasta(idServico, idEntusiasta);

        if (servico.getStatus() != StatusServico.PENDENTE) {
            throw new RegraDeNegocioException(
                    "A foto inicial é enviada depois do aceite da proposta (PENDENTE).");
        }

        servico.setUrlFotoInicial(salvarFoto(arquivo, "a foto inicial do veículo"));
        repositorioServicoMecanico.save(servico);

        return mapearParaResposta(buscarServicoPorId(idServico));
    }

    // ==================================================================
    // CHEGADA (confirmacao dupla -> CHEGOU)
    // ==================================================================

    @Transactional
    public ServicoRespostaDTO confirmarChegadaPeloEntusiasta(UUID idServico, UUID idEntusiasta) {
        ServicoMecanico servico = buscarServicoDoEntusiasta(idServico, idEntusiasta);

        validarPreRequisitosDeChegada(servico);

        if (Boolean.TRUE.equals(servico.getConfirmacaoChegadaEntusiasta())) {
            throw new RegraDeNegocioException(
                    "Você já confirmou a chegada. Aguardando a confirmação do mecânico.");
        }

        servico.setConfirmacaoChegadaEntusiasta(true);
        repositorioServicoMecanico.save(servico);

        return avancarParaChegouSePossivel(idServico);
    }

    @Transactional
    public ServicoRespostaDTO confirmarChegadaPeloMecanico(UUID idServico, UUID idMecanico) {
        Mecanico mecanico = buscarEValidarMecanico(idMecanico);
        ServicoMecanico servico = buscarServicoPorId(idServico);

        exigirMecanicoResponsavel(servico, mecanico.getId());
        validarPreRequisitosDeChegada(servico);

        if (Boolean.TRUE.equals(servico.getConfirmacaoChegadaMecanico())) {
            throw new RegraDeNegocioException(
                    "Você já confirmou a chegada. Aguardando a confirmação do colecionador.");
        }

        servico.setConfirmacaoChegadaMecanico(true);
        repositorioServicoMecanico.save(servico);

        return avancarParaChegouSePossivel(idServico);
    }

    // ==================================================================
    // EVIDENCIAS: FOTO FINAL E AVALIACAO (estado CHEGOU)
    // ==================================================================

    @Transactional
    public ServicoRespostaDTO enviarFotoFinalPeloEntusiasta(UUID idServico, UUID idEntusiasta, MultipartFile arquivo) {
        ServicoMecanico servico = buscarServicoDoEntusiasta(idServico, idEntusiasta);

        validarEstadoChegou(servico);

        if (servico.getUrlFotoFinalEntusiasta() != null) {
            throw new RegraDeNegocioException("Você já enviou a foto final deste serviço.");
        }

        servico.setUrlFotoFinalEntusiasta(salvarFoto(arquivo, "a foto final do serviço"));
        repositorioServicoMecanico.save(servico);

        return mapearParaResposta(buscarServicoPorId(idServico));
    }

    @Transactional
    public ServicoRespostaDTO enviarFotoFinalPeloMecanico(UUID idServico, UUID idMecanico, MultipartFile arquivo) {
        Mecanico mecanico = buscarEValidarMecanico(idMecanico);
        ServicoMecanico servico = buscarServicoPorId(idServico);

        exigirMecanicoResponsavel(servico, mecanico.getId());
        validarEstadoChegou(servico);

        if (servico.getUrlFotoFinalMecanico() != null) {
            throw new RegraDeNegocioException("Você já enviou a foto final deste serviço.");
        }

        servico.setUrlFotoFinalMecanico(salvarFoto(arquivo, "a foto final do serviço"));
        repositorioServicoMecanico.save(servico);

        return mapearParaResposta(buscarServicoPorId(idServico));
    }

    /** O colecionador avalia o mecanico. */
    @Transactional
    public ServicoRespostaDTO avaliarPrestador(UUID idServico, UUID idEntusiasta, AvaliacaoServicoDTO dados) {
        ServicoMecanico servico = buscarServicoDoEntusiasta(idServico, idEntusiasta);

        validarEstadoChegou(servico);

        if (servico.getUrlFotoFinalEntusiasta() == null) {
            throw new RegraDeNegocioException("Envie a sua foto final antes de avaliar o mecânico.");
        }

        if (servico.getNotaAvaliacaoEntusiasta() != null) {
            throw new RegraDeNegocioException("Você já avaliou este serviço.");
        }

        servico.setNotaAvaliacaoEntusiasta(dados.nota());
        servico.setComentarioAvaliacaoEntusiasta(dados.comentario());
        servico.setDataHoraAvaliacaoEntusiasta(LocalDateTime.now());
        repositorioServicoMecanico.save(servico);

        return mapearParaResposta(buscarServicoPorId(idServico));
    }

    /** O mecanico avalia o colecionador. */
    @Transactional
    public ServicoRespostaDTO avaliarColecionador(UUID idServico, UUID idMecanico, AvaliacaoServicoDTO dados) {
        Mecanico mecanico = buscarEValidarMecanico(idMecanico);
        ServicoMecanico servico = buscarServicoPorId(idServico);

        exigirMecanicoResponsavel(servico, mecanico.getId());
        validarEstadoChegou(servico);

        if (servico.getUrlFotoFinalMecanico() == null) {
            throw new RegraDeNegocioException("Envie a sua foto final antes de avaliar o colecionador.");
        }

        if (servico.getNotaAvaliacaoMecanico() != null) {
            throw new RegraDeNegocioException("Você já avaliou este serviço.");
        }

        servico.setNotaAvaliacaoMecanico(dados.nota());
        servico.setComentarioAvaliacaoMecanico(dados.comentario());
        servico.setDataHoraAvaliacaoMecanico(LocalDateTime.now());
        repositorioServicoMecanico.save(servico);

        return mapearParaResposta(buscarServicoPorId(idServico));
    }

    // ==================================================================
    // CONCLUSAO (handshake duplo -> FINALIZADO)
    // ==================================================================

    @Transactional
    public ServicoRespostaDTO confirmarConclusaoPeloEntusiasta(UUID idServico, UUID idEntusiasta) {
        ServicoMecanico servico = buscarServicoDoEntusiasta(idServico, idEntusiasta);

        validarPreRequisitosDeConclusao(servico,
                servico.getUrlFotoFinalEntusiasta() != null,
                servico.getNotaAvaliacaoEntusiasta() != null,
                servico.getConfirmacaoConclusaoEntusiasta(),
                "Você");

        servico.setConfirmacaoConclusaoEntusiasta(true);
        repositorioServicoMecanico.save(servico);

        return finalizarSePossivel(idServico);
    }

    @Transactional
    public ServicoRespostaDTO confirmarConclusaoPeloMecanico(UUID idServico, UUID idMecanico) {
        Mecanico mecanico = buscarEValidarMecanico(idMecanico);
        ServicoMecanico servico = buscarServicoPorId(idServico);

        exigirMecanicoResponsavel(servico, mecanico.getId());

        validarPreRequisitosDeConclusao(servico,
                servico.getUrlFotoFinalMecanico() != null,
                servico.getNotaAvaliacaoMecanico() != null,
                servico.getConfirmacaoConclusaoMecanico(),
                "Você");

        servico.setConfirmacaoConclusaoMecanico(true);
        repositorioServicoMecanico.save(servico);

        return finalizarSePossivel(idServico);
    }

    // LADO DO MECANICO

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

        // 1) O banco filtra status + retangulo pelo INDICE ESPACIAL e devolve os IDs.
        DistanciaGeografica.CaixaCoordenadas caixa =
                DistanciaGeografica.caixaDeBusca(origem.latitude(), origem.longitude(), raio);

        List<UUID> idsCandidatos =
                repositorioServicoMecanico.buscarIdsDisponiveisNaRegiao(StatusServico.SOLICITADO, caixa);

        if (idsCandidatos.isEmpty()) {
            return List.of();
        }

        // 2) Elegibilidade em JPQL (UUID tipado); Haversine, corte do raio e
        //    ordenacao por distancia em Java. Sem notaMatch aqui: o painel do
        //    prestador so mostra as informacoes do chamado (decisao de produto).
        return repositorioServicoMecanico.buscarPorIdsElegiveis(idsCandidatos, mecanico.getId())
                .stream()
                .map(servico -> {
                    Double distanciaKm = DistanciaGeografica.distanciaKm(
                            origem.latitude(),
                            origem.longitude(),
                            servico.getLatitudeAtualEntusiasta(),
                            servico.getLongitudeAtualEntusiasta());

                    return new ChamadoComDistancia(servico, distanciaKm);
                })
                .filter(chamado -> chamado.dentroDoRaio(raio))
                .sorted(ordenacaoDosChamados())
                .map(chamado -> mapearParaDisponivel(chamado.servico(), chamado.distanciaKm()))
                .toList();
    }
    // ==================================================================

    @Transactional(readOnly = true)
    public List<ServicoRespostaDTO> listarMinhaFilaDeTrabalho(UUID idMecanico) {
        buscarEValidarMecanico(idMecanico);

        Map<UUID, Long> ativosPorMecanico = Map.of(
                idMecanico, repositorioServicoMecanico.countByMecanico_IdAndStatusIn(idMecanico, statusDeServicoAtivo));

        return repositorioServicoMecanico.findAllByMecanico_IdOrderByDataHoraSolicitacaoDesc(idMecanico)
                .stream()
                .map(servico -> mapearParaResposta(servico, ativosPorMecanico))
                .toList();
    }

    @Transactional
    public ServicoRespostaDTO enviarProposta(UUID idServico, UUID idMecanico, PropostaServicoDTO dados) {
        Mecanico mecanico = buscarEValidarMecanico(idMecanico);
        ServicoMecanico servico = buscarServicoPorId(idServico);

        if (servico.getStatus() != StatusServico.SOLICITADO) {
            throw new RegraDeNegocioException("Este chamado já está em negociação com outro mecânico ou já foi atendido.");
        }

        exigirMecanicoElegivel(servico, mecanico);
        validarCotaDeServicosAtivos(mecanico);
        controleCooldown.validarPropostaDoMecanico(mecanico);

        LocalizacaoGeocodificadaDTO localizacao = resolverLocalizacao(
                dados.latitude(), dados.longitude(), null,
                mecanico.getLatitude(), mecanico.getLongitude(),
                "Atualize a sua localização (ou informe latitude e longitude na requisição) para enviar propostas.");

        int linhasAfetadas = repositorioServicoMecanico.registrarProposta(
                idServico, mecanico, dados.valorProposto(),
                localizacao.latitude(), localizacao.longitude(), LocalDateTime.now(),
                StatusServico.PROPOSTA, StatusServico.SOLICITADO, limitePropostasPorServico);

        if (linhasAfetadas == 0) {
            ServicoMecanico atual = buscarServicoPorId(idServico);

            if (atual.getStatus() == StatusServico.SOLICITADO) {
                throw new RegraDeNegocioException(
                        "Este chamado já recebeu o limite de " + limitePropostasPorServico
                                + " propostas. Aguarde o colecionador decidir.");
            }

            throw new RegraDeNegocioException("Outro mecânico enviou uma proposta primeiro. Atualize a lista de chamados.");
        }
        controleCooldown.registrarPropostaDoMecanico(mecanico);

        return mapearParaResposta(buscarServicoPorId(idServico));
    }

    // ==================================================================
    // USADO PELA TASK DE EXPIRACAO (30 minutos sem resposta)
    // ==================================================================

    @Transactional
    public int liberarPropostasExpiradas(int minutosExpiracao) {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(minutosExpiracao);
        return repositorioServicoMecanico.liberarPropostasExpiradas(
                limite, StatusServico.SOLICITADO, StatusServico.PROPOSTA);
    }

    // ==================================================================
    // VALIDACOES DE FLUXO
    // ==================================================================

    /**
     * Resolve a rede do chamado na CRIACAO: mecanicos ativos que ofertam todos os
     * servicos pedidos. E aqui que a compatibilidade e decidida — a proposta nao
     * revalida nada.
     */
    private List<Mecanico> resolverMecanicosElegiveis(List<ServicosOfertados> servicosSolicitados) {
        List<Mecanico> elegiveis = repositorioMecanico.buscarCompativeisComTodos(
                servicosSolicitados, servicosSolicitados.size());

        if (elegiveis.isEmpty()) {
            log.warn("Nenhum mecânico ativo oferta todos os serviços solicitados ({}). "
                    + "O chamado ficará na rede até a limpeza automática.", servicosSolicitados);
        }

        return elegiveis;
    }

    /** Pertencimento a lista gravada na criacao (nao e revalidacao de compatibilidade). */
    private void exigirMecanicoElegivel(ServicoMecanico servico, Mecanico mecanico) {
        boolean elegivel = servico.getMecanicosElegiveis().stream()
                .anyMatch(candidato -> candidato.getId().equals(mecanico.getId()));

        if (!elegivel) {
            throw new RegraDeNegocioException(
                    "Este chamado não está disponível para você: ele exige serviços que você não oferta por completo.");
        }
    }

    private void validarCotaDeServicosAtivos(Mecanico mecanico) {
        long ativos = repositorioServicoMecanico.countByMecanico_IdAndStatusIn(mecanico.getId(), statusDeServicoAtivo);

        if (ativos >= maximoServicosAtivos) {
            throw new RegraDeNegocioException(
                    "Você já está com " + maximoServicosAtivos + " serviços em andamento (PENDENTE ou CHEGOU). "
                            + "Finalize um deles para aceitar novos chamados.");
        }
    }

    /**
     * Cota atingida: o prestador nao pode mais pegar chamados, entao as propostas
     * que ainda aguardavam resposta voltam para a rede em vez de ficarem presas
     * (o chamado deixa de estar bloqueado para os concorrentes).
     */
    private void liberarPropostasExcedentes(Mecanico mecanico) {
        if (mecanico == null) {
            return;
        }

        long ativos = repositorioServicoMecanico.countByMecanico_IdAndStatusIn(mecanico.getId(), statusDeServicoAtivo);

        if (ativos < maximoServicosAtivos) {
            return;
        }

        int liberadas = repositorioServicoMecanico.liberarPropostasDoMecanico(
                mecanico, StatusServico.SOLICITADO, StatusServico.PROPOSTA);

        if (liberadas > 0) {
            log.info("Cota de {} serviços ativos atingida por {}: {} proposta(s) devolvida(s) para a rede.",
                    maximoServicosAtivos, mecanico.getNome(), liberadas);
        }
    }

    private void validarPreRequisitosDeChegada(ServicoMecanico servico) {
        if (servico.getStatus() != StatusServico.PENDENTE) {
            throw new RegraDeNegocioException(
                    "Só é possível confirmar a chegada de um serviço que está em execução (PENDENTE).");
        }

        if (servico.getUrlFotoInicial() == null) {
            throw new RegraDeNegocioException(
                    "O colecionador precisa enviar a foto inicial do veículo antes da confirmação de chegada.");
        }
    }

    private void validarEstadoChegou(ServicoMecanico servico) {
        if (servico.getStatus() != StatusServico.CHEGOU) {
            throw new RegraDeNegocioException(
                    "Esta etapa é liberada depois que as duas partes confirmarem a chegada (CHEGOU).");
        }
    }

    /**
     * O que trava a ida para FINALIZADO: o status tem de estar em CHEGOU e aquele
     * lado precisa ter enviado a foto final e registrado a avaliacao antes.
     */
    private void validarPreRequisitosDeConclusao(ServicoMecanico servico, boolean temFotoFinal,
                                                 boolean temAvaliacao, Boolean jaConfirmou, String quem) {
        if (servico.getStatus() != StatusServico.CHEGOU) {
            throw new RegraDeNegocioException(
                    "Só é possível confirmar a conclusão de um serviço que chegou ao local (CHEGOU).");
        }

        if (!temFotoFinal) {
            throw new RegraDeNegocioException(quem + " precisa enviar a foto final antes de confirmar a conclusão.");
        }

        if (!temAvaliacao) {
            throw new RegraDeNegocioException(quem + " precisa avaliar a outra parte antes de confirmar a conclusão.");
        }

        if (Boolean.TRUE.equals(jaConfirmou)) {
            throw new RegraDeNegocioException(quem + " já confirmou a conclusão deste serviço.");
        }
    }

    private void validarTempoMinimoParaCancelar(LocalDateTime dataHoraPendente) {
        if (dataHoraPendente == null) {
            throw new RegraDeNegocioException("Não foi possível identificar o início do serviço para liberar o cancelamento.");
        }

        LocalDateTime liberadoEm = dataHoraPendente.plusHours(horasParaLiberarCancelamento);

        if (LocalDateTime.now().isBefore(liberadoEm)) {
            long minutosRestantes = Duration.between(LocalDateTime.now(), liberadoEm).toMinutes() + 1;
            throw new RegraDeNegocioException(
                    "O mecânico ainda está a caminho. O cancelamento fica disponível em "
                            + minutosRestantes + " minuto(s).");
        }
    }

    private ServicoRespostaDTO avancarParaChegouSePossivel(UUID idServico) {
        repositorioServicoMecanico.marcarChegou(idServico, LocalDateTime.now(),
                StatusServico.CHEGOU, StatusServico.PENDENTE);

        return mapearParaResposta(buscarServicoPorId(idServico));
    }

    private ServicoRespostaDTO finalizarSePossivel(UUID idServico) {
        int linhasAfetadas = repositorioServicoMecanico.finalizarSeAmbosConfirmaram(
                idServico, LocalDateTime.now(), StatusServico.FINALIZADO, StatusServico.CHEGOU);

        if (linhasAfetadas == 1) {
            ServicoMecanico finalizado = buscarServicoPorId(idServico);
            atualizarReputacaoDoMecanico(finalizado.getMecanico(), finalizado.getNotaAvaliacaoEntusiasta());
        }

        return mapearParaResposta(buscarServicoPorId(idServico));
    }

    /**
     * O agregado de reputacao do mecanico (media + quantidade) e
     * atualizado na CONCLUSAO do servico, na mesma transacao do UPDATE de
     * finalizacao. A nota do colecionador existe desde o CHEGOU e a conclusao
     * exige que ela esteja preenchida.
     */
    private void atualizarReputacaoDoMecanico(Mecanico mecanico, Integer nota) {
        if (mecanico == null || nota == null) {
            return;
        }

        double mediaAtual = mecanico.getNotaMediaPrestador() == null ? 0.0 : mecanico.getNotaMediaPrestador();
        long quantidadeAtual = mecanico.getQuantidadeAvaliacoes() == null ? 0L : mecanico.getQuantidadeAvaliacoes();

        mecanico.setNotaMediaPrestador((mediaAtual * quantidadeAtual + nota) / (quantidadeAtual + 1.0));
        mecanico.setQuantidadeAvaliacoes(quantidadeAtual + 1);
        repositorioMecanico.save(mecanico);
    }

    // ==================================================================
    // AUXILIARES DE PERMISSAO
    // ==================================================================

    private Entusiasta buscarColecionadorOuLancarExcecao(UUID idEntusiasta) {
        Entusiasta entusiasta = buscarEntusiastaOuLancarExcecao(idEntusiasta);

        if (entusiasta.getColecionador() == null) {
            throw new RegraDeNegocioException(
                    "Apenas colecionadores podem solicitar serviços. Cadastre um veículo na sua garagem para começar.");
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
        return repositorioMecanico.findById(id)
                .orElseThrow(() -> erroDeMecanicoNaoEncontrado(id));
    }

    /**
     * 403 quando o id existe mas e de outro tipo de prestador (guincho);
     * 404 quando nao existe ninguem com esse id.
     */
    private RuntimeException erroDeMecanicoNaoEncontrado(UUID id) {
        if (repositorioPrestadorServico.existsById(id)) {
            return new PerfilIncompativelException("Acesso Negado: O perfil selecionado não é de um Mecânico.");
        }

        return new RecursoNaoEncontradoException("Sessão inválida: Mecânico não encontrado.");
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
     * Prioridade: GPS (nao gasta cota) > endereco digitado (geocodificacao direta,
     * bloqueante) > ultima localizacao do perfil > 422.
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
            return deCoordenadas(latitudeInformada, longitudeInformada);
        }

        if (enderecoInformado != null && !enderecoInformado.isBlank()) {
            return servicoGeocodificacao.geocodificarEndereco(enderecoInformado);
        }

        if (latitudePerfil != null && longitudePerfil != null) {
            validarCoordenadas(latitudePerfil, longitudePerfil);
            return deCoordenadas(latitudePerfil, longitudePerfil);
        }

        throw new RegraDeNegocioException(mensagemSemLocalizacao);
    }

    /** Coordenada sem endereco conhecido, ja normalizada para 7 casas (WGS84). */
    private LocalizacaoGeocodificadaDTO deCoordenadas(Double latitude, Double longitude) {
        return LocalizacaoGeocodificadaDTO.deCoordenadas(
                GeocodificacaoService.normalizarCoordenada(latitude),
                GeocodificacaoService.normalizarCoordenada(longitude));
    }

    /**
     * O endereco legivel e o estruturado sao preenchidos aqui. Quando a localizacao
     * veio do GPS, tentamos a geocodificacao reversa (best-effort): se falhar, o
     * chamado continua valido com EnderecoOcorrenciaDTO.vazio().
     */
    private void aplicarLocalizacao(ServicoMecanico servico, LocalizacaoGeocodificadaDTO localizacao) {
        servico.setLatitudeAtualEntusiasta(localizacao.latitude());
        servico.setLongitudeAtualEntusiasta(localizacao.longitude());

        LocalizacaoGeocodificadaDTO comEndereco = localizacao;

        if (localizacao.enderecoFormatado() == null) {
            LocalizacaoGeocodificadaDTO reversa = servicoGeocodificacao
                    .buscarEnderecoDasCoordenadas(localizacao.latitude(), localizacao.longitude());

            if (reversa != null) {
                comEndereco = reversa;
            }
        }

        servico.setEnderecoAtualEntusiasta(comEndereco.enderecoFormatado());
        servico.setEnderecoEstruturado(montarEmbeddable(comEndereco.enderecoEstruturado()));
    }

    private EnderecoOcorrencia montarEmbeddable(EnderecoOcorrenciaDTO endereco) {
        EnderecoOcorrencia embeddable = new EnderecoOcorrencia();

        if (endereco == null) {
            return embeddable;
        }

        embeddable.setRua(endereco.rua());
        embeddable.setNumero(endereco.numero());
        embeddable.setBairro(endereco.bairro());
        embeddable.setCidade(endereco.cidade());
        embeddable.setEstado(endereco.estado());
        embeddable.setCep(endereco.cep());
        embeddable.setPlaceId(endereco.placeId());

        return embeddable;
    }

    private void validarCoordenadas(Double latitude, Double longitude) {
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException("A latitude deve estar entre -90 e 90.");
        }

        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException("A longitude deve estar entre -180 e 180.");
        }
    }

    // ==================================================================
    // ARQUIVOS
    // ==================================================================

    private String salvarFoto(MultipartFile arquivo, String descricao) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException("Envie o arquivo da " + descricao + ".");
        }

        return servicoArmazenamentoArquivo.salvarArquivo(arquivo);
    }

    // ==================================================================
    // PERSISTENCIA
    // ==================================================================

    /**
     * A checagem amigavel de duplicidade acontece antes; o indice unico e a
     * garantia real. Se duas requisicoes identicas passarem juntas, a segunda
     * estoura a constraint e vira o mesmo 409.
     */
    private ServicoMecanico salvarComTratamentoDeDuplicidade(ServicoMecanico servico) {
        try {
            return repositorioServicoMecanico.saveAndFlush(servico);
        }
        catch (DataIntegrityViolationException erro) {
            throw new SolicitacaoDuplicadaException(
                    "Você já tem uma solicitação ativa idêntica (mesmo veículo e mesmos serviços). "
                            + "Finalize ou cancele a anterior para abrir outra.");
        }
    }

    // ==================================================================
    // MATCH: RANKING DE PRESTADORES PARA UM CHAMADO
    // ==================================================================

    /**
     * Prestadores compativeis de UM chamado, para o colecionador.
     * Quem esta com a cota cheia (2 ativos) nem aparece,
     * porque nao poderia receber a proposta (o corte acontece no SQL, em
     * buscarElegiveisComCotaDisponivel). Sem coordenadas no perfil = fora do
     * ranking (sem distancia nao ha score). Ordem: notaMatch desc, notaAjustada
     * desc, distancia asc e, empatando tudo, nome alfabetico.
     */
    @Transactional(readOnly = true)
    public List<PrestadorCompativelDTO> listarPrestadoresCompativeis(ServicoMecanico servico, UUID idEntusiasta) {
        if (!servico.getEntusiasta().getId().equals(idEntusiasta)) {
            throw new PerfilIncompativelException("Acesso Negado: Esta solicitação não pertence a você.");
        }

        List<Mecanico> elegiveis = repositorioServicoMecanico.buscarElegiveisComCotaDisponivel(
                servico.getId(), statusDeServicoAtivo, maximoServicosAtivos);

        long semCoordenadas = elegiveis.stream()
                .filter(m -> m.getLatitude() == null || m.getLongitude() == null)
                .count();

        if (semCoordenadas > 0) {
            log.warn("{} mecânico(s) compatível(is) sem coordenadas ficaram de fora do ranking do chamado {}.",
                    semCoordenadas, servico.getId());
        }

        return elegiveis.stream()
                .filter(m -> m.getLatitude() != null && m.getLongitude() != null)
                .map(m -> mapearPrestadorCompativel(m, servico))
                .sorted(ordenacaoDosPrestadores())
                .toList();
    }

    private PrestadorCompativelDTO mapearPrestadorCompativel(Mecanico mecanico, ServicoMecanico servico) {
        Double distanciaKm = DistanciaGeografica.distanciaKm(
                mecanico.getLatitude(), mecanico.getLongitude(),
                servico.getLatitudeAtualEntusiasta(), servico.getLongitudeAtualEntusiasta());

        return new PrestadorCompativelDTO(
                mecanico.getId(),
                TipoPrestadorServico.MECANICO,
                mecanico.getNome(),
                CalculadoraMatch.notaAjustadaArredondada(
                        mecanico.getNotaMediaPrestador(), mecanico.getQuantidadeAvaliacoes()),
                mecanico.getQuantidadeAvaliacoes() == null ? 0L : mecanico.getQuantidadeAvaliacoes(),
                distanciaKm,
                CalculadoraMatch.notaMatch(
                        mecanico.getNotaMediaPrestador(), mecanico.getQuantidadeAvaliacoes(), distanciaKm));
    }

    private Comparator<PrestadorCompativelDTO> ordenacaoDosPrestadores() {
        return Comparator.comparing(PrestadorCompativelDTO::notaMatch).reversed()
                .thenComparing(Comparator.comparingDouble(PrestadorCompativelDTO::notaAjustada).reversed())
                .thenComparing(PrestadorCompativelDTO::distanciaKm)
                .thenComparing(PrestadorCompativelDTO::nomePrestador);
    }

    /** UMA consulta para a carga ativa de todos os mecanicos da lista (evita N+1). */
    private Map<UUID, Long> contarAtivosDosMecanicos(List<ServicoMecanico> servicos) {
        List<UUID> ids = servicos.stream()
                .map(ServicoMecanico::getMecanico)
                .filter(Objects::nonNull)
                .map(Mecanico::getId)
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Long> ativos = new HashMap<>();
        for (Object[] linha : repositorioServicoMecanico.contarAtivosPorMecanico(ids, statusDeServicoAtivo)) {
            ativos.put((UUID) linha[0], (Long) linha[1]);
        }

        return ativos;
    }
    // ==================================================================
    // MAPEAMENTO
    // ==================================================================

    private ServicoRespostaDTO mapearParaResposta(ServicoMecanico servico) {
        return mapearParaResposta(servico, null);
    }

    /**
     * ativosPorMecanico: carga ativa levantada em UMA consulta para as LISTAGENS
     * (evita N+1). Quando null (chamado unico), a carga e consultada pontualmente.
     */
    private ServicoRespostaDTO mapearParaResposta(ServicoMecanico servico, Map<UUID, Long> ativosPorMecanico) {
        Mecanico mecanico = servico.getMecanico();
        boolean contatoLiberado = servico.getStatus() != StatusServico.SOLICITADO
                && servico.getStatus() != StatusServico.PROPOSTA;

        Integer notaMatch = null;
        Double notaMediaPrestador = null;
        Long quantidadeAvaliacoes = null;
        Double distanciaAteOcorrencia = null;
        Long servicosAtivosDoPrestador = null;

        if (mecanico != null) {
            notaMediaPrestador = CalculadoraMatch.notaAjustadaArredondada(
                    mecanico.getNotaMediaPrestador(), mecanico.getQuantidadeAvaliacoes());
            quantidadeAvaliacoes = mecanico.getQuantidadeAvaliacoes() == null ? 0L : mecanico.getQuantidadeAvaliacoes();

            Double latitudePrestador = servico.getLatitudeAtualMecanico() != null
                    ? servico.getLatitudeAtualMecanico() : mecanico.getLatitude();
            Double longitudePrestador = servico.getLongitudeAtualMecanico() != null
                    ? servico.getLongitudeAtualMecanico() : mecanico.getLongitude();

            if (latitudePrestador != null && longitudePrestador != null) {
                distanciaAteOcorrencia = DistanciaGeografica.distanciaKm(
                        latitudePrestador, longitudePrestador,
                        servico.getLatitudeAtualEntusiasta(), servico.getLongitudeAtualEntusiasta());

                notaMatch = CalculadoraMatch.notaMatch(
                        mecanico.getNotaMediaPrestador(), mecanico.getQuantidadeAvaliacoes(), distanciaAteOcorrencia);
            }

            long ativos = (ativosPorMecanico != null)
                    ? ativosPorMecanico.getOrDefault(mecanico.getId(), 0L)
                    : repositorioServicoMecanico.countByMecanico_IdAndStatusIn(mecanico.getId(), statusDeServicoAtivo);

            boolean chamadoOcupaCota = statusDeServicoAtivo.contains(servico.getStatus());
            servicosAtivosDoPrestador = chamadoOcupaCota ? Math.max(0, ativos - 1) : ativos;
        }

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
                servico.getMecanicosElegiveis().size(),
                servico.getLatitudeAtualEntusiasta(),
                servico.getLongitudeAtualEntusiasta(),
                servico.getEnderecoAtualEntusiasta(),
                mapearEnderecoEstruturado(servico.getEnderecoEstruturado()),
                servico.getLatitudeAtualMecanico(),
                servico.getLongitudeAtualMecanico(),
                servico.getUrlFotoInicial(),
                servico.getConfirmacaoChegadaEntusiasta(),
                servico.getConfirmacaoChegadaMecanico(),
                servico.getDataHoraChegou(),
                servico.getUrlFotoFinalEntusiasta(),
                servico.getUrlFotoFinalMecanico(),
                servico.getNotaAvaliacaoEntusiasta(),
                servico.getComentarioAvaliacaoEntusiasta(),
                servico.getDataHoraAvaliacaoEntusiasta(),
                servico.getNotaAvaliacaoMecanico(),
                servico.getComentarioAvaliacaoMecanico(),
                servico.getDataHoraAvaliacaoMecanico(),
                servico.getConfirmacaoConclusaoEntusiasta(),
                servico.getConfirmacaoConclusaoMecanico(),
                servico.getDataHoraSolicitacao(),
                servico.getDataHoraProposta(),
                servico.getDataHoraPendente(),
                servico.getDataHoraFinalizacao(),
                notaMatch,
                notaMediaPrestador,
                quantidadeAvaliacoes,
                distanciaAteOcorrencia,
                servicosAtivosDoPrestador);
    }

    private ServicoDisponivelDTO mapearParaDisponivel(ServicoMecanico servico, Double distanciaKm) {
        return new ServicoDisponivelDTO(
                servico.getId(),
                servico.getEntusiasta().getNome(),
                distanciaKm,
                new ArrayList<>(servico.getServicos()),
                descreverCarro(servico.getCarro()));
    }

    private EnderecoOcorrenciaDTO mapearEnderecoEstruturado(EnderecoOcorrencia endereco) {
        if (endereco == null) {
            return EnderecoOcorrenciaDTO.vazio();
        }

        return new EnderecoOcorrenciaDTO(
                endereco.getRua(),
                endereco.getNumero(),
                endereco.getBairro(),
                endereco.getCidade(),
                endereco.getEstado(),
                endereco.getCep(),
                endereco.getPlaceId());
    }

    private String descreverCarro(Carro carro) {
        return carro.getMarca() + " " + carro.getModelo() + " (" + carro.getPlaca() + ")";
    }

    // ==================================================================
    // AUXILIAR DE LEITURA DO PAINEL
    // ==================================================================

    /**
     * Chamado com a distancia calculada UMA unica vez: filtro e ordenacao
     * reaproveitam o mesmo valor, sem recalcular trigonometria.
     * Ordem do painel (MATCH): distancia asc -> nome do entusiasta (alfabetico,
     * para o empate total ser estavel).
     */
    private record ChamadoComDistancia(ServicoMecanico servico, Double distanciaKm) {

        boolean dentroDoRaio(double raioKm) {
            return distanciaKm != null && distanciaKm <= raioKm;
        }
    }

    private Comparator<ChamadoComDistancia> ordenacaoDosChamados() {
        return Comparator.comparing(ChamadoComDistancia::distanciaKm)
                .thenComparing(chamado -> chamado.servico().getEntusiasta().getNome());
    }
}