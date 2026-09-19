package carshub.com.br.backend.exceptions;

public class PerfilIncompativelException extends RuntimeException {
    public PerfilIncompativelException(String message) {
        super(message);
    }
}
