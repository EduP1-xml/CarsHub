package carshub.com.br.backend.repositories;

import carshub.com.br.backend.models.entities.Inscricao;
import carshub.com.br.backend.models.enums.inscricoes.StatusInscricao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InscricaoRepository extends JpaRepository<Inscricao, UUID> {
    // Conta quantas inscrições confirmadas o evento possui (para barrar lotação)
    int countByEvento_IdEventoAndStatus(UUID idEvento, StatusInscricao status);
    
    // Verifica se o usuário já tem uma inscrição ativa no evento
    boolean existsByEntusiasta_IdAndEvento_IdEventoAndStatus(UUID idEntusiasta, UUID idEvento, StatusInscricao status);
    
    // Lista todas as inscrições de um usuário específico
    List<Inscricao> findAllByEntusiasta_Id(UUID idEntusiasta);
}