package carshub.com.br.backend.models.dtos.geocodificacao;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Um resultado de geocodificacao. Usamos sempre o primeiro (o mais relevante). */
public record ResultadoGeocodificacaoDTO(
        @JsonProperty("formatted_address") String formattedAddress,
        GeometriaGeocodificacaoDTO geometry
) {}