package carshub.com.br.backend.exceptions;

public class TempoLimiteEnvioEmailException extends FalhaEnvioEmailException {

    public TempoLimiteEnvioEmailException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}