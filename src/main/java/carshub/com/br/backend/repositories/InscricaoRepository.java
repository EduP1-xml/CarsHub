package carshub.com.br.backend.repositories;

import carshub.com.br.backend.models.entities.Inscricao;
import carshub.com.br.backend.models.enums.inscricoes.StatusInscricao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface InscricaoRepository extends JpaRepository<Inscricao, UUID> {
    // Conta quantas inscrições confirmadas o evento possui (para barrar lotação)
    int countByEvento_IdAndStatus(UUID id, StatusInscricao status);
    
    // Verifica se o usuário já tem uma inscrição ativa no evento
    boolean existsByEntusiasta_IdAndEvento_IdAndStatus(UUID idEntusiasta, UUID id, StatusInscricao status);
    
    // Lista todas as inscrições de um usuário específico
    List<Inscricao> findAllByEntusiasta_Id(UUID idEntusiasta);

    // Soma todas as inscrições CONFIRMADAS de todos os eventos pertencentes a um organizador específico
    @Query("SELECT COUNT(i) FROM Inscricao i WHERE i.evento.organizadorEvento.id = :idOrganizador AND i.status = 'CONFIRMADA'")
    Integer countTotalParticipantesByOrganizador(@Param("idOrganizador") UUID idOrganizador);

    // Ajuda a montar o histórico: Conta os participantes de um evento específico que já foi concluído
    int countByEvento_Id(UUID id);
}