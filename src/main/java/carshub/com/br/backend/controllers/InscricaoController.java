package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.inscricoes.InscricaoRegistroDTO;
import carshub.com.br.backend.models.dtos.inscricoes.InscricaoResponseDTO;
import carshub.com.br.backend.services.InscricaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/carshub/entusiastas/inscricoes")
public class InscricaoController {

    private final InscricaoService inscricaoService;

    public InscricaoController(InscricaoService inscricaoService) {
        this.inscricaoService = inscricaoService;
    }

    @PostMapping
    public ResponseEntity<InscricaoResponseDTO> inscrever(
            @RequestHeader("user-id") UUID idEntusiasta,
            @Valid @RequestBody InscricaoRegistroDTO dto) {
        InscricaoResponseDTO inscricao = inscricaoService.realizarInscricao(idEntusiasta, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(inscricao);
    }

    @GetMapping
    public ResponseEntity<List<InscricaoResponseDTO>> listarMinhasInscricoes(
            @RequestHeader("user-id") UUID idEntusiasta) {
        return ResponseEntity.ok(inscricaoService.listarMinhasInscricoes(idEntusiasta));
    }

    @PatchMapping("/{idInscricao}/cancelar")
    public ResponseEntity<String> cancelarInscricao(
            @PathVariable UUID idInscricao,
            @RequestHeader("user-id") UUID idEntusiasta) {
        inscricaoService.cancelarInscricao(idInscricao, idEntusiasta);
        return ResponseEntity.ok("Inscrição cancelada com sucesso. A vaga foi devolvida ao evento.");
    }
}