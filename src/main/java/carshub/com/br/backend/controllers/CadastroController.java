package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.CadastroDTO;
import carshub.com.br.backend.models.dtos.CadastroIndividualDTO;
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

@RestController
@RequestMapping("/api/carshub/usuarios")
@RequiredArgsConstructor
public class CadastroController {

    private final CadastroService servicoCadastro;

    /**
     * Primeira etapa: cria a conta com os dados de Usuario.
     * 201 Created - um novo Usuario foi criado.
     */
    @PostMapping("/cadastrar")
    public ResponseEntity<String> registrar(@Valid @RequestBody CadastroDTO dados) {
        servicoCadastro.registrarUsuario(dados);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Usuário registrado com sucesso. Continue o cadastro em /cadastrar/individual e verifique seu e-mail para ativar a conta.");
    }

    /**
     * Segunda etapa: preenche os dados especificos da classe concreta.
     * 200 OK - nenhum recurso novo e criado, a conta existente e completada.
     */
    @PostMapping("/cadastrar/individual")
    public ResponseEntity<String> registrarDadosIndividuais(@Valid @RequestBody CadastroIndividualDTO dados) {
        servicoCadastro.registrarDadosIndividuais(dados);
        return ResponseEntity.ok("Dados individuais registrados com sucesso. Verifique seu e-mail para ativar a conta.");
    }

    /**
     * Terceira etapa: confirma o codigo recebido por e-mail e ativa a conta.
     * 200 OK - a conta existente foi atualizada, nao criada.
     */
    @PostMapping("/verificarRegistro")
    public ResponseEntity<String> verificarRegistro(@Valid @RequestBody VerificacaoRegistroRequestDTO dados) {
        servicoCadastro.verificarRegistro(dados.email(), dados.codigo());
        return ResponseEntity.ok("E-mail verificado com sucesso. Você já pode fazer login no CarsHub.");
    }
}