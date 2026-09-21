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
<<<<<<< HEAD
=======

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }
>>>>>>> 3c58334c6ea0f92faa5df94b19ded8b805b7f54e

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginDTO loginDTO) {
        String mensagem = loginService.autenticar(loginDTO);
        return ResponseEntity.ok(mensagem);
    }
}