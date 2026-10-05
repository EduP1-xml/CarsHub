package carshub.com.br.backend.models.dtos.servicos;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PropostaServicoDTO(
        @NotNull(message = "O valor da proposta é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor da proposta deve ser maior que zero.")
        @Digits(integer = 8, fraction = 2, message = "O valor da proposta deve ter no máximo 8 dígitos inteiros e 2 decimais.")
        BigDecimal valorProposto,

        Double latitude,
        Double longitude
) {}