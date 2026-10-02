package carshub.com.br.backend.exceptions;

public class AutenticacaoEnvioEmailException extends FalhaEnvioEmailException {

    public AutenticacaoEnvioEmailException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}