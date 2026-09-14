package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.updates.EntusiastaUpdateDTO;
import carshub.com.br.backend.services.EntusiastaService;
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

    @PutMapping("/perfil")
    public ResponseEntity<String> atualizarPerfil(
            @RequestHeader("user-id") UUID id,
            @RequestBody EntusiastaUpdateDTO dto) {
        entusiastaService.atualizarPerfil(id, dto);
        return ResponseEntity.ok("Perfil do entusiasta atualizado com sucesso.");
    }
}