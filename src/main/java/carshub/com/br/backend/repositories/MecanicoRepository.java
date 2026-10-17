package carshub.com.br.backend.repositories;

import carshub.com.br.backend.models.entities.Mecanico;
import carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface MecanicoRepository extends JpaRepository<Mecanico, UUID> {

    /**
     * Mecanicos ATIVOS que ofertam TODOS os servicos informados.
     *
     * O HAVING e o que garante "todos": quem tem apenas um dos servicos soma
     * menos que o total e fica de fora. E a base da elegibilidade decidida na
     * criacao da solicitacao.
     */
    @Query("""
            SELECT m
              FROM Mecanico m JOIN m.servicos s
             WHERE m.ativo = true
               AND s IN :servicos
             GROUP BY m
            HAVING COUNT(DISTINCT s) = :quantidadeServicos
            """)
    List<Mecanico> buscarCompativeisComTodos(@Param("servicos") Collection<ServicosOfertados> servicos,
                                             @Param("quantidadeServicos") long quantidadeServicos);
}