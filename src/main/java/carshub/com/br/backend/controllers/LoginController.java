package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.LoginDTO;
import carshub.com.br.backend.services.AutentificacaoLoginService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carshub/autentificacao")
public class AutentificacaoLoginController {

    private final AutentificacaoLoginService autentificacaoLoginService;

    public AutentificacaoLoginController(AutentificacaoLoginService autentificacaoLoginService) {
        this.autentificacaoLoginService = autentificacaoLoginService;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginDTO loginDTO) {
        String mensagem = autentificacaoLoginService.autenticar(loginDTO);
        return ResponseEntity.ok(mensagem);
    }
}