package carshub.com.br.backend.exceptions;

/**
 * Lancada quando o servico externo de geocodificacao esta indisponivel, sem cota,
 * devolveu erro HTTP ou nao respondeu dentro do timeout configurado.
 *
 * Mapeada para HTTP 503 (Service Unavailable): a falha e de um servico externo,
 * nao da aplicacao nem da requisicao do cliente.
 */
public class FalhaGeocodificacaoException extends RuntimeException {

    public FalhaGeocodificacaoException(String mensagem) {
        super(mensagem);
    }

    public FalhaGeocodificacaoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}