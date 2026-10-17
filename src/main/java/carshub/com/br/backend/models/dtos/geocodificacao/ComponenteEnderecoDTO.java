package carshub.com.br.backend.models.dtos.geocodificacao;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/** Item de "address_components" do Google*/
public record ComponenteEnderecoDTO(
        @JsonProperty("long_name") String longName,
        @JsonProperty("short_name") String shortName,
        List<String> types
) {}