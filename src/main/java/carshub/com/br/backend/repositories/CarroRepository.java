package carshub.com.br.backend.repositories;

import carshub.com.br.backend.models.dtos.carros.CarroGaragemProjecaoDTO;
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
     * AJUSTE 1.1b + F13: PROJECAO da garagem — os escalares do carro vem direto
     * do SQL (ordem estavel pelo id), sem instanciar entidades Carro. A colecao
     * fotosCarro nao entra na projecao: o service completa com lista vazia.
     */
    @Query("""
            SELECT new carshub.com.br.backend.models.dtos.carros.CarroGaragemProjecaoDTO(
                   c.id, c.marca, c.modelo, c.placa, c.colecionador.entusiasta.nome,
                   c.carroceria, c.ano, c.cor, c.quantidadeCilindros, c.motor,
                   c.litragemMotor, c.injecao, c.alimentacao, c.combustivel, c.cambio,
                   c.tracao, c.suspensao, c.freio, c.direcao, c.roda, c.aro,
                   c.numeroPortas, c.numeroAssentos, c.materialRevestimentoInterno,
                   c.vidrosEletricos, c.som, c.arCondicionado, c.tetoSolar)
              FROM Carro c
             WHERE c.colecionador.id = :idColecionador
             ORDER BY c.id ASC
            """)
    Page<CarroGaragemProjecaoDTO> projetarGaragemDoColecionador(@Param("idColecionador") UUID idColecionador,
                                                                Pageable pageable);
}
