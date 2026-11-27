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
import java.util.Collection;
import java.util.List;
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
                   e.cidade, e.uf, e.cep, e.latitude, e.longitude, e.organizadorEvento.nome,
                   e.organizadorEvento.login,
                   e.limiteParticipantes, e.dataHoraInicio, e.dataHoraFim)
              FROM Evento e
             WHERE e.status = :status
             ORDER BY e.dataHoraInicio ASC
            """)
    Page<CatalogoEventoProjecaoDTO> projetarCatalogoAtivos(@Param("status") StatusEvento status, Pageable pageable);

    /**
     * CLUBES: candidatos da aba Eventos — so os pares (idEvento, idFiltroCarro),
     * sem instanciar entidades. idFiltroCarro nulo = evento aberto a todos.
     */
    @Query("""
            SELECT e.id, f.id
              FROM Evento e LEFT JOIN e.filtroCarro f
             WHERE e.status = :status
             ORDER BY e.dataHoraInicio ASC
            """)
    List<Object[]> listarCandidatosAtivos(@Param("status") StatusEvento status);

    /** CLUBES: mesma projecao do catalogo, restrita aos eventos compativeis com o clube. */
    @Query("""
            SELECT new carshub.com.br.backend.models.dtos.eventos.CatalogoEventoProjecaoDTO(
                   e.id, e.nome, e.descricao, e.preco, e.rua, e.numero, e.bairro,
                   e.cidade, e.uf, e.cep, e.latitude, e.longitude, e.organizadorEvento.nome,
                   e.organizadorEvento.login,
                   e.limiteParticipantes, e.dataHoraInicio, e.dataHoraFim)
              FROM Evento e
             WHERE e.id IN :ids
             ORDER BY e.dataHoraInicio ASC
            """)
    Page<CatalogoEventoProjecaoDTO> projetarCatalogoPorIds(@Param("ids") Collection<UUID> ids, Pageable pageable);

    /** Par (idEvento, idFiltroCarro) dos eventos de uma pagina, sem inicializar o filtro. */
    @Query("SELECT e.id, e.filtroCarro.id FROM Evento e WHERE e.id IN :ids AND e.filtroCarro IS NOT NULL")
    List<Object[]> buscarIdsDosFiltros(@Param("ids") Collection<UUID> ids);

    /**
     * AVISO 7.4: fotos dos eventos de UMA pagina do catalogo levantadas em UMA
     * consulta (a colecao urlFotoEvento e lazy e nao entra na projecao de
     * escalares). O par (idEvento, url) e montado em Map pelo service.
     */
    @Query("""
            SELECT e.id, f
              FROM Evento e
              JOIN e.urlFotoEvento f
             WHERE e.id IN :ids
             ORDER BY e.dataHoraInicio ASC
            """)
    List<Object[]> buscarFotosDosEventos(@Param("ids") Collection<UUID> ids);

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
