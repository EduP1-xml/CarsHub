package carshub.com.br.backend.models.dtos.eventos;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record EventoPutDTO(
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
        @Size(max = 5, message = "O número não pode exceder 5 caracteres")
        String numero,

        @Size(max = 50, message = "O complemento não pode exceder 50 caracteres")
        String complemento,

        @NotBlank(message = "O bairro é obrigatório")
        @Size(max = 50, message = "O bairro não pode exceder 50 caracteres")
        String bairro,

        // F29: cidade, UF e CEP compoem o endereco usado na geocodificacao.
        @NotBlank(message = "A cidade é obrigatória")
        @Size(max = 50, message = "A cidade não pode exceder 50 caracteres")
        String cidade,

        @NotBlank(message = "A UF é obrigatória")
        @Size(min = 2, max = 2, message = "A UF deve conter exatamente 2 caracteres")
        String uf,

        @NotBlank(message = "O CEP é obrigatório")
        @Pattern(regexp = "\\d{8}", message = "O CEP deve conter exatamente 8 dígitos")
        String cep,

        @NotNull(message = "A data de início é obrigatória")
        LocalDateTime dataHoraInicio,

        @NotNull(message = "A data de término é obrigatória")
        LocalDateTime dataHoraFim,

        List<String> urlFotoEvento,

        // Carros aceitos no evento (opcional). Ausente/vazio = aberto a todos.
        @jakarta.validation.Valid
        carshub.com.br.backend.models.dtos.clubes.FiltroCarroDTO filtroCarro
) {}
