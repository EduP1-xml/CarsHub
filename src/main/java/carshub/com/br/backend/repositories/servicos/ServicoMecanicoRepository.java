package carshub.com.br.backend.repositories.servicos;

import carshub.com.br.backend.models.entities.Mecanico;
import carshub.com.br.backend.models.entities.servicos.ServicoMecanico;
import carshub.com.br.backend.models.enums.servicos.StatusServico;
import carshub.com.br.backend.utils.DistanciaGeografica;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ServicoMecanicoRepository extends JpaRepository<ServicoMecanico, UUID> {

    // ==================================================================
    // LEITURAS
    // ==================================================================

    List<ServicoMecanico> findAllByEntusiasta_IdOrderByDataHoraSolicitacaoDesc(UUID idEntusiasta);

    List<ServicoMecanico> findAllByEntusiasta_IdAndStatusNotOrderByDataHoraSolicitacaoDesc(UUID idEntusiasta, StatusServico status);

    List<ServicoMecanico> findAllByMecanico_IdOrderByDataHoraSolicitacaoDesc(UUID idMecanico);

    List<ServicoMecanico> findAllByCarro_IdAndStatusOrderByDataHoraFinalizacaoDesc(UUID idCarro, StatusServico status);

    List<ServicoMecanico> findAllByStatusAndDataHoraSolicitacaoBefore(StatusServico status, LocalDateTime limite);

    /** Cota de servicos ativos do prestador (no maximo 2). */
    long countByMecanico_IdAndStatusIn(UUID idMecanico, Collection<StatusServico> status);

    boolean existsByChaveIdempotencia(String chaveIdempotencia);

    /**
     * API TIPADA do painel: recebe o enum de status e a caixa de coordenadas.
     *
     * A conversao para texto acontece AQUI, sempre a partir do proprio valor
     * (StatusServico.name() e o WKT gerado pelo DistanciaGeografica) — nunca de um
     * literal. Se o enum for renomeado no futuro, a compilacao aponta esta linha.
     *
     * Motivo de o metodo anotado receber String: em consulta NATIVA o Hibernate nao
     * tem o contexto de @Enumerated(STRING) da entidade e tende a enviar o enum como
     * numero, e o MySQL compara coluna ENUM por POSICAO. Evitamos a questao.
     */
    default List<UUID> buscarIdsDisponiveisNaRegiao(StatusServico status,
                                                    DistanciaGeografica.CaixaCoordenadas caixa) {
        return buscarIdsDisponiveisNaRegiaoNativo(status.name(), DistanciaGeografica.envelopeWkt(caixa))
                .stream()
                .map(UUID::fromString)
                .toList();
    }

    /**
     * Candidatos do painel pelo INDICE ESPACIAL: chamados naquele status cuja
     * localizacao esta dentro do retangulo. Devolve apenas os IDs (em texto), porque
     * a coluna POINT nao esta mapeada na entidade.
     */
    @Query(value = """
            SELECT BIN_TO_UUID(s.id_ServicoMecanico)
              FROM ServicoMecanico s
             WHERE s.status_ServicoMecanico = :status
               AND ST_Within(s.localizacao_ServicoMecanico, ST_GeomFromText(:envelope))
            """, nativeQuery = true)
    List<String> buscarIdsDisponiveisNaRegiaoNativo(@Param("status") String status,
                                                    @Param("envelope") String envelope);

    /**
     * Entidades dos IDs candidatos, restritas aos chamados em que o mecanico esta na
     * lista de ELEGIVEIS.
     *
     * A comparacao do UUID fica aqui, em JPQL, para o Hibernate converter o
     * identificador para BINARY(16) pelo mapeamento da entidade — em SQL nativo isso
     * dependeria de UUID_TO_BIN e da ordem dos bytes.
     */
    @Query("""
            SELECT s FROM ServicoMecanico s
              JOIN s.mecanicosElegiveis m
             WHERE m.id = :idMecanico
               AND s.id IN (:ids)
            """)
    List<ServicoMecanico> buscarPorIdsElegiveis(@Param("ids") Collection<UUID> ids,
                                                @Param("idMecanico") UUID idMecanico);

    /**
     * carga ativa por mecanico (PENDENTE/CHEGOU) levantada em UMA
     * consulta para as listagens, evitando 1 count por chamado (N+1). O par
     * (id, contagem) e montado em Map pelo service.
     */
    @Query("""
            SELECT a.mecanico.id, COUNT(a)
              FROM ServicoMecanico a
             WHERE a.mecanico.id IN :ids
               AND a.status IN :statuses
             GROUP BY a.mecanico.id
            """)
    List<Object[]> contarAtivosPorMecanico(@Param("ids") Collection<UUID> ids,
                                           @Param("statuses") Collection<StatusServico> statuses);

    /**
     * MATCH (M4): mecanicos da lista de ELEGIVEIS gravada na criacao, menos os
     * que estao com a cota cheia (2 ativos) — quem nao pode receber proposta nem
     * aparece para o colecionador.
     */
    @Query("""
            SELECT m FROM ServicoMecanico s
              JOIN s.mecanicosElegiveis m
             WHERE s.id = :idServico
               AND m.ativo = true
               AND (SELECT COUNT(a) FROM ServicoMecanico a
                     WHERE a.mecanico = m
                       AND a.status IN :statusAtivos) < :maximoAtivos
            """)
    List<Mecanico> buscarElegiveisComCotaDisponivel(@Param("idServico") UUID idServico,
                                                    @Param("statusAtivos") Collection<StatusServico> statusAtivos,
                                                    @Param("maximoAtivos") long maximoAtivos);

    // ==================================================================
    // TRANSICOES CRITICAS (guardas atomicas)
    // ==================================================================

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServicoMecanico s
               SET s.mecanico = :mecanico,
                   s.valorProposto = :valorProposto,
                   s.latitudeAtualMecanico = :latitude,
                   s.longitudeAtualMecanico = :longitude,
                   s.dataHoraProposta = :dataHoraProposta,
                   s.status = :statusNovo,
                   s.quantidadePropostas = s.quantidadePropostas + 1
             WHERE s.id = :idServico
               AND s.status = :statusEsperado
               AND s.quantidadePropostas < :maximoPropostas
            """)
    int registrarProposta(@Param("idServico") UUID idServico,
                          @Param("mecanico") Mecanico mecanico,
                          @Param("valorProposto") BigDecimal valorProposto,
                          @Param("latitude") Double latitude,
                          @Param("longitude") Double longitude,
                          @Param("dataHoraProposta") LocalDateTime dataHoraProposta,
                          @Param("statusNovo") StatusServico statusNovo,
                          @Param("statusEsperado") StatusServico statusEsperado,
                          @Param("maximoPropostas") int maximoPropostas);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServicoMecanico s
               SET s.mecanico = null,
                   s.valorProposto = null,
                   s.latitudeAtualMecanico = null,
                   s.longitudeAtualMecanico = null,
                   s.dataHoraProposta = null,
                   s.status = :statusNovo
             WHERE s.id = :idServico
               AND s.status = :statusEsperado
            """)
    int devolverParaRede(@Param("idServico") UUID idServico,
                         @Param("statusNovo") StatusServico statusNovo,
                         @Param("statusEsperado") StatusServico statusEsperado);

    /**
     * Libera de uma vez TODAS as propostas abertas do mecanico (usado quando ele
     * atinge a cota de 2 servicos ativos).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServicoMecanico s
               SET s.mecanico = null,
                   s.valorProposto = null,
                   s.latitudeAtualMecanico = null,
                   s.longitudeAtualMecanico = null,
                   s.dataHoraProposta = null,
                   s.status = :statusNovo
             WHERE s.mecanico = :mecanico
               AND s.status = :statusEsperado
            """)
    int liberarPropostasDoMecanico(@Param("mecanico") Mecanico mecanico,
                                   @Param("statusNovo") StatusServico statusNovo,
                                   @Param("statusEsperado") StatusServico statusEsperado);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServicoMecanico s
               SET s.status = :statusNovo,
                   s.dataHoraPendente = :dataHoraPendente
             WHERE s.id = :idServico
               AND s.status = :statusEsperado
            """)
    int aceitarProposta(@Param("idServico") UUID idServico,
                        @Param("dataHoraPendente") LocalDateTime dataHoraPendente,
                        @Param("statusNovo") StatusServico statusNovo,
                        @Param("statusEsperado") StatusServico statusEsperado);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServicoMecanico s
               SET s.status = :statusChegou,
                   s.dataHoraChegou = :dataHoraChegou
             WHERE s.id = :idServico
               AND s.status = :statusPendente
               AND s.confirmacaoChegadaEntusiasta = true
               AND s.confirmacaoChegadaMecanico = true
            """)
    int marcarChegou(@Param("idServico") UUID idServico,
                     @Param("dataHoraChegou") LocalDateTime dataHoraChegou,
                     @Param("statusChegou") StatusServico statusChegou,
                     @Param("statusPendente") StatusServico statusPendente);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServicoMecanico s
               SET s.status = :statusFinalizado,
                   s.dataHoraFinalizacao = :dataHoraFinalizacao,
                   s.chaveIdempotencia = null
             WHERE s.id = :idServico
               AND s.status = :statusChegou
               AND s.confirmacaoConclusaoEntusiasta = true
               AND s.confirmacaoConclusaoMecanico = true
            """)
    int finalizarSeAmbosConfirmaram(@Param("idServico") UUID idServico,
                                    @Param("dataHoraFinalizacao") LocalDateTime dataHoraFinalizacao,
                                    @Param("statusFinalizado") StatusServico statusFinalizado,
                                    @Param("statusChegou") StatusServico statusChegou);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServicoMecanico s
               SET s.mecanico = null,
                   s.valorProposto = null,
                   s.latitudeAtualMecanico = null,
                   s.longitudeAtualMecanico = null,
                   s.dataHoraProposta = null,
                   s.status = :statusNovo
             WHERE s.status = :statusEsperado
               AND s.dataHoraProposta < :limite
            """)
    int liberarPropostasExpiradas(@Param("limite") LocalDateTime limite,
                                  @Param("statusNovo") StatusServico statusNovo,
                                  @Param("statusEsperado") StatusServico statusEsperado);
}