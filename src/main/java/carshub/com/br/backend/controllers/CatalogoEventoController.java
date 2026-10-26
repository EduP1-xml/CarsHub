package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.PaginaRespostaDTO;
import carshub.com.br.backend.models.dtos.eventos.CatalogoEventoResponseDTO;
import carshub.com.br.backend.services.CatalogoEventoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/carshub/eventos")
public class CatalogoEventoController {

    private final CatalogoEventoService catalogoEventoService;

    // Endpoint livre para popular o feed principal do aplicativo paginado, default 5
    @GetMapping
    public ResponseEntity<PaginaRespostaDTO<CatalogoEventoResponseDTO>> listarEventosAtivos(
            @RequestHeader(value = "user-id", required = false) UUID idEntusiasta,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamanho", defaultValue = "5") int tamanho) {
        return ResponseEntity.ok(PaginaRespostaDTO.de(
                catalogoEventoService.listarEventosAtivos(idEntusiasta, pagina, tamanho)));
    }

    @GetMapping("/{idEvento}")
    public ResponseEntity<CatalogoEventoResponseDTO> detalharEvento(
            @PathVariable UUID idEvento,
            @RequestHeader(value = "user-id", required = false) UUID idEntusiasta) {
        return ResponseEntity.ok(catalogoEventoService.buscarDetalhesEvento(idEvento, idEntusiasta));
    }
}