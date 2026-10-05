package carshub.com.br.backend.controllers.servicos;

import carshub.com.br.backend.models.dtos.servicos.ServicoGuinchoRegistroDTO;
import carshub.com.br.backend.models.dtos.servicos.ServicoRespostaDTO;
import carshub.com.br.backend.services.servicos.ServicoGuinchoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Solicitacoes de guincho feitas pelo colecionador. */
@RestController
@RequestMapping("/api/carshub/colecionador/servicos/guinchos")
@RequiredArgsConstructor
public class ServicoGuinchoController {

    private final ServicoGuinchoService servicoGuincho;

    @PostMapping
    public ResponseEntity<ServicoRespostaDTO> solicitarServico(
            @RequestHeader("user-id") UUID id,
            @Valid @RequestBody ServicoGuinchoRegistroDTO dados) {

        return ResponseEntity.status(HttpStatus.CREATED).body(servicoGuincho.solicitarServico(id, dados));
    }

    @GetMapping
    public ResponseEntity<List<ServicoRespostaDTO>> listarMeusServicos(
            @RequestHeader("user-id") UUID id,
            @RequestParam(name = "somenteAbertos", defaultValue = "true") boolean somenteAbertos) {

        return ResponseEntity.ok(servicoGuincho.listarMeusServicos(id, somenteAbertos));
    }

    @GetMapping("/{idServico}")
    public ResponseEntity<ServicoRespostaDTO> buscarServico(
            @PathVariable UUID idServico,
            @RequestHeader("user-id") UUID id) {

        return ResponseEntity.ok(servicoGuincho.buscarServico(idServico, id));
    }

    @PatchMapping("/{idServico}/aceitar-proposta")
    public ResponseEntity<ServicoRespostaDTO> aceitarProposta(
            @PathVariable UUID idServico,
            @RequestHeader("user-id") UUID id) {

        return ResponseEntity.ok(servicoGuincho.aceitarProposta(idServico, id));
    }

    @PatchMapping("/{idServico}/rejeitar-proposta")
    public ResponseEntity<ServicoRespostaDTO> rejeitarProposta(
            @PathVariable UUID idServico,
            @RequestHeader("user-id") UUID id) {

        return ResponseEntity.ok(servicoGuincho.rejeitarProposta(idServico, id));
    }

    @PatchMapping("/{idServico}/confirmar-conclusao")
    public ResponseEntity<ServicoRespostaDTO> confirmarConclusao(
            @PathVariable UUID idServico,
            @RequestHeader("user-id") UUID id) {

        return ResponseEntity.ok(servicoGuincho.confirmarConclusaoPeloEntusiasta(idServico, id));
    }

    @DeleteMapping("/{idServico}")
    public ResponseEntity<Void> cancelarSolicitacao(
            @PathVariable UUID idServico,
            @RequestHeader("user-id") UUID id) {

        servicoGuincho.cancelarSolicitacao(idServico, id);
        return ResponseEntity.noContent().build();
    }
}