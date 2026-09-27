package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.ForgotPasswordRequestDTO;
import carshub.com.br.backend.models.dtos.LoginDTO;
import carshub.com.br.backend.models.dtos.ResetPasswordRequestDTO;
import carshub.com.br.backend.services.LoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/carshub/autenticacao")
@RequiredArgsConstructor
public class LoginController {

    private final LoginService usuarioService;


    @PostMapping("/login")
    public ResponseEntity<String> realizarLogin(@Valid @RequestBody LoginDTO dadosLogin) {
        String mensagem = usuarioService.autenticar(dadosLogin);
        return ResponseEntity.ok(mensagem);
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequestDTO request) {
        if (request.email() == null || request.email().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("message", "Email é obrigatório."));
        }
        try {
            usuarioService.esqueciSenha(request.email());
            return ResponseEntity.ok().build();
        }
        catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequestDTO request) {
        try {
            usuarioService.resetarSenha(request.email(), request.codigo(), request.novaSenha());
            return ResponseEntity.ok().body(Map.of("message", "Senha alterada com sucesso."));
        }
        catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}