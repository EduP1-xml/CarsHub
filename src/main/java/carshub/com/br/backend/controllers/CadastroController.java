package carshub.com.br.backend.controllers;
import carshub.com.br.backend.models.dtos.VerifyRegisterRequestDTO;
import carshub.com.br.backend.models.entities.Usuario;
import carshub.com.br.backend.services.CadastroService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/carshub/usuarios")
public class CadastroController {
    private final CadastroService cadastroService;

    public CadastroController(CadastroService cadastroService) {
        this.cadastroService = cadastroService;
    }

    @PostMapping("/cadastrar")
    public ResponseEntity<?> register(@RequestBody Usuario user) {
        try {
            cadastroService.registrarUsuario(user);
            return ResponseEntity.status(HttpStatus.CREATED).build();
        }
        catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
    @PostMapping("/verify-register")
    public ResponseEntity<?> verifyRegister(@RequestBody VerifyRegisterRequestDTO payload) {
        try {
            cadastroService.verifyRegister(payload.email(), payload.code());
            return ResponseEntity.ok().build();
        }
        catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

}
