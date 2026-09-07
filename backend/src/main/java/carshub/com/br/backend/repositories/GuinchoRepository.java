package carshub.com.br.backend.repositories;

import carshub.com.br.backend.models.entities.Guincho;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GuinchoRepository extends JpaRepository<Guincho, UUID> {
    boolean existsByPlaca(String placa);
}
