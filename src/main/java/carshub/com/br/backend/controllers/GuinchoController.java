package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.perfis.patches.PerfilGuinchoPatchDTO;
import carshub.com.br.backend.models.dtos.perfis.puts.PerfilGuinchoPutDTO;
import carshub.com.br.backend.services.GuinchoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/carshub/guinchos")
public class GuinchoController {

    private final GuinchoService guinchoService;

    public GuinchoController(GuinchoService guinchoService) {
        this.guinchoService = guinchoService;
    }

    @PutMapping("/perfil")
    public ResponseEntity<String> substituirPerfilCompleto(
            @RequestHeader("user-id") UUID id,
            @Valid @RequestBody PerfilGuinchoPutDTO dto) {

        guinchoService.substituirPerfil(id, dto);
        return ResponseEntity.ok("Perfil do guincho substituído com sucesso.");
    }

    @PatchMapping("/perfil")
    public ResponseEntity<String> atualizarPerfilParcial(
            @RequestHeader("user-id") UUID id,
            @RequestBody PerfilGuinchoPatchDTO dto) {

        guinchoService.atualizarPerfilCondicional(id, dto);
        return ResponseEntity.ok("Perfil do guincho modificado parcialmente com sucesso.");
    }
}