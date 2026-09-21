package carshub.com.br.backend.models.dtos;


import carshub.com.br.backend.models.entities.Usuario;

public record LatitudeLogintudeDTO(Double latitude, Double longitude) {
    public LatitudeLogintudeDTO(Usuario user) {
        this(user.getLatitude(), user.getLongitude());
    }
}
