package carshub.com.br.backend.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@Slf4j
public class ExcecoesGlobais {

    // 1. Captura os erros disparados pelo @Valid nos Controllers (Status 400 - Bad Request)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validacoesException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(errors);
    }

    // Captura usuário não encontrado (Status 404 - Not Found)
    @ExceptionHandler(UsuarioNaoEncontradoException.class)
    public ResponseEntity<String> usuarioNaoEncontrado(UsuarioNaoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
    }

    // Captura senha incorreta (Status 401 - Unauthorized)
    @ExceptionHandler(SenhaIncorretaException.class)
    public ResponseEntity<String> senhaIncorreta(SenhaIncorretaException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
    }

    // Captura conflito de dados (Status 409 - Conflict)
    @ExceptionHandler(EmailJaCadastradoException.class)
    public ResponseEntity<String> emailJaCadastrado(EmailJaCadastradoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    // Captura conflito de dados para CPF OU CNPJ (Status 409 - Conflict)
    @ExceptionHandler(DocumentoJaCadastradoException.class)
    public ResponseEntity<String> documentoJaCadastrado(DocumentoJaCadastradoException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    // Captura erros de preenchimento no cadastro (Status 400 - Bad Request)
    @ExceptionHandler(CampoObrigatorioCadastroException.class)
    public ResponseEntity<String> documentoObrigatorioCadastro(CampoObrigatorioCadastroException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(PlacaJaCadastradaException.class)
    public ResponseEntity<String> placaJaCadastrada(PlacaJaCadastradaException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ex.getMessage());
    }

    //FILTRO DE SEGURANÇA: Captura qualquer outro erro inesperado (Status 500 - Internal Server Error) e salva os logs de forma estruturada
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> errosInesperados(Exception ex) {
        log.error("Erro inesperado no servidor: ", ex);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Ocorreu um erro interno inesperado no servidor. Por favor, tente novamente mais tarde.");
    }
}