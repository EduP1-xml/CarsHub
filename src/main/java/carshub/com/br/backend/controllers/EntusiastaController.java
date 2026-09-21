package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.perfis.patches.PerfilEntusiastaPatchDTO;
import carshub.com.br.backend.models.dtos.perfis.puts.PerfilEntusiastaPutDTO;
import carshub.com.br.backend.models.dtos.updates.EntusiastaPutDTO;
import carshub.com.br.backend.models.dtos.updates.EntusiastaUpdateDTO;
import carshub.com.br.backend.services.EntusiastaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
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
}