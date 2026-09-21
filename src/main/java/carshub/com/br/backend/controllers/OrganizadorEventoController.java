package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.perfis.patches.PerfilOrganizadorEventoPatchDTO;
import carshub.com.br.backend.models.dtos.perfis.puts.PerfilOrganizadorEventoPutDTO;
import carshub.com.br.backend.services.OrganizadorEventoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/carshub/organizadores")
public class OrganizadorEventoController {

    private final OrganizadorEventoService organizadorService;

    public OrganizadorEventoController(OrganizadorEventoService organizadorService) {
        this.organizadorService = organizadorService;
    }

    @PutMapping("/perfil")
    public ResponseEntity<String> substituirPerfilCompleto(
            @RequestHeader("user-id") UUID id,
            @Valid @RequestBody PerfilOrganizadorEventoPutDTO dto) {

        organizadorService.substituirPerfil(id, dto);
        return ResponseEntity.ok("Perfil do organizador substituído com sucesso.");
    }

    @PatchMapping("/perfil")
    public ResponseEntity<String> atualizarPerfilParcial(
            @RequestHeader("user-id") UUID id,
            @RequestBody PerfilOrganizadorEventoPatchDTO dto) {

        organizadorService.atualizarPerfilCondicional(id, dto);
        return ResponseEntity.ok("Perfil do organizador modificado parcialmente com sucesso.");
    }
}