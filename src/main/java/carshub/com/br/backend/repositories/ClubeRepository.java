package carshub.com.br.backend.repositories;

import carshub.com.br.backend.models.entities.Clube;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ClubeRepository extends JpaRepository<Clube, UUID> {

    boolean existsByNomeIgnoreCase(String nome);

    boolean existsByNomeIgnoreCaseAndIdNot(String nome, UUID id);

    /** Todos os clubes (busca opcional por nome), em ordem alfabetica. */
    @Query(value = """
            SELECT c FROM Clube c JOIN FETCH c.administrador
             WHERE :busca IS NULL OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :busca, '%'))
             ORDER BY c.nome ASC
            """,
            countQuery = """
            SELECT COUNT(c) FROM Clube c
             WHERE :busca IS NULL OR LOWER(c.nome) LIKE LOWER(CONCAT('%', :busca, '%'))
            """)
    Page<Clube> buscarPorNome(@Param("busca") String busca, Pageable pageable);

    /** Clubes de que o entusiasta e membro, do mais recente para o mais antigo. */
    @Query(value = """
            SELECT c FROM MembroClube m JOIN m.clube c JOIN FETCH c.administrador
             WHERE m.id.idEntusiasta = :idEntusiasta
             ORDER BY m.dataEntrada DESC
            """,
            countQuery = "SELECT COUNT(m) FROM MembroClube m WHERE m.id.idEntusiasta = :idEntusiasta")
    Page<Clube> buscarClubesDoEntusiasta(@Param("idEntusiasta") UUID idEntusiasta, Pageable pageable);

    /** Fotos dos clubes de UMA pagina em UMA consulta (par idClube, url). */
    @Query("SELECT c.id, f FROM Clube c JOIN c.fotosClube f WHERE c.id IN :ids")
    List<Object[]> buscarFotosDosClubes(@Param("ids") Collection<UUID> ids);

    /** Par (idClube, idFiltroCarro) sem inicializar o filtro. */
    @Query("SELECT c.id, c.filtroCarro.id FROM Clube c WHERE c.id IN :ids")
    List<Object[]> buscarIdsDosFiltros(@Param("ids") Collection<UUID> ids);
}
