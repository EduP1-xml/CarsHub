package carshub.com.br.backend.repositories;

import carshub.com.br.backend.models.entities.Entusiasta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface EntusiastaRepository extends JpaRepository<Entusiasta, UUID> {
    boolean existsByUsuarioId(UUID usuarioId);
}
