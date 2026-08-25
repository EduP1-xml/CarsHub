package carshub.com.br.backend.exceptions;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ExcessoesGlobais {

    // Captura os erros disparados pelo @Valid nos Controllers
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> validacoesExcessao(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();

        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        return ResponseEntity.badRequest().body(errors);
    }

    // Captura as exceções de regra de negócio que lançamos no AuthService
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> excessoesTempoExecucao(RuntimeException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}