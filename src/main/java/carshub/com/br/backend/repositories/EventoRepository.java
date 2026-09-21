package carshub.com.br.backend.repositories;

import carshub.com.br.backend.models.entities.Evento;
import carshub.com.br.backend.models.enums.eventos.StatusEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EventoRepository extends JpaRepository<Evento, UUID> {
    List<Evento> findAllByOrganizadorEvento_Id(UUID idOrganizador);
    // Retorna apenas eventos com um status específico (ex: ATIVO)
    List<Evento> findAllByStatus(StatusEvento status);
}
