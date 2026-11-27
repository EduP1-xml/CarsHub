package carshub.com.br.backend.repositories;

import carshub.com.br.backend.models.entities.FiltroCarro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Carregamento EM LOTE dos filtros (padrao do buscarFotosDosEventos): uma
 * consulta para os escalares + uma por colecao, sempre com "IN :ids". Quem
 * monta os objetos e o CarregadorFiltroCarro — total fixo de 16 consultas,
 * qualquer que seja o numero de filtros.
 */
public interface FiltroCarroRepository extends JpaRepository<FiltroCarro, UUID> {

    /** [id, anoMin, anoMax, cilMin, cilMax, litMin, litMax, aroMin, aroMax, portasMin, portasMax, assentosMin, assentosMax, som] */
    @Query("""
            SELECT f.id, f.anoMin, f.anoMax, f.quantidadeCilindrosMin, f.quantidadeCilindrosMax,
                   f.litragemMotorMin, f.litragemMotorMax, f.aroMin, f.aroMax,
                   f.numeroPortasMin, f.numeroPortasMax, f.numeroAssentosMin, f.numeroAssentosMax, f.som
              FROM FiltroCarro f
             WHERE f.id IN :ids
            """)
    List<Object[]> buscarEscalares(@Param("ids") Collection<UUID> ids);

    @Query("SELECT f.id, v FROM FiltroCarro f JOIN f.marcas v WHERE f.id IN :ids")
    List<Object[]> buscarMarcas(@Param("ids") Collection<UUID> ids);

    @Query("SELECT f.id, v FROM FiltroCarro f JOIN f.modelos v WHERE f.id IN :ids")
    List<Object[]> buscarModelos(@Param("ids") Collection<UUID> ids);

    @Query("SELECT f.id, v FROM FiltroCarro f JOIN f.cores v WHERE f.id IN :ids")
    List<Object[]> buscarCores(@Param("ids") Collection<UUID> ids);

    @Query("SELECT f.id, v FROM FiltroCarro f JOIN f.carrocerias v WHERE f.id IN :ids")
    List<Object[]> buscarCarrocerias(@Param("ids") Collection<UUID> ids);

    @Query("SELECT f.id, v FROM FiltroCarro f JOIN f.motores v WHERE f.id IN :ids")
    List<Object[]> buscarMotores(@Param("ids") Collection<UUID> ids);

    @Query("SELECT f.id, v FROM FiltroCarro f JOIN f.injecoes v WHERE f.id IN :ids")
    List<Object[]> buscarInjecoes(@Param("ids") Collection<UUID> ids);

    @Query("SELECT f.id, v FROM FiltroCarro f JOIN f.alimentacoes v WHERE f.id IN :ids")
    List<Object[]> buscarAlimentacoes(@Param("ids") Collection<UUID> ids);

    @Query("SELECT f.id, v FROM FiltroCarro f JOIN f.combustiveis v WHERE f.id IN :ids")
    List<Object[]> buscarCombustiveis(@Param("ids") Collection<UUID> ids);

    @Query("SELECT f.id, v FROM FiltroCarro f JOIN f.cambios v WHERE f.id IN :ids")
    List<Object[]> buscarCambios(@Param("ids") Collection<UUID> ids);

    @Query("SELECT f.id, v FROM FiltroCarro f JOIN f.tracoes v WHERE f.id IN :ids")
    List<Object[]> buscarTracoes(@Param("ids") Collection<UUID> ids);

    @Query("SELECT f.id, v FROM FiltroCarro f JOIN f.suspensoes v WHERE f.id IN :ids")
    List<Object[]> buscarSuspensoes(@Param("ids") Collection<UUID> ids);

    @Query("SELECT f.id, v FROM FiltroCarro f JOIN f.freios v WHERE f.id IN :ids")
    List<Object[]> buscarFreios(@Param("ids") Collection<UUID> ids);

    @Query("SELECT f.id, v FROM FiltroCarro f JOIN f.direcoes v WHERE f.id IN :ids")
    List<Object[]> buscarDirecoes(@Param("ids") Collection<UUID> ids);

    @Query("SELECT f.id, v FROM FiltroCarro f JOIN f.rodas v WHERE f.id IN :ids")
    List<Object[]> buscarRodas(@Param("ids") Collection<UUID> ids);

    @Query("SELECT f.id, v FROM FiltroCarro f JOIN f.materiaisRevestimentoInterno v WHERE f.id IN :ids")
    List<Object[]> buscarMateriaisRevestimentoInterno(@Param("ids") Collection<UUID> ids);
}
