package carshub.com.br.backend.exceptions;

/**
 * SPRINT 6 (11.4): o @UsuarioAutenticado obrigatorio foi usado sem que haja
 * usuario autenticado no SecurityContext (defesa em profundidade — as rotas
 * protegidas ja barram anonimos na camada de URL). Traduzida para 401.
 */
public class NaoAutenticadoException extends RuntimeException {

    public NaoAutenticadoException(String mensagem) {
        super(mensagem);
    }
}
