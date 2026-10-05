package carshub.com.br.backend.models.dtos.geocodificacao;

import java.util.List;

/**
 * Espelho do JSON devolvido pelo Google Geocoding API.
 *
 * status: OK, ZERO_RESULTS, REQUEST_DENIED, OVER_QUERY_LIMIT, INVALID_REQUEST,
 *         UNKNOWN_ERROR (campo nulo e tratado como OK, para permitir outros
 *         provedores que nao devolvem esse campo).
 */
public record RespostaGeocodificacaoDTO(
        String status,
        List<ResultadoGeocodificacaoDTO> results
) {}