package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.RequisicaoLoginDTO;
import carshub.com.br.backend.services.AutentificacaoLoginService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AutentificacaoLoginController {

    private final AutentificacaoLoginService authService;

    public AutentificacaoLoginController(AutentificacaoLoginService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody RequisicaoLoginDTO dto) {
        String mensagem = authService.autenticar(dto);
        return ResponseEntity.ok(mensagem);
    }
}