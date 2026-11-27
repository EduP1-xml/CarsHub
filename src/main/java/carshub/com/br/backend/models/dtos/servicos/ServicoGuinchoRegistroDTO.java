package carshub.com.br.backend.models.dtos.servicos;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Mesma prioridade de localizacao do servico mecanico:
 * latitude/longitude (GPS) > endereco digitado > localizacao do perfil.
 */
public record ServicoGuinchoRegistroDTO(
        @NotNull(message = "O ID do carro é obrigatório para solicitar o guincho.")
        UUID idCarro,

        @DecimalMin(value = "-90.0", message = "A latitude deve estar entre -90 e 90.")
        @DecimalMax(value = "90.0", message = "A latitude deve estar entre -90 e 90.")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "A longitude deve estar entre -180 e 180.")
        @DecimalMax(value = "180.0", message = "A longitude deve estar entre -180 e 180.")
        Double longitude,

        String endereco
) {}
