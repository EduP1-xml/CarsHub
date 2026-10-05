package carshub.com.br.backend.models.dtos.geocodificacao;

/** Bloco "location" do Google: lat/lng em graus decimais. */
public record CoordenadaGeocodificacaoDTO(
        Double lat,
        Double lng
) {}