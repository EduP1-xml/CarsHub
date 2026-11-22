package carshub.com.br.backend.exceptions;

/**
 * 1.5 UPLOADS: extensao/formato de imagem nao permitido nos endpoints de
 * foto (JPG, JPEG e PNG sao os aceitos). Status HTTP: 415 Unsupported Media Type.
 */
public class FormatoImagemInvalidoException extends RuntimeException {

    public FormatoImagemInvalidoException(String mensagem) {
        super(mensagem);
    }
}
