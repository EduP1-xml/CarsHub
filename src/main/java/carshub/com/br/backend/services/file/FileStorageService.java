package carshub.com.br.backend.services.file;

import carshub.com.br.backend.models.entities.Usuario;
import carshub.com.br.backend.repositories.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class FileStorageService {

    private Path arquivo;
    @Value("${app.upload.dir:uploads}")
    private String uploadDir;
    @Autowired
    private UsuarioRepository userRepository;

    @PostConstruct
    public void initFileLocation() {
        try {
            this.arquivo = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(this.arquivo);
        } catch (Exception ex) {
            throw new RuntimeException("Não foi possível criar o diretório de uploads.", ex);
        }
    }

    // --- MÉTODOS DE SEGURANÇA AUXILIARES ---

    private String obterNomeSeguro(MultipartFile file) {
        // Remove caracteres maliciosos (../) que causam Path Traversal
        String originalName = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
        if (originalName.contains("..")) {
            throw new RuntimeException("Tentativa bloqueada");
        }
        return originalName;
    }

    private void validarSegurancaGeral(String fileName) {
        // Bloqueia XSS e vírus. Permite apenas imagens e documentos seguros.
        String ext = fileName.toLowerCase();
        if (!ext.endsWith(".jpg") && !ext.endsWith(".jpeg") && !ext.endsWith(".png") &&
                !ext.endsWith(".pdf") && !ext.endsWith(".docx")) {
            throw new RuntimeException("Envie apenas Imagens ou Documentos (PDF/DOCX).");
        }
    }


    public String salvarArquivo(MultipartFile file) {
        try {
            String originalName = obterNomeSeguro(file);
            validarSegurancaGeral(originalName);

            String ext = originalName.toLowerCase();
            if (!ext.endsWith(".jpg") && !ext.endsWith(".jpeg") && !ext.endsWith(".png")) {
                throw new RuntimeException("Formato de arquivo inválido. Por favor, envie apenas imagens (JPG, JPEG ou PNG).");
            }

            String fileName = UUID.randomUUID().toString() + "_" + originalName;
            Path targetLocation = this.arquivo.resolve(fileName);

            // Salva apenas se passou nas validações
            Files.copy(file.getInputStream(), targetLocation);

            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/")
                    .path(fileName)
                    .toUriString();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar arquivo", e);
        }
    }

    public String salvarArquivoGeral(MultipartFile file) {
        try {
            String originalName = obterNomeSeguro(file);

            // Aqui a validação geral permite PDF e DOCX, mas barra .html (evitando XSS)
            validarSegurancaGeral(originalName);

            String fileName = UUID.randomUUID().toString() + "_" + originalName;
            Path targetLocation = this.arquivo.resolve(fileName);

            Files.copy(file.getInputStream(), targetLocation);

            return ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/")
                    .path(fileName)
                    .toUriString();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar arquivo", e);
        }
    }

    public String uploadFotoPerfil(UUID id, MultipartFile file) {
        Usuario user = userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        try {
            String originalName = obterNomeSeguro(file);
            validarSegurancaGeral(originalName);

            String ext = originalName.toLowerCase();
            if (!ext.endsWith(".jpg") && !ext.endsWith(".jpeg") && !ext.endsWith(".png")) {
                throw new RuntimeException("Formato de arquivo inválido. Por favor, envie apenas imagens (JPG, JPEG ou PNG).");
            }

            String fileName = UUID.randomUUID().toString() + "_" + originalName;
            Path targetLocation = this.arquivo.resolve(fileName);

            Files.copy(file.getInputStream(), targetLocation);

            String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                    .path("/uploads/")
                    .path(fileName)
                    .toUriString();

            user.setUrlFotoPerfil(fileDownloadUri);
            userRepository.save(user);

            return fileDownloadUri;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao salvar imagem: " + e.getMessage(), e);
        }
    }
}