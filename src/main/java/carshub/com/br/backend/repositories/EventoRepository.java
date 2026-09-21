package carshub.com.br.backend.repositories;

import carshub.com.br.backend.models.entities.Evento;
import carshub.com.br.backend.models.enums.eventos.StatusEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface EventoRepository extends JpaRepository<Evento, UUID> {
    List<Evento> findAllByOrganizadorEvento_Id(UUID idOrganizador);
    // Retorna apenas eventos com um status específico (ex: ATIVO)
    List<Evento> findAllByStatus(StatusEvento status);

    // Conta quantos eventos o organizador já criou na vida
    int countByOrganizadorEvento_Id(UUID idOrganizador);

    // Busca apenas eventos de um organizador com um status específico (ex: CONCLUIDO ou CANCELADO)
    List<Evento> findAllByOrganizadorEvento_IdAndStatus(UUID idOrganizador, StatusEvento status);

    // Busca eventos com um status específico cuja data de fim já passou
    List<Evento> findByStatusAndDataHoraFimBefore(StatusEvento status, LocalDateTime dataAtual);
}
