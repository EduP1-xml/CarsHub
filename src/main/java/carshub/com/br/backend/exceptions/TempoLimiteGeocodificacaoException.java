package carshub.com.br.backend.exceptions;

/** O servico de geocodificacao foi alcancado, mas nao respondeu no prazo. HTTP 504. */
public class TempoLimiteGeocodificacaoException extends FalhaGeocodificacaoException {

    public TempoLimiteGeocodificacaoException(String mensagem) {
        super(mensagem);
    }

    public TempoLimiteGeocodificacaoException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}