package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.LoginDTO;
import carshub.com.br.backend.services.LoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/carshub/autentificacao")
public class LoginController {

    private final LoginService loginService;

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginDTO loginDTO) {
        String mensagem = loginService.autenticar(loginDTO);
        return ResponseEntity.ok(mensagem);
    }
}