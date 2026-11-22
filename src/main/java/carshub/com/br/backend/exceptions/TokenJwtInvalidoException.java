package carshub.com.br.backend.exceptions;

/**
 * SPRINT 6 (11.1): token JWT ausente, malformado, adulterado, expirado ou do
 * tipo errado (ex.: refresh usado como access). Traduzida para 401 no
 * GlobalException — na cadeia de filtros o FiltroJwtAutenticacao escreve o 401
 * direto na resposta (denials de filtro nao passam pelo advice).
 */
public class TokenJwtInvalidoException extends RuntimeException {

    public TokenJwtInvalidoException(String mensagem) {
        super(mensagem);
    }
}
