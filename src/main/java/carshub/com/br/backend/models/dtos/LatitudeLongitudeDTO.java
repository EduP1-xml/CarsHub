package carshub.com.br.backend.models.dtos;


import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

/**
 * Padrao de coordenadas (WGS84): latitude de -90 a 90 e longitude de -180 a 180.
 * O backend normaliza para 7 casas decimais antes de gravar.
 */
public record LatitudeLongitudeDTO(
        @DecimalMin(value = "-90.0", message = "A latitude deve estar entre -90 e 90.")
        @DecimalMax(value = "90.0", message = "A latitude deve estar entre -90 e 90.")
        Double latitude,

        @DecimalMin(value = "-180.0", message = "A longitude deve estar entre -180 e 180.")
        @DecimalMax(value = "180.0", message = "A longitude deve estar entre -180 e 180.")
        Double longitude) {

}
