package carshub.com.br.backend.exceptions;

/**
 * O servico de geocodificacao respondeu OK, mas nenhum endereco corresponde ao
 * texto informado (ZERO_RESULTS). E erro do dado enviado pelo usuario, por isso
 * herda de RegraDeNegocioException: o GlobalException ja devolve HTTP 422.
 */
public class EnderecoNaoEncontradoException extends RegraDeNegocioException {

    public EnderecoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}