package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.LatitudeLongitudeDTO;
import carshub.com.br.backend.models.dtos.perfis.patches.PerfilEntusiastaPatchDTO;
import carshub.com.br.backend.models.dtos.perfis.puts.PerfilEntusiastaPutDTO;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.services.EntusiastaService;
import carshub.com.br.backend.services.file.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/carshub/entusiastas")
@RequiredArgsConstructor
public class EntusiastaController {

    private final EntusiastaService servicoEntusiasta;
    private final FileStorageService servicoArmazenamentoArquivo;

    @PutMapping("/perfil")
    public ResponseEntity<String> substituirPerfilCompleto(
            @RequestHeader("user-id") UUID id,
            @Valid @RequestBody PerfilEntusiastaPutDTO dados) {

        servicoEntusiasta.substituirPerfil(id, dados);
        return ResponseEntity.ok("Perfil do entusiasta substituído com sucesso.");
    }

    @PatchMapping("/perfil")
    public ResponseEntity<String> atualizarPerfilParcial(
            @RequestHeader("user-id") UUID id,
            @RequestBody PerfilEntusiastaPatchDTO dados) {

        servicoEntusiasta.atualizarPerfilCondicional(id, dados);
        return ResponseEntity.ok("Perfil do entusiasta modificado parcialmente com sucesso.");
    }

    @PutMapping("/localizacao")
    public ResponseEntity<Void> atualizarLocalizacao(
            @RequestHeader("user-id") UUID id,
            @RequestBody LatitudeLongitudeDTO dados) {

        Entusiasta entusiastaAtualizado = servicoEntusiasta.atualizarLocalizacao(dados, id);
        if (entusiastaAtualizado != null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }

    @PostMapping("/foto-perfil")
    public ResponseEntity<String> enviarFotoPerfil(
            @RequestHeader("user-id") UUID id,
            @RequestParam("arquivo") MultipartFile arquivo) {
        try {
            // Nota: usando o nome atual do método no FileStorageService
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