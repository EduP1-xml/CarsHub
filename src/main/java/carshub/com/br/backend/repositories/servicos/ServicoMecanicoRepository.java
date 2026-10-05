package carshub.com.br.backend.repositories.servicos;

import carshub.com.br.backend.models.entities.Mecanico;
import carshub.com.br.backend.models.entities.servicos.ServicoMecanico;
import carshub.com.br.backend.models.enums.servicos.StatusServico;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface ServicoMecanicoRepository extends JpaRepository<ServicoMecanico, UUID> {

    List<ServicoMecanico> findAllByEntusiasta_IdOrderByDataHoraSolicitacaoDesc(UUID idEntusiasta);

    List<ServicoMecanico> findAllByEntusiasta_IdAndStatusNotOrderByDataHoraSolicitacaoDesc(UUID idEntusiasta, StatusServico status);

    List<ServicoMecanico> findAllByMecanico_IdOrderByDataHoraSolicitacaoDesc(UUID idMecanico);

    List<ServicoMecanico> findAllByCarro_IdAndStatusOrderByDataHoraFinalizacaoDesc(UUID idCarro, StatusServico status);

    /**
     * Painel do mecanico: chamados SOLICITADO dentro do bounding box do raio.
     *
     * A distancia (Haversine, em km) e calculada UMA unica vez, no SELECT, e
     * devolvida como segundo item de cada linha. O WHERE so faz o pre-filtro
     * barato por caixa de coordenadas; o filtro exato do raio e feito em Java,
     * junto com a ordenacao.
     *
     * O least(1.0, ...) evita que o ACOS receba um valor levemente maior que 1
     * (dois pontos praticamente no mesmo lugar), o que devolveria NULL no MySQL
     * e sumiria com o chamado. Se o seu Hibernate nao conhecer least() em HQL,
     * troque por function('least', 1.0, <expressao>).
     */
    @Query("""
            SELECT s,
                   (6371 * acos(least(1.0,
                        cos(function('radians', :latitude)) * cos(function('radians', s.latitudeAtualEntusiasta))
                            * cos(function('radians', s.longitudeAtualEntusiasta) - function('radians', :longitude))
                        + sin(function('radians', :latitude)) * sin(function('radians', s.latitudeAtualEntusiasta))
                   )))
              FROM ServicoMecanico s
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
    // TRANSICOES DE ESTADO
    //
    // Todas usam UPDATE condicional: o "AND status = :statusEsperado" e a guarda
    // que impede duas pessoas de mudarem o mesmo chamado ao mesmo tempo (o mesmo
    // defeito de check-then-act que existe no InscricaoService). Se o metodo
    // devolver 0, o estado mudou no meio do caminho e o service lanca 422.
    // ==================================================================

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServicoMecanico s
               SET s.mecanico = :mecanico,
                   s.valorProposto = :valorProposto,
                   s.latitudeAtualMecanico = :latitude,
                   s.longitudeAtualMecanico = :longitude,
                   s.dataHoraProposta = :dataHoraProposta,
                   s.status = :statusNovo
             WHERE s.id = :idServico
               AND s.status = :statusEsperado
            """)
    int registrarProposta(@Param("idServico") UUID idServico,
                          @Param("mecanico") Mecanico mecanico,
                          @Param("valorProposto") BigDecimal valorProposto,
                          @Param("latitude") Double latitude,
                          @Param("longitude") Double longitude,
                          @Param("dataHoraProposta") LocalDateTime dataHoraProposta,
                          @Param("statusNovo") StatusServico statusNovo,
                          @Param("statusEsperado") StatusServico statusEsperado);

    /** Rejeicao (ou expiracao): volta para a rede zerando prestador, valor e GPS. */
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServicoMecanico s
               SET s.status = :statusNovo
             WHERE s.id = :idServico
               AND s.status = :statusEsperado
            """)
    int alterarStatus(@Param("idServico") UUID idServico,
                      @Param("statusNovo") StatusServico statusNovo,
                      @Param("statusEsperado") StatusServico statusEsperado);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServicoMecanico s
               SET s.confirmacaoConclusaoEntusiasta = true
             WHERE s.id = :idServico
               AND s.status = :statusEsperado
               AND s.confirmacaoConclusaoEntusiasta = false
            """)
    int confirmarConclusaoEntusiasta(@Param("idServico") UUID idServico,
                                     @Param("statusEsperado") StatusServico statusEsperado);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServicoMecanico s
               SET s.confirmacaoConclusaoMecanico = true
             WHERE s.id = :idServico
               AND s.status = :statusEsperado
               AND s.confirmacaoConclusaoMecanico = false
            """)
    int confirmarConclusaoMecanico(@Param("idServico") UUID idServico,
                                   @Param("statusEsperado") StatusServico statusEsperado);

    /**
     * Handshake duplo: so finaliza quando as DUAS confirmacoes ja estao gravadas.
     * Como a condicao inteira esta no WHERE, duas confirmacoes simultaneas nao
     * conseguem finalizar o mesmo chamado duas vezes.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE ServicoMecanico s
               SET s.status = :statusFinalizado,
                   s.dataHoraFinalizacao = :dataHoraFinalizacao
             WHERE s.id = :idServico
               AND s.status = :statusPendente
               AND s.confirmacaoConclusaoEntusiasta = true
               AND s.confirmacaoConclusaoMecanico = true
            """)
    int finalizarSeAmbosConfirmaram(@Param("idServico") UUID idServico,
                                    @Param("dataHoraFinalizacao") LocalDateTime dataHoraFinalizacao,
                                    @Param("statusFinalizado") StatusServico statusFinalizado,
                                    @Param("statusPendente") StatusServico statusPendente);

    /** Usado pela PropostaExpiradaTask: propostas vencidas voltam para a rede. */
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