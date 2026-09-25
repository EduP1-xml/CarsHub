package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.perfis.patches.PerfilGuinchoPatchDTO;
import carshub.com.br.backend.models.dtos.perfis.puts.PerfilGuinchoPutDTO;
import carshub.com.br.backend.services.GuinchoService;
import carshub.com.br.backend.services.file.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/carshub/guinchos")
@RequiredArgsConstructor
public class GuinchoController {

    private final GuinchoService servicoGuincho;
    private final FileStorageService servicoArmazenamentoArquivo;

    @PutMapping("/perfil")
    public ResponseEntity<String> substituirPerfilCompleto(
            @RequestHeader("user-id") UUID id,
            @Valid @RequestBody PerfilGuinchoPutDTO dados) {

        servicoGuincho.substituirPerfil(id, dados);
        return ResponseEntity.ok("Perfil do guincho substituído com sucesso.");
    }

    @PatchMapping("/perfil")
    public ResponseEntity<String> atualizarPerfilParcial(
            @RequestHeader("user-id") UUID id,
            @RequestBody PerfilGuinchoPatchDTO dados) {

        servicoGuincho.atualizarPerfilCondicional(id, dados);
        return ResponseEntity.ok("Perfil do guincho modificado parcialmente com sucesso.");
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