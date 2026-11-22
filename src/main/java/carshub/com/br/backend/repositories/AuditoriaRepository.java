package carshub.com.br.backend.repositories;

import carshub.com.br.backend.models.entities.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * SPRINT 6 (11.9): persistencia da trilha de auditoria (append-only).
 */
public interface AuditoriaRepository extends JpaRepository<Auditoria, UUID> {
}
