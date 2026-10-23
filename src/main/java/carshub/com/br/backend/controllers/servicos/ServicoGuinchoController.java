package carshub.com.br.backend.controllers.servicos;

import carshub.com.br.backend.models.dtos.PaginaRespostaDTO;
import carshub.com.br.backend.models.dtos.servicos.AvaliacaoServicoDTO;
import carshub.com.br.backend.models.dtos.servicos.ServicoGuinchoRegistroDTO;
import carshub.com.br.backend.models.dtos.servicos.ServicoRespostaDTO;
import carshub.com.br.backend.services.servicos.ServicoGuinchoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/** Solicitacoes de guincho feitas pelo COLECIONADOR (mesmo fluxo do servico mecanico). */
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
    public ResponseEntity<PaginaRespostaDTO<ServicoRespostaDTO>> listarMeusServicos(
            @RequestHeader("user-id") UUID id,
            @RequestParam(name = "somenteAbertos", defaultValue = "true") boolean somenteAbertos,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamanho", defaultValue = "5") int tamanho) {

        List<ServicoRespostaDTO> servicos = servicoGuincho.listarMeusServicos(id, somenteAbertos);
        return ResponseEntity.ok(PaginaRespostaDTO.de(servicos, pagina, tamanho));
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

    @PostMapping("/{idServico}/foto-inicial")
    public ResponseEntity<ServicoRespostaDTO> enviarFotoInicial(
            @PathVariable UUID idServico,
            @RequestHeader("user-id") UUID id,
            @RequestParam("arquivo") MultipartFile arquivo) {

        return ResponseEntity.ok(servicoGuincho.enviarFotoInicial(idServico, id, arquivo));
    }

    @PatchMapping("/{idServico}/confirmar-chegada")
    public ResponseEntity<ServicoRespostaDTO> confirmarChegada(
            @PathVariable UUID idServico,
            @RequestHeader("user-id") UUID id) {

        return ResponseEntity.ok(servicoGuincho.confirmarChegadaPeloEntusiasta(idServico, id));
    }

    @PostMapping("/{idServico}/foto-final")
    public ResponseEntity<ServicoRespostaDTO> enviarFotoFinal(
            @PathVariable UUID idServico,
            @RequestHeader("user-id") UUID id,
            @RequestParam("arquivo") MultipartFile arquivo) {

        return ResponseEntity.ok(servicoGuincho.enviarFotoFinalPeloEntusiasta(idServico, id, arquivo));
    }

    @PostMapping("/{idServico}/avaliacao")
    public ResponseEntity<ServicoRespostaDTO> avaliarPrestador(
            @PathVariable UUID idServico,
            @RequestHeader("user-id") UUID id,
            @Valid @RequestBody AvaliacaoServicoDTO dados) {

        return ResponseEntity.ok(servicoGuincho.avaliarPrestador(idServico, id, dados));
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