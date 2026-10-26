package carshub.com.br.backend.models.dtos.servicos;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Avaliacao mutua feita ao final do servico (decisao C5).
 * AJUSTE 1.4 (decisao do time): o comentario e OPCIONAL e continua armazenado
 * nas colunas comentarioAvaliacao*_Servico* da entidade do servico (VARCHAR(500)
 * anulavel) — sem tabela separada, sem migracao. null = sem comentario.
 */
public record AvaliacaoServicoDTO(
        @NotNull(message = "A nota é obrigatória.")
        @Min(value = 1, message = "A nota mínima é 1.")
        @Max(value = 5, message = "A nota máxima é 5.")
        Integer nota,

        @Size(max = 500, message = "O comentário não pode exceder 500 caracteres.")
        String comentario
) {}