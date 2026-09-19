package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.CadastroDTO;
import carshub.com.br.backend.models.entities.Usuario;
import carshub.com.br.backend.services.CadastroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/carshub/usuarios")
public class CadastroController {
    private final CadastroService cadastroService;

    public CadastroController(CadastroService cadastroService) {
        this.cadastroService = cadastroService;
    }

    @PostMapping("/cadastrar")
    public ResponseEntity<String> registrar(@Valid @RequestBody CadastroDTO cadastroDTO) {
        Usuario salvo = cadastroService.registrarUsuario(cadastroDTO);
        return ResponseEntity.status(
                HttpStatus.CREATED)
                .body("Usuário " + salvo.getNome() + " registrado com sucesso!");
    }
}
