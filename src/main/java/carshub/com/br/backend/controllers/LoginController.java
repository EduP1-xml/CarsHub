package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.LoginDTO;
import carshub.com.br.backend.models.dtos.RedefinicaoSenhaDTO;
import carshub.com.br.backend.models.dtos.SolicitacaoCodigoSenhaDTO;
import carshub.com.br.backend.services.LoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carshub/autenticacao")
@RequiredArgsConstructor
public class LoginController {

    private final LoginService servicoLogin;

    @PostMapping("/login")
    public ResponseEntity<String> realizarLogin(@Valid @RequestBody LoginDTO dadosLogin) {
        String mensagem = servicoLogin.autenticar(dadosLogin);
        return ResponseEntity.ok(mensagem);
    }

    @PostMapping("/esqueci-senha")
    public ResponseEntity<String> solicitarCodigoRedefinicaoSenha(
            @Valid @RequestBody SolicitacaoCodigoSenhaDTO dados) {
        servicoLogin.solicitarCodigoRedefinicaoSenha(dados.email());
        return ResponseEntity.ok("Se o e-mail estiver cadastrado, enviaremos um código de verificação.");
    }

    @PatchMapping("/senha/redefinir")
    public ResponseEntity<String> redefinirSenha(@Valid @RequestBody RedefinicaoSenhaDTO dados) {
        servicoLogin.redefinirSenha(dados);
        return ResponseEntity.ok("Senha redefinida com sucesso.");
    }
}