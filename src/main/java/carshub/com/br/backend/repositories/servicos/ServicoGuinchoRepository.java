package carshub.com.br.backend.repositories.servicos;

import carshub.com.br.backend.models.entities.Guincho;
import carshub.com.br.backend.models.entities.servicos.ServicoGuincho;
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

public interface ServicoGuinchoRepository extends JpaRepository<ServicoGuincho, UUID> {

    // ==================================================================
    // LEITURAS
    // ==================================================================

    List<ServicoGuincho> findAllByEntusiasta_IdOrderByDataHoraSolicitacaoDesc(UUID idEntusiasta);

    List<ServicoGuincho> findAllByEntusiasta_IdAndStatusNotOrderByDataHoraSolicitacaoDesc(UUID idEntusiasta, StatusServico status);

    List<ServicoGuincho> findAllByGuincho_IdOrderByDataHoraSolicitacaoDesc(UUID idGuincho);

    List<ServicoGuincho> findAllByCarro_IdAndStatusOrderByDataHoraFinalizacaoDesc(UUID idCarro, StatusServico status);

    List<ServicoGuincho> findAllByStatusAndDataHoraSolicitacaoBefore(StatusServico status, LocalDateTime limite);

    long countByGuincho_IdAndStatusIn(UUID idGuincho, Collection<StatusServico> status);

    boolean existsByChaveIdempotencia(String chaveIdempotencia);

    /** API TIPADA do painel do guincho (enum + caixa de coordenadas). */
    default List<UUID> buscarIdsDisponiveisNaRegiao(StatusServico status,
                                                    DistanciaGeografica.CaixaCoordenadas caixa) {
        return buscarIdsDisponiveisNaRegiaoNativo(status.name(), DistanciaGeografica.envelopeWkt(caixa))
                .stream()
                .map(UUID::fromString)
                .toList();
    }

    @Query(value = """
            SELECT BIN_TO_UUID(s.id_ServicoGuincho)
              FROM ServicoGuincho s
             WHERE s.status_ServicoGuincho = :status
               AND ST_Within(s.localizacao_ServicoGuincho, ST_GeomFromText(:envelope))
            """, nativeQuery = true)
    List<String> buscarIdsDisponiveisNaRegiaoNativo(@Param("status") String status,
                                                    @Param("envelope") String envelope);

    /** Busca tipada das entidades dos IDs que vieram da consulta espacial. */
    @Query("SELECT s FROM ServicoGuincho s WHERE s.id IN (:ids)")
    List<ServicoGuincho> buscarPorIds(@Param("ids") Collection<UUID> ids);
    // ==================================================================
    // TRANSICOES CRITICAS (guardas atomicas)
    // ==================================================================

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServicoGuincho s
               SET s.guincho = :guincho,
                   s.valorProposto = :valorProposto,
                   s.latitudeAtualGuincho = :latitude,
                   s.longitudeAtualGuincho = :longitude,
                   s.dataHoraProposta = :dataHoraProposta,
                   s.status = :statusNovo
             WHERE s.id = :idServico
               AND s.status = :statusEsperado
            """)
    int registrarProposta(@Param("idServico") UUID idServico,
                          @Param("guincho") Guincho guincho,
                          @Param("valorProposto") BigDecimal valorProposto,
                          @Param("latitude") Double latitude,
                          @Param("longitude") Double longitude,
                          @Param("dataHoraProposta") LocalDateTime dataHoraProposta,
                          @Param("statusNovo") StatusServico statusNovo,
                          @Param("statusEsperado") StatusServico statusEsperado);

    /** Rejeicao ou expiracao: volta para a rede zerando prestador, valor e GPS DO GUINCHO. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServicoGuincho s
               SET s.guincho = null,
                   s.valorProposto = null,
                   s.latitudeAtualGuincho = null,
                   s.longitudeAtualGuincho = null,
                   s.dataHoraProposta = null,
                   s.status = :statusNovo
             WHERE s.id = :idServico
               AND s.status = :statusEsperado
            """)
    int devolverParaRede(@Param("idServico") UUID idServico,
                         @Param("statusNovo") StatusServico statusNovo,
                         @Param("statusEsperado") StatusServico statusEsperado);

    /**
     * Libera de uma vez TODAS as propostas abertas do guincho (usado quando ele
     * atinge a cota de 2 servicos ativos).
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServicoGuincho s
               SET s.guincho = null,
                   s.valorProposto = null,
                   s.latitudeAtualGuincho = null,
                   s.longitudeAtualGuincho = null,
                   s.dataHoraProposta = null,
                   s.status = :statusNovo
             WHERE s.guincho = :guincho
               AND s.status = :statusEsperado
            """)
    int liberarPropostasDoGuincho(@Param("guincho") Guincho guincho,
                                  @Param("statusNovo") StatusServico statusNovo,
                                  @Param("statusEsperado") StatusServico statusEsperado);

    /** Aceite: PROPOSTA -> PENDENTE gravando a data que libera o cancelamento em 2 h. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServicoGuincho s
               SET s.status = :statusNovo,
                   s.dataHoraPendente = :dataHoraPendente
             WHERE s.id = :idServico
               AND s.status = :statusEsperado
            """)
    int aceitarProposta(@Param("idServico") UUID idServico,
                        @Param("dataHoraPendente") LocalDateTime dataHoraPendente,
                        @Param("statusNovo") StatusServico statusNovo,
                        @Param("statusEsperado") StatusServico statusEsperado);

    /** CHEGADA: PENDENTE -> CHEGOU quando as DUAS confirmacoes ja estao gravadas. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServicoGuincho s
               SET s.status = :statusChegou,
                   s.dataHoraChegou = :dataHoraChegou
             WHERE s.id = :idServico
               AND s.status = :statusPendente
               AND s.confirmacaoChegadaEntusiasta = true
               AND s.confirmacaoChegadaGuincho = true
            """)
    int marcarChegou(@Param("idServico") UUID idServico,
                     @Param("dataHoraChegou") LocalDateTime dataHoraChegou,
                     @Param("statusChegou") StatusServico statusChegou,
                     @Param("statusPendente") StatusServico statusPendente);

    /**
     * FINALIZACAO: CHEGOU -> FINALIZADO com as duas confirmacoes de conclusao.
     * Zera a chave de idempotencia, liberando uma nova solicitacao igual.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServicoGuincho s
               SET s.status = :statusFinalizado,
                   s.dataHoraFinalizacao = :dataHoraFinalizacao,
                   s.chaveIdempotencia = null
             WHERE s.id = :idServico
               AND s.status = :statusChegou
               AND s.confirmacaoConclusaoEntusiasta = true
               AND s.confirmacaoConclusaoGuincho = true
            """)
    int finalizarSeAmbosConfirmaram(@Param("idServico") UUID idServico,
                                    @Param("dataHoraFinalizacao") LocalDateTime dataHoraFinalizacao,
                                    @Param("statusFinalizado") StatusServico statusFinalizado,
                                    @Param("statusChegou") StatusServico statusChegou);

    /** Usado pela PropostaExpiradaTask (30 minutos sem resposta). */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServicoGuincho s
               SET s.guincho = null,
                   s.valorProposto = null,
                   s.latitudeAtualGuincho = null,
                   s.longitudeAtualGuincho = null,
                   s.dataHoraProposta = null,
                   s.status = :statusNovo
             WHERE s.status = :statusEsperado
               AND s.dataHoraProposta < :limite
            """)
    int liberarPropostasExpiradas(@Param("limite") LocalDateTime limite,
                                  @Param("statusNovo") StatusServico statusNovo,
                                  @Param("statusEsperado") StatusServico statusEsperado);
}