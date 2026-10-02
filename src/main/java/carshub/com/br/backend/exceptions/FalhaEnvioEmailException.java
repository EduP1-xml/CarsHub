package carshub.com.br.backend.exceptions;

/**
 * Lancada quando o servidor de e-mail esta indisponivel, recusou a autenticacao
 * ou nao respondeu dentro do timeout configurado.
 *
 * Mapeada para HTTP 503 (Service Unavailable): a falha e de um servico externo,
 * nao da aplicacao nem da requisicao do cliente.
 */
public class FalhaEnvioEmailException extends RuntimeException {

    public FalhaEnvioEmailException(String mensagem) {
        super(mensagem);
    }

    public FalhaEnvioEmailException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}