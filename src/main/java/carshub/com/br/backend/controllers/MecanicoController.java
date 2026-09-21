package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.perfis.patches.PerfilMecanicoPatchDTO;
import carshub.com.br.backend.models.dtos.perfis.puts.PerfilMecanicoPutDTO;
import carshub.com.br.backend.services.MecanicoService;
import jakarta.validation.Valid;
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
    public ResponseEntity<String> substituirPerfilCompleto(
            @RequestHeader("user-id") UUID id,
            @Valid @RequestBody PerfilMecanicoPutDTO dto) {

        mecanicoService.substituirPerfil(id, dto);
        return ResponseEntity.ok("Perfil do mecânico substituído com sucesso.");
    }

    @PatchMapping("/perfil")
    public ResponseEntity<String> atualizarPerfilParcial(
            @RequestHeader("user-id") UUID id,
            @RequestBody PerfilMecanicoPatchDTO dto) {

        mecanicoService.atualizarPerfilCondicional(id, dto);
        return ResponseEntity.ok("Perfil do mecânico modificado parcialmente com sucesso.");
    }
}