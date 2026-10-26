package carshub.com.br.backend.repositories;

import carshub.com.br.backend.models.dtos.carros.CarroRespostaDTO;
import carshub.com.br.backend.models.entities.Carro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface CarroRepository extends JpaRepository<Carro, UUID> {
    boolean existsByPlaca(String placa);

    /**
     * AJUSTE 1.1b: PROJECAO da garagem — os 5 escalares do CarroRespostaDTO vem
     * direto do SQL (ordem estavel pelo id), sem instanciar entidades Carro.
     */
    @Query("""
            SELECT new carshub.com.br.backend.models.dtos.carros.CarroRespostaDTO(
                   c.id, c.marca, c.modelo, c.placa, c.colecionador.entusiasta.nome)
              FROM Carro c
             WHERE c.colecionador.id = :idColecionador
             ORDER BY c.id ASC
            """)
    Page<CarroRespostaDTO> projetarGaragemDoColecionador(@Param("idColecionador") UUID idColecionador,
                                                         Pageable pageable);
}
