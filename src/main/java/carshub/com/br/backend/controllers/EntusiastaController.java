package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.LatitudeLogintudeDTO;
import carshub.com.br.backend.models.dtos.perfis.patches.PerfilEntusiastaPatchDTO;
import carshub.com.br.backend.models.dtos.perfis.puts.PerfilEntusiastaPutDTO;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.Usuario;
import carshub.com.br.backend.services.EntusiastaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/carshub/entusiastas")
public class EntusiastaController {

    private final EntusiastaService entusiastaService;

    public EntusiastaController(EntusiastaService entusiastaService) {
        this.entusiastaService = entusiastaService;
    }

    // PUT exige envio de todos os dados do formulário (com validação @Valid)
    @PutMapping("/perfil")
    public ResponseEntity<String> substituirPerfilCompleto(
            @RequestHeader("user-id") UUID id,
            @Valid @RequestBody PerfilEntusiastaPutDTO dto) {

        entusiastaService.substituirPerfil(id, dto);
        return ResponseEntity.ok("Perfil do entusiasta substituído com sucesso.");
    }

    // PATCH permite envio de apenas um campo flexível
    @PatchMapping("/perfil")
    public ResponseEntity<String> atualizarPerfilParcial(
            @RequestHeader("user-id") UUID id,
            @RequestBody PerfilEntusiastaPatchDTO dto) {

        entusiastaService.atualizarPerfilCondicional(id, dto);
        return ResponseEntity.ok("Perfil do entusiasta modificado parcialmente com sucesso.");
    }

    @PutMapping("/update-loc")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> atualizarLoc(@RequestBody LatitudeLogintudeDTO dados, @AuthenticationPrincipal Entusiasta userAuthenticated) {
        Entusiasta updateLoc = entusiastaService.updateLoc(dados, userAuthenticated);
        if (updateLoc != null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
}