package carshub.com.br.backend.models.dtos.eventos;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * F31: o PATCH ganhou Bean Validation com as MESMAS regras da entidade Evento
 * (todos os campos sao opcionais, mas, quando informados, sao validados).
 */
public record EventoPatchDTO(
        @Size(max = 50, message = "O nome não pode exceder 50 caracteres")
        String nome,

        @Size(max = 1000, message = "A descrição não pode exceder 1000 caracteres")
        String descricao,

        @DecimalMin(value = "0.00", message = "O preço não pode ser negativo")
        BigDecimal preco,

        @Min(value = 1, message = "O evento deve permitir pelo menos 1 participante")
        Integer limiteParticipantes,

        @Size(max = 100, message = "A rua não pode exceder 100 caracteres")
        String rua,

        @Size(max = 5, message = "O número não pode exceder 5 caracteres")
        String numero,

        @Size(max = 50, message = "O complemento não pode exceder 50 caracteres")
        String complemento,

        @Size(max = 50, message = "O bairro não pode exceder 50 caracteres")
        String bairro,

        @Size(max = 50, message = "A cidade não pode exceder 50 caracteres")
        String cidade,

        @Size(min = 2, max = 2, message = "A UF deve conter exatamente 2 caracteres")
        String uf,

        @Pattern(regexp = "\\d{8}", message = "O CEP deve conter exatamente 8 dígitos")
        String cep,

        @Future(message = "A data de início deve estar no futuro")
        LocalDateTime dataHoraInicio,

        @Future(message = "A data de fim deve estar no futuro")
        LocalDateTime dataHoraFim,

        List<String> urlFotoEvento
) {}
