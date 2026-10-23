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
import carshub.com.br.backend.models.dtos.servicos.ServicoGuinchoRegistroDTO;
import carshub.com.br.backend.models.dtos.servicos.ServicoRespostaDTO;
import carshub.com.br.backend.models.entities.Carro;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.Guincho;
import carshub.com.br.backend.models.entities.servicos.EnderecoOcorrencia;
import carshub.com.br.backend.models.entities.servicos.ServicoGuincho;
import carshub.com.br.backend.models.enums.servicos.StatusServico;
import carshub.com.br.backend.models.enums.tiposUsuarios.TipoPrestadorServico;
import carshub.com.br.backend.models.enums.servicos.TipoServico;
import carshub.com.br.backend.repositories.CarroRepository;
import carshub.com.br.backend.repositories.EntusiastaRepository;
import carshub.com.br.backend.repositories.GuinchoRepository;
import carshub.com.br.backend.repositories.servicos.ServicoGuinchoRepository;
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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Maquina de estados do servico de guincho:
 * SOLICITADO -> PROPOSTA -> PENDENTE -> CHEGOU -> FINALIZADO.
 *
 * REGRAS DESTA CLASSE:
 *  - transicoes criticas por UPDATE condicional (guarda de estado atomica);
 *  - idempotencia: chave unica por colecionador+carro, liberada ao finalizar;
 *  - rate limiting: 5 min entre solicitacoes do colecionador, 1 min entre propostas;
 *  - cota: no maximo 2 servicos ATIVOS (PENDENTE ou CHEGOU); proposta nao ocupa
 *    cota, e ao fechar a cota as propostas abertas voltam para a rede;
 *  - evidencias: foto inicial, chegada confirmada pelos dois, foto final de cada
 *    lado, avaliacao mutua e conclusao confirmada pelos dois;
 *  - cancelamento: SOLICITADO/PROPOSTA na hora; PENDENTE somente apos 2 horas.
 *
 * Nao existe compatibilidade de servicos no guincho: o painel lista todos os
 * chamados SOLICITADO dentro do raio (quem tem lista de elegiveis e o mecanico).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServicoGuinchoService {

    /** Raio padrao (km) do painel quando o app nao informa outro. */
    private static final double raioPadraoKm = 50.0;

    /** Horas em PENDENTE antes de o colecionador poder cancelar. */
    private static final long horasParaLiberarCancelamento = 2;

    /** Maximo de servicos que o guincho pode manter em andamento ao mesmo tempo. */
    private static final long maximoServicosAtivos = 2;

    /**
     * Status que OCUPAM a cota de servicos ativos: o colecionador ja escolheu o
     * guincho, que esta a caminho (PENDENTE) ou no local (CHEGOU).
     * PROPOSTA nao ocupa cota — o guincho pode ter varias aguardando resposta;
     * quando a cota fecha, as excedentes voltam para a rede.
     */
    private static final List<StatusServico> statusDeServicoAtivo =
            List.of(StatusServico.PENDENTE, StatusServico.CHEGOU);

    private final ServicoGuinchoRepository repositorioServicoGuincho;
    private final EntusiastaRepository repositorioEntusiasta;
    private final GuinchoRepository repositorioGuincho;
    private final CarroRepository repositorioCarro;
    private final GeocodificacaoService servicoGeocodificacao;
    private final FileStorageService servicoArmazenamentoArquivo;
    private final ControleCooldown controleCooldown;
    private static final int limitePropostasPorServico = 10;

    // ==================================================================
    // LADO DO COLECIONADOR
    // ==================================================================

    @Transactional
    public ServicoRespostaDTO solicitarServico(UUID idEntusiasta, ServicoGuinchoRegistroDTO dados) {
        Entusiasta colecionador = buscarColecionadorOuLancarExcecao(idEntusiasta);
        Carro carro = buscarCarroDoColecionador(dados.idCarro(), idEntusiasta);

        String chaveIdempotencia = GeradorChaveIdempotencia.gerar(
                idEntusiasta, dados.idCarro(), TipoServico.GUINCHO, null);

        if (repositorioServicoGuincho.existsByChaveIdempotencia(chaveIdempotencia)) {
            throw new SolicitacaoDuplicadaException(
                    "Você já tem uma solicitação de guincho ativa para este veículo. "
                            + "Finalize ou cancele a anterior para abrir outra.");
        }

        controleCooldown.validarSolicitacaoDoColecionador(colecionador);

        LocalizacaoGeocodificadaDTO localizacao = resolverLocalizacao(
                dados.latitude(), dados.longitude(), dados.endereco(),
                colecionador.getLatitude(), colecionador.getLongitude(),
                "Informe a localização da ocorrência (latitude e longitude do GPS ou o endereço) — ou atualize a localização do seu perfil.");

        ServicoGuincho servico = new ServicoGuincho();
        servico.setEntusiasta(colecionador);
        servico.setCarro(carro);
        servico.setChaveIdempotencia(chaveIdempotencia);
        aplicarLocalizacao(servico, localizacao);
        servico.setStatus(StatusServico.SOLICITADO);

        ServicoGuincho servicoSalvo = salvarComTratamentoDeDuplicidade(servico);
        controleCooldown.registrarSolicitacaoDoColecionador(colecionador);

        return mapearParaResposta(servicoSalvo);
    }

    @Transactional(readOnly = true)
    public List<ServicoRespostaDTO> listarMeusServicos(UUID idEntusiasta, boolean somenteAbertos) {
        buscarEntusiastaOuLancarExcecao(idEntusiasta);

        List<ServicoGuincho> servicos = somenteAbertos
                ? repositorioServicoGuincho.findAllByEntusiasta_IdAndStatusNotOrderByDataHoraSolicitacaoDesc(idEntusiasta, StatusServico.FINALIZADO)
                : repositorioServicoGuincho.findAllByEntusiasta_IdOrderByDataHoraSolicitacaoDesc(idEntusiasta);

        Map<UUID, Long> ativosPorGuincho = contarAtivosDosGuinchos(servicos);

        return servicos.stream()
                .map(servico -> mapearParaResposta(servico, ativosPorGuincho))
                .toList();
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

        if (repositorioServicoGuincho.aceitarProposta(idServico, LocalDateTime.now(),
                StatusServico.PENDENTE, StatusServico.PROPOSTA) == 0) {
            throw new RegraDeNegocioException("A solicitação mudou de estado enquanto você decidia. Atualize a tela e tente novamente.");
        }

        liberarPropostasExcedentes(servico.getGuincho());

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

    /**
     * Cancelamento = remocao fisica, so pelo colecionador:
     *   SOLICITADO / PROPOSTA -> liberado na hora;
     *   PENDENTE              -> so depois de 2 horas contadas de dataHoraPendente;
     *   CHEGOU / FINALIZADO   -> bloqueado (use a confirmacao de conclusao).
     */
    @Transactional
    public void cancelarSolicitacao(UUID idServico, UUID idEntusiasta) {
        ServicoGuincho servico = buscarServicoDoEntusiasta(idServico, idEntusiasta);

        if (servico.getStatus() == StatusServico.CHEGOU || servico.getStatus() == StatusServico.FINALIZADO) {
            throw new RegraDeNegocioException(
                    "Um serviço que já chegou ou foi finalizado não pode ser cancelado. Use a confirmação de conclusão.");
        }

        if (servico.getStatus() == StatusServico.PENDENTE) {
            validarTempoMinimoParaCancelar(servico.getDataHoraPendente());
        }

        repositorioServicoGuincho.delete(servico);
    }

    // ==================================================================
    // EVIDENCIAS: FOTO INICIAL
    // ==================================================================

    /** Foto do veiculo antes do servico. Pode ser substituida enquanto PENDENTE. */
    @Transactional
    public ServicoRespostaDTO enviarFotoInicial(UUID idServico, UUID idEntusiasta, MultipartFile arquivo) {
        ServicoGuincho servico = buscarServicoDoEntusiasta(idServico, idEntusiasta);

        if (servico.getStatus() != StatusServico.PENDENTE) {
            throw new RegraDeNegocioException(
                    "A foto inicial é enviada depois do aceite da proposta (PENDENTE).");
        }

        servico.setUrlFotoInicial(salvarFoto(arquivo, "a foto inicial do veículo"));
        repositorioServicoGuincho.save(servico);

        return mapearParaResposta(buscarServicoPorId(idServico));
    }

    // ==================================================================
    // CHEGADA (confirmacao dupla -> CHEGOU)
    // ==================================================================

    @Transactional
    public ServicoRespostaDTO confirmarChegadaPeloEntusiasta(UUID idServico, UUID idEntusiasta) {
        ServicoGuincho servico = buscarServicoDoEntusiasta(idServico, idEntusiasta);

        validarPreRequisitosDeChegada(servico);

        if (Boolean.TRUE.equals(servico.getConfirmacaoChegadaEntusiasta())) {
            throw new RegraDeNegocioException(
                    "Você já confirmou a chegada. Aguardando a confirmação do guincho.");
        }

        servico.setConfirmacaoChegadaEntusiasta(true);
        repositorioServicoGuincho.save(servico);

        return avancarParaChegouSePossivel(idServico);
    }

    @Transactional
    public ServicoRespostaDTO confirmarChegadaPeloGuincho(UUID idServico, UUID idGuincho) {
        Guincho guincho = buscarEValidarGuincho(idGuincho);
        ServicoGuincho servico = buscarServicoPorId(idServico);

        exigirGuinchoResponsavel(servico, guincho.getId());
        validarPreRequisitosDeChegada(servico);

        if (Boolean.TRUE.equals(servico.getConfirmacaoChegadaGuincho())) {
            throw new RegraDeNegocioException(
                    "Você já confirmou a chegada. Aguardando a confirmação do colecionador.");
        }

        servico.setConfirmacaoChegadaGuincho(true);
        repositorioServicoGuincho.save(servico);

        return avancarParaChegouSePossivel(idServico);
    }

    // ==================================================================
    // EVIDENCIAS: FOTO FINAL E AVALIACAO (estado CHEGOU)
    // ==================================================================

    @Transactional
    public ServicoRespostaDTO enviarFotoFinalPeloEntusiasta(UUID idServico, UUID idEntusiasta, MultipartFile arquivo) {
        ServicoGuincho servico = buscarServicoDoEntusiasta(idServico, idEntusiasta);

        validarEstadoChegou(servico);

        if (servico.getUrlFotoFinalEntusiasta() != null) {
            throw new RegraDeNegocioException("Você já enviou a foto final deste serviço.");
        }

        servico.setUrlFotoFinalEntusiasta(salvarFoto(arquivo, "a foto final do serviço"));
        repositorioServicoGuincho.save(servico);

        return mapearParaResposta(buscarServicoPorId(idServico));
    }

    @Transactional
    public ServicoRespostaDTO enviarFotoFinalPeloGuincho(UUID idServico, UUID idGuincho, MultipartFile arquivo) {
        Guincho guincho = buscarEValidarGuincho(idGuincho);
        ServicoGuincho servico = buscarServicoPorId(idServico);

        exigirGuinchoResponsavel(servico, guincho.getId());
        validarEstadoChegou(servico);

        if (servico.getUrlFotoFinalGuincho() != null) {
            throw new RegraDeNegocioException("Você já enviou a foto final deste serviço.");
        }

        servico.setUrlFotoFinalGuincho(salvarFoto(arquivo, "a foto final do serviço"));
        repositorioServicoGuincho.save(servico);

        return mapearParaResposta(buscarServicoPorId(idServico));
    }

    /** O colecionador avalia o guincho. */
    @Transactional
    public ServicoRespostaDTO avaliarPrestador(UUID idServico, UUID idEntusiasta, AvaliacaoServicoDTO dados) {
        ServicoGuincho servico = buscarServicoDoEntusiasta(idServico, idEntusiasta);

        validarEstadoChegou(servico);

        if (servico.getUrlFotoFinalEntusiasta() == null) {
            throw new RegraDeNegocioException("Envie a sua foto final antes de avaliar o guincho.");
        }

        if (servico.getNotaAvaliacaoEntusiasta() != null) {
            throw new RegraDeNegocioException("Você já avaliou este serviço.");
        }

        servico.setNotaAvaliacaoEntusiasta(dados.nota());
        servico.setComentarioAvaliacaoEntusiasta(dados.comentario());
        servico.setDataHoraAvaliacaoEntusiasta(LocalDateTime.now());
        repositorioServicoGuincho.save(servico);

        return mapearParaResposta(buscarServicoPorId(idServico));
    }

    /** O guincho avalia o colecionador. */
    @Transactional
    public ServicoRespostaDTO avaliarColecionador(UUID idServico, UUID idGuincho, AvaliacaoServicoDTO dados) {
        Guincho guincho = buscarEValidarGuincho(idGuincho);
        ServicoGuincho servico = buscarServicoPorId(idServico);

        exigirGuinchoResponsavel(servico, guincho.getId());
        validarEstadoChegou(servico);

        if (servico.getUrlFotoFinalGuincho() == null) {
            throw new RegraDeNegocioException("Envie a sua foto final antes de avaliar o colecionador.");
        }

        if (servico.getNotaAvaliacaoGuincho() != null) {
            throw new RegraDeNegocioException("Você já avaliou este serviço.");
        }

        servico.setNotaAvaliacaoGuincho(dados.nota());
        servico.setComentarioAvaliacaoGuincho(dados.comentario());
        servico.setDataHoraAvaliacaoGuincho(LocalDateTime.now());
        repositorioServicoGuincho.save(servico);

        return mapearParaResposta(buscarServicoPorId(idServico));
    }

    // ==================================================================
    // CONCLUSAO (handshake duplo -> FINALIZADO)
    // ==================================================================

    @Transactional
    public ServicoRespostaDTO confirmarConclusaoPeloEntusiasta(UUID idServico, UUID idEntusiasta) {
        ServicoGuincho servico = buscarServicoDoEntusiasta(idServico, idEntusiasta);

        validarPreRequisitosDeConclusao(servico,
                servico.getUrlFotoFinalEntusiasta() != null,
                servico.getNotaAvaliacaoEntusiasta() != null,
                servico.getConfirmacaoConclusaoEntusiasta(),
                "Você");

        servico.setConfirmacaoConclusaoEntusiasta(true);
        repositorioServicoGuincho.save(servico);

        return finalizarSePossivel(idServico);
    }

    @Transactional
    public ServicoRespostaDTO confirmarConclusaoPeloGuincho(UUID idServico, UUID idGuincho) {
        Guincho guincho = buscarEValidarGuincho(idGuincho);
        ServicoGuincho servico = buscarServicoPorId(idServico);

        exigirGuinchoResponsavel(servico, guincho.getId());

        validarPreRequisitosDeConclusao(servico,
                servico.getUrlFotoFinalGuincho() != null,
                servico.getNotaAvaliacaoGuincho() != null,
                servico.getConfirmacaoConclusaoGuincho(),
                "Você");

        servico.setConfirmacaoConclusaoGuincho(true);
        repositorioServicoGuincho.save(servico);

        return finalizarSePossivel(idServico);
    }

    // ==================================================================
    // LADO DO GUINCHO
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

        DistanciaGeografica.CaixaCoordenadas caixa =
                DistanciaGeografica.caixaDeBusca(origem.latitude(), origem.longitude(), raio);

        List<UUID> idsCandidatos =
                repositorioServicoGuincho.buscarIdsDisponiveisNaRegiao(StatusServico.SOLICITADO, caixa);

        if (idsCandidatos.isEmpty()) {
            return List.of();
        }

        return repositorioServicoGuincho.buscarPorIds(idsCandidatos)
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

    @Transactional(readOnly = true)
    public List<ServicoRespostaDTO> listarMinhaFilaDeTrabalho(UUID idGuincho) {
        buscarEValidarGuincho(idGuincho);

        Map<UUID, Long> ativosPorGuincho = Map.of(
                idGuincho, repositorioServicoGuincho.countByGuincho_IdAndStatusIn(idGuincho, statusDeServicoAtivo));

        return repositorioServicoGuincho.findAllByGuincho_IdOrderByDataHoraSolicitacaoDesc(idGuincho)
                .stream()
                .map(servico -> mapearParaResposta(servico, ativosPorGuincho))
                .toList();
    }

    @Transactional
    public ServicoRespostaDTO enviarProposta(UUID idServico, UUID idGuincho, PropostaServicoDTO dados) {
        Guincho guincho = buscarEValidarGuincho(idGuincho);
        ServicoGuincho servico = buscarServicoPorId(idServico);

        if (servico.getStatus() != StatusServico.SOLICITADO) {
            throw new RegraDeNegocioException("Este chamado já está em negociação com outro guincho ou já foi atendido.");
        }

        validarCotaDeServicosAtivos(guincho);
        controleCooldown.validarPropostaDoGuincho(guincho);

        LocalizacaoGeocodificadaDTO localizacao = resolverLocalizacao(
                dados.latitude(), dados.longitude(), null,
                guincho.getLatitude(), guincho.getLongitude(),
                "Atualize a sua localização (ou informe latitude e longitude na requisição) para enviar propostas.");

        int linhasAfetadas = repositorioServicoGuincho.registrarProposta(
                idServico, guincho, dados.valorProposto(),
                localizacao.latitude(), localizacao.longitude(), LocalDateTime.now(),
                StatusServico.PROPOSTA, StatusServico.SOLICITADO, limitePropostasPorServico);

        if (linhasAfetadas == 0) {
            ServicoGuincho atual = buscarServicoPorId(idServico);

            if (atual.getStatus() == StatusServico.SOLICITADO) {
                throw new RegraDeNegocioException(
                        "Este chamado já recebeu o limite de " + limitePropostasPorServico
                                + " propostas. Aguarde o colecionador decidir.");
            }

            throw new RegraDeNegocioException("Outro guincho enviou uma proposta primeiro. Atualize a lista de chamados.");
        }
        controleCooldown.registrarPropostaDoGuincho(guincho);

        return mapearParaResposta(buscarServicoPorId(idServico));
    }

    // ==================================================================
    // USADO PELA TASK DE EXPIRACAO (30 minutos sem resposta)
    // ==================================================================

    @Transactional
    public int liberarPropostasExpiradas(int minutosExpiracao) {
        LocalDateTime limite = LocalDateTime.now().minusMinutes(minutosExpiracao);
        return repositorioServicoGuincho.liberarPropostasExpiradas(
                limite, StatusServico.SOLICITADO, StatusServico.PROPOSTA);
    }

    // ==================================================================
    // VALIDACOES DE FLUXO
    // ==================================================================

    private void validarCotaDeServicosAtivos(Guincho guincho) {
        long ativos = repositorioServicoGuincho.countByGuincho_IdAndStatusIn(guincho.getId(), statusDeServicoAtivo);

        if (ativos >= maximoServicosAtivos) {
            throw new RegraDeNegocioException(
                    "Você já está com " + maximoServicosAtivos + " serviços em andamento (PENDENTE ou CHEGOU). "
                            + "Finalize um deles para aceitar novos chamados.");
        }
    }

    /**
     * Cota atingida: as propostas que ainda aguardavam resposta voltam para a rede
     * em vez de ficarem presas num guincho que nao pode mais atender.
     */
    private void liberarPropostasExcedentes(Guincho guincho) {
        long ativos = repositorioServicoGuincho.countByGuincho_IdAndStatusIn(guincho.getId(), statusDeServicoAtivo);

        if (ativos < maximoServicosAtivos) {
            return;
        }

        int liberadas = repositorioServicoGuincho.liberarPropostasDoGuincho(
                guincho, StatusServico.SOLICITADO, StatusServico.PROPOSTA);

        if (liberadas > 0) {
            log.info("Cota de {} serviços ativos atingida por {}: {} proposta(s) devolvida(s) para a rede.",
                    maximoServicosAtivos, guincho.getNome(), liberadas);
        }
    }

    private void validarPreRequisitosDeChegada(ServicoGuincho servico) {
        if (servico.getStatus() != StatusServico.PENDENTE) {
            throw new RegraDeNegocioException(
                    "Só é possível confirmar a chegada de um serviço que está em execução (PENDENTE).");
        }

        if (servico.getUrlFotoInicial() == null) {
            throw new RegraDeNegocioException(
                    "O colecionador precisa enviar a foto inicial do veículo antes da confirmação de chegada.");
        }
    }

    private void validarEstadoChegou(ServicoGuincho servico) {
        if (servico.getStatus() != StatusServico.CHEGOU) {
            throw new RegraDeNegocioException(
                    "Esta etapa é liberada depois que as duas partes confirmarem a chegada (CHEGOU).");
        }
    }

    /**
     * O que trava a ida para FINALIZADO: o status tem de estar em CHEGOU e aquele
     * lado precisa ter enviado a foto final e registrado a avaliacao antes.
     */
    private void validarPreRequisitosDeConclusao(ServicoGuincho servico, boolean temFotoFinal,
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
                    "O guincho ainda está a caminho. O cancelamento fica disponível em "
                            + minutosRestantes + " minuto(s).");
        }
    }

    private ServicoRespostaDTO avancarParaChegouSePossivel(UUID idServico) {
        repositorioServicoGuincho.marcarChegou(idServico, LocalDateTime.now(),
                StatusServico.CHEGOU, StatusServico.PENDENTE);

        return mapearParaResposta(buscarServicoPorId(idServico));
    }

    private ServicoRespostaDTO finalizarSePossivel(UUID idServico) {
        int linhasAfetadas = repositorioServicoGuincho.finalizarSeAmbosConfirmaram(
                idServico, LocalDateTime.now(), StatusServico.FINALIZADO, StatusServico.CHEGOU);

        if (linhasAfetadas == 1) {
            ServicoGuincho finalizado = buscarServicoPorId(idServico);
            atualizarReputacaoDoGuincho(finalizado.getGuincho(), finalizado.getNotaAvaliacaoEntusiasta());
        }

        return mapearParaResposta(buscarServicoPorId(idServico));
    }

    /**
     * O agregado de reputacao do guincho (media + quantidade) e
     * atualizado na CONCLUSAO do servico, na mesma transacao do UPDATE de
     * finalizacao. A nota do colecionador existe desde o CHEGOU e a conclusao
     * exige que ela esteja preenchida.
     */
    private void atualizarReputacaoDoGuincho(Guincho guincho, Integer nota) {
        if (guincho == null || nota == null) {
            return;
        }

        double mediaAtual = guincho.getNotaMediaPrestador() == null ? 0.0 : guincho.getNotaMediaPrestador();
        long quantidadeAtual = guincho.getQuantidadeAvaliacoes() == null ? 0L : guincho.getQuantidadeAvaliacoes();

        guincho.setNotaMediaPrestador((mediaAtual * quantidadeAtual + nota) / (quantidadeAtual + 1.0));
        guincho.setQuantidadeAvaliacoes(quantidadeAtual + 1);
        repositorioGuincho.save(guincho);
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
    private void aplicarLocalizacao(ServicoGuincho servico, LocalizacaoGeocodificadaDTO localizacao) {
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
    private ServicoGuincho salvarComTratamentoDeDuplicidade(ServicoGuincho servico) {
        try {
            return repositorioServicoGuincho.saveAndFlush(servico);
        }
        catch (DataIntegrityViolationException erro) {
            throw new SolicitacaoDuplicadaException(
                    "Você já tem uma solicitação de guincho ativa para este veículo. "
                            + "Finalize ou cancele a anterior para abrir outra.");
        }
    }

    // MATCH: RANKING DE PRESTADORES PARA UM CHAMADO
    /**
     * Prestadores compativeis de UM chamado de guincho, para o colecionador.
     * Elegibilidade = ativo + cota disponivel (corte no SQL). Ordem: notaMatch desc,
     * notaAjustada desc, distancia asc e, empatando tudo, nome alfabetico.
     */
    @Transactional(readOnly = true)
    public List<PrestadorCompativelDTO> listarPrestadoresCompativeis(ServicoGuincho servico, UUID idEntusiasta) {
        if (!servico.getEntusiasta().getId().equals(idEntusiasta)) {
            throw new PerfilIncompativelException("Acesso Negado: Esta solicitação não pertence a você.");
        }

        List<Guincho> disponiveis = repositorioServicoGuincho.buscarDisponiveisComCota(statusDeServicoAtivo, maximoServicosAtivos);

        long semCoordenadas = disponiveis.stream()
                .filter(g -> g.getLatitude() == null || g.getLongitude() == null)
                .count();

        if (semCoordenadas > 0) {
            log.warn("{} guincho(s) sem coordenadas ficou(aram) de fora do ranking do chamado {}.",
                    semCoordenadas, servico.getId());
        }

        return disponiveis.stream()
                .filter(g -> g.getLatitude() != null && g.getLongitude() != null)
                .map(g -> mapearGuinchoCompativel(g, servico))
                .sorted(ordenacaoDosPrestadores())
                .toList();
    }

    private PrestadorCompativelDTO mapearGuinchoCompativel(Guincho guincho, ServicoGuincho servico) {
        Double distanciaKm = DistanciaGeografica.distanciaKm(
                guincho.getLatitude(), guincho.getLongitude(),
                servico.getLatitudeAtualEntusiasta(), servico.getLongitudeAtualEntusiasta());

        return new PrestadorCompativelDTO(
                guincho.getId(),
                TipoPrestadorServico.GUINCHO,
                guincho.getNome(),
                CalculadoraMatch.notaAjustadaArredondada(
                        guincho.getNotaMediaPrestador(), guincho.getQuantidadeAvaliacoes()),
                guincho.getQuantidadeAvaliacoes() == null ? 0L : guincho.getQuantidadeAvaliacoes(),
                distanciaKm,
                CalculadoraMatch.notaMatch(
                        guincho.getNotaMediaPrestador(), guincho.getQuantidadeAvaliacoes(), distanciaKm));
    }

    private Comparator<PrestadorCompativelDTO> ordenacaoDosPrestadores() {
        return Comparator.comparing(PrestadorCompativelDTO::notaMatch).reversed()
                .thenComparing(Comparator.comparingDouble(PrestadorCompativelDTO::notaAjustada).reversed())
                .thenComparing(PrestadorCompativelDTO::distanciaKm)
                .thenComparing(PrestadorCompativelDTO::nomePrestador);
    }

    /** UMA consulta para a carga ativa de todos os guinchos da lista (evita N+1). */
    private Map<UUID, Long> contarAtivosDosGuinchos(List<ServicoGuincho> servicos) {
        List<UUID> ids = servicos.stream()
                .map(ServicoGuincho::getGuincho)
                .filter(Objects::nonNull)
                .map(Guincho::getId)
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Long> ativos = new HashMap<>();
        for (Object[] linha : repositorioServicoGuincho.contarAtivosPorGuincho(ids, statusDeServicoAtivo)) {
            ativos.put((UUID) linha[0], (Long) linha[1]);
        }

        return ativos;
    }

    // ==================================================================
    // MAPEAMENTO
    // ==================================================================

    private ServicoRespostaDTO mapearParaResposta(ServicoGuincho servico) {
        return mapearParaResposta(servico, null);
    }

    private ServicoRespostaDTO mapearParaResposta(ServicoGuincho servico, Map<UUID, Long> ativosPorGuincho) {
        Guincho guincho = servico.getGuincho();
        boolean contatoLiberado = servico.getStatus() != StatusServico.SOLICITADO
                && servico.getStatus() != StatusServico.PROPOSTA;

        Integer notaMatch = null;
        Double notaMediaPrestador = null;
        Long quantidadeAvaliacoes = null;
        Double distanciaAteOcorrencia = null;
        Long servicosAtivosDoPrestador = null;

        if (guincho != null) {
            notaMediaPrestador = CalculadoraMatch.notaAjustadaArredondada(
                    guincho.getNotaMediaPrestador(), guincho.getQuantidadeAvaliacoes());
            quantidadeAvaliacoes = guincho.getQuantidadeAvaliacoes() == null ? 0L : guincho.getQuantidadeAvaliacoes();

            Double latitudePrestador = servico.getLatitudeAtualGuincho() != null
                    ? servico.getLatitudeAtualGuincho() : guincho.getLatitude();
            Double longitudePrestador = servico.getLongitudeAtualGuincho() != null
                    ? servico.getLongitudeAtualGuincho() : guincho.getLongitude();

            if (latitudePrestador != null && longitudePrestador != null) {
                distanciaAteOcorrencia = DistanciaGeografica.distanciaKm(
                        latitudePrestador, longitudePrestador,
                        servico.getLatitudeAtualEntusiasta(), servico.getLongitudeAtualEntusiasta());

                notaMatch = CalculadoraMatch.notaMatch(
                        guincho.getNotaMediaPrestador(), guincho.getQuantidadeAvaliacoes(), distanciaAteOcorrencia);
            }

            long ativos = (ativosPorGuincho != null)
                    ? ativosPorGuincho.getOrDefault(guincho.getId(), 0L)
                    : repositorioServicoGuincho.countByGuincho_IdAndStatusIn(guincho.getId(), statusDeServicoAtivo);

            boolean chamadoOcupaCota = statusDeServicoAtivo.contains(servico.getStatus());
            servicosAtivosDoPrestador = chamadoOcupaCota ? Math.max(0, ativos - 1) : ativos;
        }

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
                null,
                servico.getLatitudeAtualEntusiasta(),
                servico.getLongitudeAtualEntusiasta(),
                servico.getEnderecoAtualEntusiasta(),
                mapearEnderecoEstruturado(servico.getEnderecoEstruturado()),
                servico.getLatitudeAtualGuincho(),
                servico.getLongitudeAtualGuincho(),
                servico.getUrlFotoInicial(),
                servico.getConfirmacaoChegadaEntusiasta(),
                servico.getConfirmacaoChegadaGuincho(),
                servico.getDataHoraChegou(),
                servico.getUrlFotoFinalEntusiasta(),
                servico.getUrlFotoFinalGuincho(),
                servico.getNotaAvaliacaoEntusiasta(),
                servico.getComentarioAvaliacaoEntusiasta(),
                servico.getDataHoraAvaliacaoEntusiasta(),
                servico.getNotaAvaliacaoGuincho(),
                servico.getComentarioAvaliacaoGuincho(),
                servico.getDataHoraAvaliacaoGuincho(),
                servico.getConfirmacaoConclusaoEntusiasta(),
                servico.getConfirmacaoConclusaoGuincho(),
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

    private ServicoDisponivelDTO mapearParaDisponivel(ServicoGuincho servico, Double distanciaKm) {
        return new ServicoDisponivelDTO(
                servico.getId(),
                servico.getEntusiasta().getNome(),
                distanciaKm,
                List.of(),
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
     * Chamado com a distancia calculada UMA unica vez. Ordem do painel
     * (MATCH): distancia asc -> nome do entusiasta (alfabetico).
     */
    private record ChamadoComDistancia(ServicoGuincho servico, Double distanciaKm) {

        boolean dentroDoRaio(double raioKm) {
            return distanciaKm != null && distanciaKm <= raioKm;
        }
    }

    private Comparator<ChamadoComDistancia> ordenacaoDosChamados() {
        return Comparator.comparing(ChamadoComDistancia::distanciaKm)
                .thenComparing(chamado -> chamado.servico().getEntusiasta().getNome());
    }
}