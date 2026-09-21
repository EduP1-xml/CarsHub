package carshub.com.br.backend.models.dtos.eventos;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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

        @NotNull(message = "A data de início é obrigatória")
        @Future(message = "A data de início deve estar no futuro")
        LocalDateTime dataHoraInicio,

        @NotNull(message = "A data de término é obrigatória")
        @Future(message = "A data de fim deve estar no futuro")
        LocalDateTime dataHoraFim
) {}