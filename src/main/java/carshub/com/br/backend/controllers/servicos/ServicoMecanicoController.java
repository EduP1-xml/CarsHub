package carshub.com.br.backend.controllers.servicos;

import carshub.com.br.backend.models.dtos.PaginaRespostaDTO;
import carshub.com.br.backend.models.dtos.servicos.AvaliacaoServicoDTO;
import carshub.com.br.backend.models.dtos.servicos.ServicoMecanicoRegistroDTO;
import carshub.com.br.backend.models.dtos.servicos.ServicoRespostaDTO;
import carshub.com.br.backend.services.servicos.ServicoMecanicoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Solicitacoes de servico mecanico feitas pelo COLECIONADOR.
 * Fluxo de estados atendido por estas rotas:
 *   POST ""                     -> SOLICITADO
 *   POST /{id}/foto-inicial     -> evidencia do estado do veiculo (PENDENTE)
 *   PATCH /{id}/confirmar-chegada -> CHEGOU (precisa da confirmacao do mecanico)
 *   POST /{id}/foto-final       -> evidencia final do colecionador (CHEGOU)
 *   POST /{id}/avaliacao        -> avalia o mecanico (CHEGOU)
 *   PATCH /{id}/confirmar-conclusao -> FINALIZADO (precisa da confirmacao do mecanico)
 */
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
    public ResponseEntity<PaginaRespostaDTO<ServicoRespostaDTO>> listarMeusServicos(
            @RequestHeader("user-id") UUID id,
            @RequestParam(name = "somenteAbertos", defaultValue = "true") boolean somenteAbertos,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamanho", defaultValue = "5") int tamanho) {

        return ResponseEntity.ok(PaginaRespostaDTO.de(
                servicoMecanico.listarMeusServicos(id, somenteAbertos, pagina, tamanho)));
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

    @PostMapping("/{idServico}/foto-inicial")
    public ResponseEntity<ServicoRespostaDTO> enviarFotoInicial(
            @PathVariable UUID idServico,
            @RequestHeader("user-id") UUID id,
            @RequestParam("arquivo") MultipartFile arquivo) {

        return ResponseEntity.ok(servicoMecanico.enviarFotoInicial(idServico, id, arquivo));
    }

    @PatchMapping("/{idServico}/confirmar-chegada")
    public ResponseEntity<ServicoRespostaDTO> confirmarChegada(
            @PathVariable UUID idServico,
            @RequestHeader("user-id") UUID id) {

        return ResponseEntity.ok(servicoMecanico.confirmarChegadaPeloEntusiasta(idServico, id));
    }

    @PostMapping("/{idServico}/foto-final")
    public ResponseEntity<ServicoRespostaDTO> enviarFotoFinal(
            @PathVariable UUID idServico,
            @RequestHeader("user-id") UUID id,
            @RequestParam("arquivo") MultipartFile arquivo) {

        return ResponseEntity.ok(servicoMecanico.enviarFotoFinalPeloEntusiasta(idServico, id, arquivo));
    }

    @PostMapping("/{idServico}/avaliacao")
    public ResponseEntity<ServicoRespostaDTO> avaliarPrestador(
            @PathVariable UUID idServico,
            @RequestHeader("user-id") UUID id,
            @Valid @RequestBody AvaliacaoServicoDTO dados) {

        return ResponseEntity.ok(servicoMecanico.avaliarPrestador(idServico, id, dados));
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