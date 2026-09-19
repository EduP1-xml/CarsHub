package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.updates.MecanicoUpdateDTO;
import carshub.com.br.backend.services.MecanicoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/carshub/mecanicos")
public class MecanicoController {

    private final MecanicoService mecanicoService;

    public MecanicoController(MecanicoService mecanicoService) {
        this.mecanicoService = mecanicoService;
    }

    @PutMapping("/perfil")
    public ResponseEntity<String> atualizarPerfil(
            @RequestHeader("user-id") UUID id,
            @RequestBody MecanicoUpdateDTO dto) {
        mecanicoService.atualizarPerfil(id, dto);
        return ResponseEntity.ok("Perfil do mecânico atualizado com sucesso.");
    }
}