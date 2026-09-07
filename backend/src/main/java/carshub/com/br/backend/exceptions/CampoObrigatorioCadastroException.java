package carshub.com.br.backend.exceptions;

public class CampoObrigatorioCadastroException extends RuntimeException {
    public CampoObrigatorioCadastroException(String message) {
        super(message);
    }
}
