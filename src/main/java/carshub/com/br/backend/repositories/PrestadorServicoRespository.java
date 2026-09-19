package carshub.com.br.backend.repositories;

import carshub.com.br.backend.models.entities.PrestadorServico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PrestadorServicoRespository extends JpaRepository<PrestadorServico, UUID> {
    boolean existsByCnpj(String cnpj);
}
