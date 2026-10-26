package carshub.com.br.backend.repositories;

import carshub.com.br.backend.models.dtos.dashboards.EventoHistoricoProjecaoDTO;
import carshub.com.br.backend.models.dtos.eventos.CatalogoEventoProjecaoDTO;
import carshub.com.br.backend.models.entities.Evento;
import carshub.com.br.backend.models.enums.eventos.StatusEvento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.UUID;

public interface EventoRepository extends JpaRepository<Evento, UUID> {

    // ==================================================================
    // LEITURAS PAGINADAS (AJUSTE 1.1)
    // ==================================================================

    /** Meus eventos: do mais recente (data de inicio) para o mais antigo. O DTO tem fotos -> entidade + batch. */
    Page<Evento> findAllByOrganizadorEvento_IdOrderByDataHoraInicioDesc(UUID idOrganizador, Pageable pageable);

    /**
     * AJUSTE 1.1b: PROJECAO do catalogo — os escalares do DTO vem direto do SQL,
     * sem instanciar entidades Evento. vagas/inscrito sao completados pelo
     * service com os GROUP BYs de Inscricao.
     */
    @Query("""
            SELECT new carshub.com.br.backend.models.dtos.eventos.CatalogoEventoProjecaoDTO(
                   e.id, e.nome, e.descricao, e.preco, e.rua, e.numero, e.bairro,
                   e.latitude, e.longitude, e.organizadorEvento.nome, e.limiteParticipantes,
                   e.dataHoraInicio, e.dataHoraFim)
              FROM Evento e
             WHERE e.status = :status
             ORDER BY e.dataHoraInicio ASC
            """)
    Page<CatalogoEventoProjecaoDTO> projetarCatalogoAtivos(@Param("status") StatusEvento status, Pageable pageable);

    /**
     * AJUSTE 1.1b: PROJECAO do historico do dashboard. Traz a dataHoraFim porque
     * e a chave da mesclagem CONCLUIDO + CANCELADO em Java (janela).
     */
    @Query("""
            SELECT new carshub.com.br.backend.models.dtos.dashboards.EventoHistoricoProjecaoDTO(
                   e.id, e.nome, e.status, e.limiteParticipantes, e.dataHoraFim)
              FROM Evento e
             WHERE e.organizadorEvento.id = :idOrganizador
               AND e.status = :status
             ORDER BY e.dataHoraFim DESC
            """)
    Page<EventoHistoricoProjecaoDTO> projetarHistoricoPorOrganizador(@Param("idOrganizador") UUID idOrganizador,
                                                                     @Param("status") StatusEvento status,
                                                                     Pageable pageable);

    // Conta quantos eventos o organizador já criou na vida
    int countByOrganizadorEvento_Id(UUID idOrganizador);

    // ==================================================================
    // TRANSICOES (AJUSTE 1.3)
    // ==================================================================

    /**
     * O @Version das entidades protege o PUT/PATCH concorrente, mas
     * a task automatica NAO pode disputar linha com um humano — entao ela usa
     * UPDATE condicional em massa, como as transicoes de Servico*.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            UPDATE Evento e
               SET e.status = :statusNovo
             WHERE e.status = :statusAtual
               AND e.dataHoraFim < :agora
            """)
    int concluirEventosExpirados(@Param("statusNovo") StatusEvento statusNovo,
                                 @Param("statusAtual") StatusEvento statusAtual,
                                 @Param("agora") LocalDateTime agora);
}
