package carshub.com.br.backend.controllers;

import org.springframework.security.access.prepost.PreAuthorize;

import carshub.com.br.backend.models.dtos.LoginDTO;
import carshub.com.br.backend.models.dtos.LoginResponseDTO;
import carshub.com.br.backend.models.dtos.RedefinicaoSenhaDTO;
import carshub.com.br.backend.models.dtos.RenovacaoTokenDTO;
import carshub.com.br.backend.models.dtos.SolicitacaoCodigoSenhaDTO;
import carshub.com.br.backend.services.LoginService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carshub/autenticacao")
@RequiredArgsConstructor
@PreAuthorize("permitAll()")
public class LoginController {

    private final LoginService servicoLogin;

    // SPRINT 6 (11.1): login devolve o par de tokens JWT (antes: mensagem em texto).
    @PostMapping("/login")
    public ResponseEntity<LoginResponseDTO> realizarLogin(@Valid @RequestBody LoginDTO dadosLogin) {
        return ResponseEntity.ok(servicoLogin.autenticar(dadosLogin));
    }

    // SPRINT 6 (11.1): renovacao do par com o refresh token (rotacao a cada uso).
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponseDTO> renovarTokens(@Valid @RequestBody RenovacaoTokenDTO dados) {
        return ResponseEntity.ok(servicoLogin.renovarTokens(dados.refreshToken()));
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
