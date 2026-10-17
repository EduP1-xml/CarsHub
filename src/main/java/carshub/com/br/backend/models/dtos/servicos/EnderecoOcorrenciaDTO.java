package carshub.com.br.backend.models.dtos.servicos;

/**
 * Espelho de leitura/escrita do embeddable EnderecoOcorrencia (decisao A6).
 *
 * Existe o estado "vazio" (todos os campos nulos) para representar endereco
 * DESCONHECIDO sem usar null: quando a coordenada veio do GPS e a geocodificacao
 * reversa falhou, a ocorrencia continua valida — so nao temos o endereco legivel.
 */
public record EnderecoOcorrenciaDTO(
        String rua,
        String numero,
        String bairro,
        String cidade,
        String estado,
        String cep,
        String placeId
) {

    public static EnderecoOcorrenciaDTO vazio() {
        return new EnderecoOcorrenciaDTO(null, null, null, null, null, null, null);
    }
}