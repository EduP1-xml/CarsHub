package carshub.com.br.backend.models.dtos.servicos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PropostaServicoDTO(
        @NotNull(message = "O valor da proposta é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor da proposta deve ser maior que zero.")
        @Digits(integer = 8, fraction = 2, message = "O valor da proposta deve ter no máximo 8 dígitos inteiros e 2 decimais.")
        BigDecimal valorProposto,

        /* Opcionais; quando informadas, tambem ATUALIZAM a localizacao do perfil do prestador. */
        @DecimalMin(value = "-90.0", message = "A latitude deve estar entre -90 e 90.")
        @DecimalMax(value = "90.0", message = "A latitude deve estar entre -90 e 90.")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "A longitude deve estar entre -180 e 180.")
        @DecimalMax(value = "180.0", message = "A longitude deve estar entre -180 e 180.")
        Double longitude
) {}