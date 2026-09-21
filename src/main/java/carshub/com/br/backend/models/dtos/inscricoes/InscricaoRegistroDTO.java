package carshub.com.br.backend.models.dtos.inscricoes;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record InscricaoRegistroDTO(
        @NotNull(message = "O ID do evento é obrigatório para a inscrição.")
        UUID idEvento,
        
        // Opcional: Se ele for um colecionador e quiser vincular um carro ao evento
        UUID idCarro 
) {}