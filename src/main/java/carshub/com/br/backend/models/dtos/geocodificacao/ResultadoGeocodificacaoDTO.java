package carshub.com.br.backend.models.dtos.geocodificacao;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Um resultado de geocodificacao. Usamos sempre o primeiro (o mais relevante). */
public record ResultadoGeocodificacaoDTO(
        @JsonProperty("formatted_address") String formattedAddress,
        @JsonProperty("place_id") String placeId,
        @JsonProperty("address_components") List<ComponenteEnderecoDTO> addressComponents,
        GeometriaGeocodificacaoDTO geometry
) {}