package carshub.com.br.backend.controllers;

import carshub.com.br.backend.config.UsuarioAutenticado;

import org.springframework.security.access.prepost.PreAuthorize;

import carshub.com.br.backend.models.dtos.PaginaRespostaDTO;
import carshub.com.br.backend.models.dtos.inscricoes.InscricaoRegistroDTO;
import carshub.com.br.backend.models.dtos.inscricoes.InscricaoResponseDTO;
import carshub.com.br.backend.services.InscricaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/carshub/entusiastas/inscricoes")
@PreAuthorize("hasAuthority('ENTUSIASTA')")
public class InscricaoController {

    private final InscricaoService inscricaoService;

    public InscricaoController(InscricaoService inscricaoService) {
        this.inscricaoService = inscricaoService;
    }

    // INVENTARIO DE ENDPOINTS (item 2): o idEvento vem na rota, permitindo que o
    // front selecione o evento exato em GET /eventos/{idEvento} antes de inscrever.
    @PostMapping("/{idEvento}")
    public ResponseEntity<InscricaoResponseDTO> inscrever(
            @UsuarioAutenticado UUID idEntusiasta,
            @PathVariable UUID idEvento,
            @Valid @RequestBody InscricaoRegistroDTO dto) {
        InscricaoResponseDTO inscricao = inscricaoService.realizarInscricao(idEntusiasta, idEvento, dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(inscricao);
    }

    @GetMapping
    public ResponseEntity<PaginaRespostaDTO<InscricaoResponseDTO>> listarMinhasInscricoes(
            @UsuarioAutenticado UUID idEntusiasta,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamanho", defaultValue = "5") int tamanho) {
        return ResponseEntity.ok(PaginaRespostaDTO.de(
                inscricaoService.listarMinhasInscricoes(idEntusiasta, pagina, tamanho)));
    }

    @PatchMapping("/{idInscricao}/cancelar")
    public ResponseEntity<String> cancelarInscricao(
            @PathVariable UUID idInscricao,
            @UsuarioAutenticado UUID idEntusiasta) {
        inscricaoService.cancelarInscricao(idInscricao, idEntusiasta);
        return ResponseEntity.ok("Inscrição cancelada com sucesso. A vaga foi devolvida ao evento.");
    }
}