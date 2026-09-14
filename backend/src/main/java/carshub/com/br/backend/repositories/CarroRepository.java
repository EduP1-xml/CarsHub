package carshub.com.br.backend.repositories;

import carshub.com.br.backend.models.entities.Carro;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CarroRepository extends JpaRepository<Carro, UUID> {
    boolean existsByPlaca(String placa);
}