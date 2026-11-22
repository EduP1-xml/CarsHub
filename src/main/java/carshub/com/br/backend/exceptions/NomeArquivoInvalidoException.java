package carshub.com.br.backend.exceptions;

/**
 * 1.5 UPLOADS: nome de arquivo indevido (vazio, com ".."/path traversal ou
 * caracteres perigosos). Status HTTP: 400 Bad Request.
 */
public class NomeArquivoInvalidoException extends RuntimeException {

    public NomeArquivoInvalidoException(String mensagem) {
        super(mensagem);
    }
}
