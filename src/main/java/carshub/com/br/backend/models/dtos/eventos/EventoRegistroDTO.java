package carshub.com.br.backend.models.dtos.eventos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record EventoRegistroDTO(
        @NotBlank(message = "O nome do evento é obrigatório")
        @Size(max = 50, message = "O nome não pode exceder 50 caracteres")
        String nome,

        @NotBlank(message = "A descrição do evento é obrigatória")
        @Size(max = 1000, message = "A descrição não pode exceder 1000 caracteres")
        String descricao,

        @NotNull(message = "O preço do ingresso é obrigatório")
        @DecimalMin(value = "0.00", message = "O preço não pode ser negativo")
        BigDecimal preco,

        @NotNull(message = "O limite de participantes é obrigatório")
        @Min(value = 1, message = "O evento deve permitir pelo menos 1 participante")
        Integer limiteParticipantes,

        @NotBlank(message = "A rua é obrigatória")
        @Size(max = 100, message = "A rua não pode exceder 100 caracteres")
        String rua,

        @NotBlank(message = "O número é obrigatório")
        @Size(max = 10, message = "O número não pode exceder 10 caracteres")
        String numero,

        @Size(max = 50, message = "O complemento não pode exceder 50 caracteres")
        String complemento, // Opcional, sem @NotBlank

        @NotBlank(message = "O bairro é obrigatório")
        @Size(max = 50, message = "O bairro não pode exceder 50 caracteres")
        String bairro,

        String urlFotoEvento
) {}