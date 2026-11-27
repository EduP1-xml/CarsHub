package carshub.com.br.backend.repositories;

import carshub.com.br.backend.models.dtos.carros.CarroGaragemProjecaoDTO;
import carshub.com.br.backend.models.entities.Carro;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
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

    /** Garagem completa do colecionador (seletor de carros compativeis com um clube). */
    List<Carro> findAllByColecionador_IdOrderByMarcaAscModeloAsc(UUID idColecionador);

    /**
     * CLUBES: aba Carros — so os carros VINCULADOS ao clube, na mesma projecao
     * da garagem (o service omite a placa e completa as fotos em lote).
     */
    @Query(value = """
            SELECT new carshub.com.br.backend.models.dtos.carros.CarroGaragemProjecaoDTO(
                   c.id, c.marca, c.modelo, c.placa, c.colecionador.entusiasta.nome,
                   c.carroceria, c.ano, c.cor, c.quantidadeCilindros, c.motor,
                   c.litragemMotor, c.injecao, c.alimentacao, c.combustivel, c.cambio,
                   c.tracao, c.suspensao, c.freio, c.direcao, c.roda, c.aro,
                   c.numeroPortas, c.numeroAssentos, c.materialRevestimentoInterno,
                   c.vidrosEletricos, c.som, c.arCondicionado, c.tetoSolar)
              FROM MembroClube m JOIN m.carrosVinculados c
             WHERE m.id.idClube = :idClube
             ORDER BY c.marca ASC, c.modelo ASC, c.id ASC
            """,
            countQuery = """
            SELECT COUNT(c) FROM MembroClube m JOIN m.carrosVinculados c
             WHERE m.id.idClube = :idClube
            """)
    Page<CarroGaragemProjecaoDTO> projetarCarrosVinculadosDoClube(@Param("idClube") UUID idClube,
                                                                  Pageable pageable);

    /** Fotos dos carros de UMA pagina em UMA consulta (par idCarro, url). */
    @Query("SELECT c.id, f FROM Carro c JOIN c.fotosCarro f WHERE c.id IN :ids")
    List<Object[]> buscarFotosDosCarros(@Param("ids") Collection<UUID> ids);
}
