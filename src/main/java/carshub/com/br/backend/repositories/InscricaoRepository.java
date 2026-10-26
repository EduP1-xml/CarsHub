package carshub.com.br.backend.repositories;

import carshub.com.br.backend.models.entities.Inscricao;
import carshub.com.br.backend.models.enums.inscricoes.StatusInscricao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface InscricaoRepository extends JpaRepository<Inscricao, UUID> {
    // Conta quantas inscrições confirmadas o evento possui (para barrar lotação)
    int countByEvento_IdAndStatus(UUID id, StatusInscricao status);
    
    // Verifica se o usuário já tem uma inscrição ativa no evento
    boolean existsByEntusiasta_IdAndEvento_IdAndStatus(UUID idEntusiasta, UUID id, StatusInscricao status);
    
    // Lista todas as inscrições de um usuário específico
    List<Inscricao> findAllByEntusiasta_Id(UUID idEntusiasta);

    // Soma todas as inscrições CONFIRMADAS de todos os eventos de um organizador
    @Query("SELECT COUNT(i) FROM Inscricao i WHERE i.evento.organizadorEvento.id = :idOrganizador AND i.status = :status")
    Integer countTotalParticipantesByOrganizador(@Param("idOrganizador") UUID idOrganizador,
                                                 @Param("status") StatusInscricao status);

    // Ajuda a montar o histórico: Conta os participantes de um evento específico que já foi concluído
    int countByEvento_Id(UUID id);

    // Integrantes levantados em UMA consulta por pagina do catalogo(evita o N+1)

    @Query("SELECT i.evento.id, COUNT(i) FROM Inscricao i WHERE i.evento.id IN :ids AND i.status = :status GROUP BY i.evento.id")
    List<Object[]> contarConfirmadasPorEventos(@Param("ids") Collection<UUID> ids,
                                               @Param("status") StatusInscricao status);

    @Query("SELECT DISTINCT i.evento.id FROM Inscricao i WHERE i.entusiasta.id = :idEntusiasta AND i.evento.id IN :ids AND i.status = :status")
    List<UUID> buscarIdsDeEventosInscritos(@Param("idEntusiasta") UUID idEntusiasta,
                                           @Param("ids") Collection<UUID> ids,
                                           @Param("status") StatusInscricao status);

    @Query("SELECT i.evento.id, COUNT(i) FROM Inscricao i WHERE i.evento.id IN :ids GROUP BY i.evento.id")
    List<Object[]> contarTodasPorEventos(@Param("ids") Collection<UUID> ids);
}