package carshub.com.br.backend.exceptions;

/**
 * A chave da API de geocodificacao foi recusada (REQUEST_DENIED / HTTP 401-403)
 * ou nem foi configurada. Mapeada para HTTP 502 (Bad Gateway), igual ao padrao
 * ja usado para autenticacao recusada no envio de e-mail.
 */
public class ChaveGeocodificacaoException extends FalhaGeocodificacaoException {

    public ChaveGeocodificacaoException(String mensagem) {
        super(mensagem);
    }

    public ChaveGeocodificacaoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}