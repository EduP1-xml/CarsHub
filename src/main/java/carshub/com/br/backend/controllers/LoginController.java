package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.LoginDTO;
import carshub.com.br.backend.services.LoginService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carshub/autenticacao")
public class LoginController {

    private final LoginService servicoLogin;

    public LoginController(LoginService servicoLogin) {
        this.servicoLogin = servicoLogin;
    }

    @PostMapping("/login")
    public ResponseEntity<String> realizarLogin(@Valid @RequestBody LoginDTO dadosLogin) {
        String mensagem = servicoLogin.autenticar(dadosLogin);
        return ResponseEntity.ok(mensagem);
    }
}