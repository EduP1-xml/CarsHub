package carshub.com.br.backend.repositories;

import carshub.com.br.backend.models.entities.MembroClube;
import carshub.com.br.backend.models.entities.MembroClubeId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MembroClubeRepository extends JpaRepository<MembroClube, MembroClubeId> {

    /** Membros do clube na ordem de entrada (o entusiasta ja vem junto, sem N+1). */
    @Query(value = """
            SELECT m FROM MembroClube m JOIN FETCH m.entusiasta
             WHERE m.id.idClube = :idClube
             ORDER BY m.dataEntrada ASC
            """,
            countQuery = "SELECT COUNT(m) FROM MembroClube m WHERE m.id.idClube = :idClube")
    Page<MembroClube> listarMembros(@Param("idClube") UUID idClube, Pageable pageable);

    /** GROUP BY: quantidade de membros por clube de uma pagina. */
    @Query("""
            SELECT m.id.idClube, COUNT(m) FROM MembroClube m
             WHERE m.id.idClube IN :ids
             GROUP BY m.id.idClube
            """)
    List<Object[]> contarMembrosPorClubes(@Param("ids") Collection<UUID> ids);

    /** GROUP BY: quantidade de carros vinculados por clube de uma pagina. */
    @Query("""
            SELECT m.id.idClube, COUNT(c) FROM MembroClube m JOIN m.carrosVinculados c
             WHERE m.id.idClube IN :ids
             GROUP BY m.id.idClube
            """)
    List<Object[]> contarCarrosVinculadosPorClubes(@Param("ids") Collection<UUID> ids);

    /** Dentre os clubes informados, os que o entusiasta ja e membro (flag souMembro). */
    @Query("""
            SELECT m.id.idClube FROM MembroClube m
             WHERE m.id.idEntusiasta = :idEntusiasta AND m.id.idClube IN :ids
            """)
    List<UUID> buscarIdsDeClubesDoMembro(@Param("idEntusiasta") UUID idEntusiasta,
                                         @Param("ids") Collection<UUID> ids);

    /** Resumo dos carros vinculados de uma pagina de membros: [idEntusiasta, idCarro, marca, modelo, ano, cor]. */
    @Query("""
            SELECT m.id.idEntusiasta, c.id, c.marca, c.modelo, c.ano, c.cor
              FROM MembroClube m JOIN m.carrosVinculados c
             WHERE m.id.idClube = :idClube AND m.id.idEntusiasta IN :idsEntusiastas
             ORDER BY c.marca ASC, c.modelo ASC
            """)
    List<Object[]> buscarCarrosVinculadosDosMembros(@Param("idClube") UUID idClube,
                                                    @Param("idsEntusiastas") Collection<UUID> idsEntusiastas);

    /** Ids dos carros que o membro ja vinculou ao clube. */
    @Query("""
            SELECT c.id FROM MembroClube m JOIN m.carrosVinculados c
             WHERE m.id.idClube = :idClube AND m.id.idEntusiasta = :idEntusiasta
            """)
    List<UUID> buscarIdsCarrosVinculados(@Param("idClube") UUID idClube,
                                         @Param("idEntusiasta") UUID idEntusiasta);

    @Query("""
            SELECT COUNT(m) > 0 FROM MembroClube m JOIN m.carrosVinculados c
             WHERE m.id.idClube = :idClube AND c.id = :idCarro
            """)
    boolean carroVinculadoAoClube(@Param("idClube") UUID idClube, @Param("idCarro") UUID idCarro);

    /** Todos os vinculos de um carro: [MembroClube, idFiltroCarro do clube, nome do clube]. */
    @Query("""
            SELECT m, cl.filtroCarro.id, cl.nome
              FROM MembroClube m JOIN m.clube cl JOIN m.carrosVinculados c
             WHERE c.id = :idCarro
            """)
    List<Object[]> buscarVinculosDoCarro(@Param("idCarro") UUID idCarro);

    /** Todos os pares (membro, carro vinculado) de um clube — reavaliacao apos mudar o filtro. */
    @Query("""
            SELECT m, c FROM MembroClube m JOIN m.carrosVinculados c
             WHERE m.id.idClube = :idClube
            """)
    List<Object[]> buscarVinculosDoClube(@Param("idClube") UUID idClube);
}
