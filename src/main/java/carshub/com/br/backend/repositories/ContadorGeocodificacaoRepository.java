package carshub.com.br.backend.repositories;

import carshub.com.br.backend.models.entities.ContadorGeocodificacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ContadorGeocodificacaoRepository extends JpaRepository<ContadorGeocodificacao, UUID> {

    Optional<ContadorGeocodificacao> findByMesReferencia(String mesReferencia);
}