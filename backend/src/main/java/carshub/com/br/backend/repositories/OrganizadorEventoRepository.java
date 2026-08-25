package carshub.com.br.backend.repositories;

import carshub.com.br.backend.models.entities.OrganizadorEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrganizadorEventoRepository extends JpaRepository<OrganizadorEvento, UUID> {
    boolean existsByUsuarioId(UUID usuarioId);
}
