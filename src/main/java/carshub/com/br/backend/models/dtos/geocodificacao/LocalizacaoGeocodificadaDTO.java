package carshub.com.br.backend.models.dtos.geocodificacao;

/**
 * Resultado unico usado pelo restante da aplicacao.
 *
 * enderecoFormatado pode ser nulo quando a coordenada veio do GPS: nesse caso o
 * chamador decide se tenta a geocodificacao reversa (best-effort).
 */
public record LocalizacaoGeocodificadaDTO(
        Double latitude,
        Double longitude,
        String enderecoFormatado
) {}