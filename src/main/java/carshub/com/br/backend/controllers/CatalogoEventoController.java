package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.eventos.CatalogoEventoResponseDTO;
import carshub.com.br.backend.services.CatalogoEventoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/carshub/eventos")
public class CatalogoEventoController {

    private final CatalogoEventoService catalogoEventoService;

    // Endpoint livre para popular o feed principal do aplicativo
    @GetMapping
    public ResponseEntity<List<CatalogoEventoResponseDTO>> listarEventosAtivos(
            @RequestHeader(value = "user-id", required = false) UUID idEntusiasta) {
        return ResponseEntity.ok(catalogoEventoService.listarEventosAtivos(idEntusiasta));
    }

    @GetMapping("/{idEvento}")
    public ResponseEntity<CatalogoEventoResponseDTO> detalharEvento(
            @PathVariable UUID idEvento,
            @RequestHeader(value = "user-id", required = false) UUID idEntusiasta) {
        return ResponseEntity.ok(catalogoEventoService.buscarDetalhesEvento(idEvento, idEntusiasta));
    }
}