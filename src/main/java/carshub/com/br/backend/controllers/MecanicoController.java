package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.perfis.patches.PerfilMecanicoPatchDTO;
import carshub.com.br.backend.models.dtos.perfis.puts.PerfilMecanicoPutDTO;
import carshub.com.br.backend.services.MecanicoService;
import carshub.com.br.backend.services.file.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/carshub/mecanicos")
@RequiredArgsConstructor
public class MecanicoController {

    private final MecanicoService servicoMecanico;
    private final FileStorageService servicoArmazenamentoArquivo;

    @PutMapping("/perfil")
    public ResponseEntity<String> substituirPerfilCompleto(
            @RequestHeader("user-id") UUID id,
            @Valid @RequestBody PerfilMecanicoPutDTO dados) {

        servicoMecanico.substituirPerfil(id, dados);
        return ResponseEntity.ok("Perfil do mecânico substituído com sucesso.");
    }

    @PatchMapping("/perfil")
    public ResponseEntity<String> atualizarPerfilParcial(
            @RequestHeader("user-id") UUID id,
            @RequestBody PerfilMecanicoPatchDTO dados) {

        servicoMecanico.atualizarPerfilCondicional(id, dados);
        return ResponseEntity.ok("Perfil do mecânico modificado parcialmente com sucesso.");
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