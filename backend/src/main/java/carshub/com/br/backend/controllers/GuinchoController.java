package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.updates.GuinchoUpdateDTO;
import carshub.com.br.backend.services.GuinchoService;
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
    public ResponseEntity<String> atualizarPerfil(
            @RequestHeader("user-id") UUID id,
            @RequestBody GuinchoUpdateDTO dto) {
        guinchoService.atualizarPerfil(id, dto);
        return ResponseEntity.ok("Perfil do guincho atualizado com sucesso.");
    }
}