package carshub.com.br.backend.repositories.servicos;

import carshub.com.br.backend.models.entities.Guincho;
import carshub.com.br.backend.models.entities.servicos.ServicoGuincho;
import carshub.com.br.backend.models.enums.servicos.StatusServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    /**
     * Painel do guincho: chamados SOLICITADO dentro do bounding box do raio, com
     * a distancia (Haversine, km) calculada uma unica vez no SELECT.
     * Ver ServicoMecanicoRepository para a explicacao do least(1.0, ...).
     */
    @Query("""
            SELECT s,
                   (6371 * acos(least(1.0,
                        cos(function('radians', :latitude)) * cos(function('radians', s.latitudeAtualEntusiasta))
                            * cos(function('radians', s.longitudeAtualEntusiasta) - function('radians', :longitude))
                        + sin(function('radians', :latitude)) * sin(function('radians', s.latitudeAtualEntusiasta))
                   )))
              FROM ServicoGuincho s
             WHERE s.status = :statusSolicitado
               AND s.latitudeAtualEntusiasta BETWEEN :latitudeMinima AND :latitudeMaxima
               AND s.longitudeAtualEntusiasta BETWEEN :longitudeMinima AND :longitudeMaxima
            """)
    List<Object[]> buscarDisponiveisNaRegiao(@Param("latitude") Double latitude,
                                             @Param("longitude") Double longitude,
                                             @Param("latitudeMinima") Double latitudeMinima,
                                             @Param("latitudeMaxima") Double latitudeMaxima,
                                             @Param("longitudeMinima") Double longitudeMinima,
                                             @Param("longitudeMaxima") Double longitudeMaxima,
                                             @Param("statusSolicitado") StatusServico statusSolicitado);

    // ==================================================================
    // TRANSICOES DE ESTADO (guardas identicas as do servico mecanico)
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServicoGuincho s
               SET s.status = :statusNovo
             WHERE s.id = :idServico
               AND s.status = :statusEsperado
            """)
    int alterarStatus(@Param("idServico") UUID idServico,
                      @Param("statusNovo") StatusServico statusNovo,
                      @Param("statusEsperado") StatusServico statusEsperado);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServicoGuincho s
               SET s.confirmacaoConclusaoEntusiasta = true
             WHERE s.id = :idServico
               AND s.status = :statusEsperado
               AND s.confirmacaoConclusaoEntusiasta = false
            """)
    int confirmarConclusaoEntusiasta(@Param("idServico") UUID idServico,
                                     @Param("statusEsperado") StatusServico statusEsperado);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServicoGuincho s
               SET s.confirmacaoConclusaoGuincho = true
             WHERE s.id = :idServico
               AND s.status = :statusEsperado
               AND s.confirmacaoConclusaoGuincho = false
            """)
    int confirmarConclusaoGuincho(@Param("idServico") UUID idServico,
                                  @Param("statusEsperado") StatusServico statusEsperado);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServicoGuincho s
               SET s.status = :statusFinalizado,
                   s.dataHoraFinalizacao = :dataHoraFinalizacao
             WHERE s.id = :idServico
               AND s.status = :statusPendente
               AND s.confirmacaoConclusaoEntusiasta = true
               AND s.confirmacaoConclusaoGuincho = true
            """)
    int finalizarSeAmbosConfirmaram(@Param("idServico") UUID idServico,
                                    @Param("dataHoraFinalizacao") LocalDateTime dataHoraFinalizacao,
                                    @Param("statusFinalizado") StatusServico statusFinalizado,
                                    @Param("statusPendente") StatusServico statusPendente);

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