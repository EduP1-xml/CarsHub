package carshub.com.br.backend.controllers.servicos;

import carshub.com.br.backend.models.dtos.servicos.ServicoMecanicoRegistroDTO;
import carshub.com.br.backend.models.dtos.servicos.ServicoRespostaDTO;
import carshub.com.br.backend.services.servicos.ServicoMecanicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Solicitacoes de servico mecanico feitas pelo colecionador. */
@RestController
@RequestMapping("/api/carshub/colecionador/servicos/mecanicos")
@RequiredArgsConstructor
public class ServicoMecanicoController {

    private final ServicoMecanicoService servicoMecanico;

    @PostMapping
    public ResponseEntity<ServicoRespostaDTO> solicitarServico(
            @RequestHeader("user-id") UUID id,
            @Valid @RequestBody ServicoMecanicoRegistroDTO dados) {

        return ResponseEntity.status(HttpStatus.CREATED).body(servicoMecanico.solicitarServico(id, dados));
    }

    @GetMapping
    public ResponseEntity<List<ServicoRespostaDTO>> listarMeusServicos(
            @RequestHeader("user-id") UUID id,
            @RequestParam(name = "somenteAbertos", defaultValue = "true") boolean somenteAbertos) {

        return ResponseEntity.ok(servicoMecanico.listarMeusServicos(id, somenteAbertos));
    }

    @GetMapping("/{idServico}")
    public ResponseEntity<ServicoRespostaDTO> buscarServico(
            @PathVariable UUID idServico,
            @RequestHeader("user-id") UUID id) {

        return ResponseEntity.ok(servicoMecanico.buscarServico(idServico, id));
    }

    @PatchMapping("/{idServico}/aceitar-proposta")
    public ResponseEntity<ServicoRespostaDTO> aceitarProposta(
            @PathVariable UUID idServico,
            @RequestHeader("user-id") UUID id) {

        return ResponseEntity.ok(servicoMecanico.aceitarProposta(idServico, id));
    }

    @PatchMapping("/{idServico}/rejeitar-proposta")
    public ResponseEntity<ServicoRespostaDTO> rejeitarProposta(
            @PathVariable UUID idServico,
            @RequestHeader("user-id") UUID id) {

        return ResponseEntity.ok(servicoMecanico.rejeitarProposta(idServico, id));
    }

    @PatchMapping("/{idServico}/confirmar-conclusao")
    public ResponseEntity<ServicoRespostaDTO> confirmarConclusao(
            @PathVariable UUID idServico,
            @RequestHeader("user-id") UUID id) {

        return ResponseEntity.ok(servicoMecanico.confirmarConclusaoPeloEntusiasta(idServico, id));
    }

    @DeleteMapping("/{idServico}")
    public ResponseEntity<Void> cancelarSolicitacao(
            @PathVariable UUID idServico,
            @RequestHeader("user-id") UUID id) {

        servicoMecanico.cancelarSolicitacao(idServico, id);
        return ResponseEntity.noContent().build();
    }
}