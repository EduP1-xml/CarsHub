package carshub.com.br.backend.models.dtos.geocodificacao;

import carshub.com.br.backend.models.dtos.servicos.EnderecoOcorrenciaDTO;

/**
 * Resultado unico usado pelo restante da aplicacao.
 *
 * INVARIANTE: enderecoEstruturado NUNCA e null. Quando nao ha endereco conhecido
 * (coordenada vinda do GPS), ele vem como EnderecoOcorrenciaDTO.vazio(); o campo
 * enderecoFormatado continua sendo a unica forma de saber se ha texto legivel.
 */
public record LocalizacaoGeocodificadaDTO(
        Double latitude,
        Double longitude,
        String enderecoFormatado,
        EnderecoOcorrenciaDTO enderecoEstruturado
) {

    /** Coordenada conhecida e endereco ainda desconhecido (GPS ou perfil). */
    public static LocalizacaoGeocodificadaDTO deCoordenadas(Double latitude, Double longitude) {
        return new LocalizacaoGeocodificadaDTO(latitude, longitude, null, EnderecoOcorrenciaDTO.vazio());
    }
}