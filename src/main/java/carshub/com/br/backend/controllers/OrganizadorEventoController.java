package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.perfis.patches.PerfilOrganizadorEventoPatchDTO;
import carshub.com.br.backend.models.dtos.perfis.puts.PerfilOrganizadorEventoPutDTO;
import carshub.com.br.backend.services.OrganizadorEventoService;
import carshub.com.br.backend.services.file.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/carshub/organizadores")
@RequiredArgsConstructor
public class OrganizadorEventoController {

    private final OrganizadorEventoService servicoOrganizador;
    private final FileStorageService servicoArmazenamentoArquivo;

    @PutMapping("/perfil")
    public ResponseEntity<String> substituirPerfilCompleto(
            @RequestHeader("user-id") UUID id,
            @Valid @RequestBody PerfilOrganizadorEventoPutDTO dados) {

        servicoOrganizador.substituirPerfil(id, dados);
        return ResponseEntity.ok("Perfil do organizador substituído com sucesso.");
    }

    @PatchMapping("/perfil")
    public ResponseEntity<String> atualizarPerfilParcial(
            @RequestHeader("user-id") UUID id,
            @RequestBody PerfilOrganizadorEventoPatchDTO dados) {

        servicoOrganizador.atualizarPerfilCondicional(id, dados);
        return ResponseEntity.ok("Perfil do organizador modificado parcialmente com sucesso.");
    }

    @PostMapping("/foto-perfil")
    public ResponseEntity<String> enviarFotoPerfil(
            @RequestHeader("user-id") UUID id,
            @RequestParam("arquivo") MultipartFile arquivo) {
        try {
            String uriDownloadArquivo = servicoArmazenamentoArquivo.uploadFotoPerfil(id, arquivo);
            return ResponseEntity.ok(uriDownloadArquivo);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao salvar imagem: " + e.getMessage());
        }
    }
}