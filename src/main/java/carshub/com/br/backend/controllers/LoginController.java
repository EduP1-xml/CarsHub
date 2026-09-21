package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.LatitudeLogintudeDTO;
import carshub.com.br.backend.models.dtos.LoginDTO;
import carshub.com.br.backend.models.dtos.UserPerfilDTO;
import carshub.com.br.backend.models.entities.Usuario;
import carshub.com.br.backend.services.LoginService;
import carshub.com.br.backend.services.file.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api/carshub/autentificacao")
public class LoginController {
    private final LoginService loginService;
    private final FileStorageService fileStorageService;


    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginDTO loginDTO) {
        String mensagem = loginService.autenticar(loginDTO);
        return ResponseEntity.ok(mensagem);
    }

    @PutMapping("/edit")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> update(@RequestBody @Valid UserPerfilDTO dados, @AuthenticationPrincipal Usuario authenticatedUser) {
        Usuario updatedUser = loginService.update(dados, authenticatedUser);
        if (updatedUser != null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.notFound().build();
    }

    @PostMapping("/urlFotoPerfil")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> uploadFoto(@RequestParam("file") MultipartFile file, @AuthenticationPrincipal Usuario authenticatedUser) {
        try {
            String fileDownloadUri = fileStorageService.uploadFotoPerfil(authenticatedUser.getId(), file);
            return ResponseEntity.ok(fileDownloadUri);
        }
        catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
        catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao salvar imagem: " + e.getMessage());
        }
    }

    @PutMapping("/update-loc")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> atualizarLoc(@RequestBody LatitudeLogintudeDTO dados, @AuthenticationPrincipal Usuario userAuthenticated) {
        Usuario updateLoc = loginService.updateLoc(dados, userAuthenticated);
        if (updateLoc != null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
}