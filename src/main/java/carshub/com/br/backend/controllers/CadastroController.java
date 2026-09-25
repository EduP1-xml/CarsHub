package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.CadastroDTO;
import carshub.com.br.backend.models.dtos.VerificacaoRegistroRequestDTO;
import carshub.com.br.backend.services.CadastroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/carshub/usuarios")
public class CadastroController {

    private final CadastroService cadastroService;

    @PostMapping("/cadastrar")
    public ResponseEntity<String> registrar(@Valid @RequestBody CadastroDTO dto) {
        cadastroService.registrarUsuario(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body("Usuário registrado com sucesso. Verifique seu e-mail para ativar a conta.");
    }

    @PostMapping("/verificarRegistro")
    public ResponseEntity<String> verificarRegistro(@Valid @RequestBody VerificacaoRegistroRequestDTO dados) {
        cadastroService.verificarRegistro(dados.email(), dados.codigo());
        return ResponseEntity.ok("E-mail verificado com sucesso. Você já pode fazer login no CarsHub.");
    }
}