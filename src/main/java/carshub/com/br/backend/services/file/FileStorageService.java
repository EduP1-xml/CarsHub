package carshub.com.br.backend.services.file;

import carshub.com.br.backend.models.entities.Usuario;
import carshub.com.br.backend.repositories.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
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
@RequiredArgsConstructor
public class FileStorageService {

    private final UsuarioRepository repositorioUsuario;

    // Diretorio fisico onde os arquivos sao gravados.
    private Path diretorioArmazenamento;

    @Value("${app.upload.dir:uploads}")
    private String diretorioUpload;

    @PostConstruct
    public void inicializarDiretorio() {
        try {
            this.diretorioArmazenamento = Paths.get(diretorioUpload).toAbsolutePath().normalize();
            Files.createDirectories(this.diretorioArmazenamento);
        } catch (Exception ex) {
            throw new RuntimeException("Não foi possível criar o diretório de uploads.", ex);
        }
    }

    // --- METODOS DE SEGURANCA AUXILIARES ---

    private String obterNomeSeguro(MultipartFile arquivo) {
        // Remove caracteres maliciosos (../) que causam Path Traversal
        String nomeOriginal = StringUtils.cleanPath(
                arquivo.getOriginalFilename() != null ? arquivo.getOriginalFilename() : "");
        if (nomeOriginal.contains("..")) {
            throw new RuntimeException("Tentativa bloqueada");
        }
        return nomeOriginal;
    }

    private void validarSegurancaGeral(String nomeArquivo) {
        // Bloqueia XSS e virus. Permite apenas imagens e documentos seguros.
        String extensao = nomeArquivo.toLowerCase();
        if (!extensao.endsWith(".jpg") && !extensao.endsWith(".jpeg") && !extensao.endsWith(".png") &&
                !extensao.endsWith(".pdf") && !extensao.endsWith(".docx")) {
            throw new RuntimeException("Envie apenas Imagens ou Documentos (PDF/DOCX).");
        }
    }

    private void exigirFormatoDeImagem(String nomeArquivo) {
        String extensao = nomeArquivo.toLowerCase();
        if (!extensao.endsWith(".jpg") && !extensao.endsWith(".jpeg") && !extensao.endsWith(".png")) {
            throw new RuntimeException("Formato de arquivo inválido. Por favor, envie apenas imagens (JPG, JPEG ou PNG).");
        }
    }

    private String gerarNomeUnico(String nomeOriginal) {
        return UUID.randomUUID().toString() + "_" + nomeOriginal;
    }

    private String montarUriDeDownload(String nomeArquivo) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/uploads/")
                .path(nomeArquivo)
                .toUriString();
    }

    // --- METODOS PUBLICOS ---

    /**
     * Salva uma imagem (JPG, JPEG ou PNG) e devolve a URI publica de acesso.
     */
    public String salvarArquivo(MultipartFile arquivo) {
        try {
            String nomeOriginal = obterNomeSeguro(arquivo);
            validarSegurancaGeral(nomeOriginal);
            exigirFormatoDeImagem(nomeOriginal);

            String nomeArquivo = gerarNomeUnico(nomeOriginal);
            Path destino = this.diretorioArmazenamento.resolve(nomeArquivo);

            Files.copy(arquivo.getInputStream(), destino);

            return montarUriDeDownload(nomeArquivo);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar arquivo", e);
        }
    }

    /**
     * Salva imagens ou documentos (PDF/DOCX) e devolve a URI publica de acesso.
     * A validacao geral barra .html, evitando XSS.
     */
    public String salvarArquivoGeral(MultipartFile arquivo) {
        try {
            String nomeOriginal = obterNomeSeguro(arquivo);
            validarSegurancaGeral(nomeOriginal);

            String nomeArquivo = gerarNomeUnico(nomeOriginal);
            Path destino = this.diretorioArmazenamento.resolve(nomeArquivo);

            Files.copy(arquivo.getInputStream(), destino);

            return montarUriDeDownload(nomeArquivo);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar arquivo", e);
        }
    }

    /**
     * Salva a foto de perfil do usuario, atualiza a coluna urlFotoPerfil e
     * devolve a URI publica de acesso.
     */
    public String salvarFotoPerfil(UUID id, MultipartFile arquivo) {
        Usuario usuario = repositorioUsuario.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));
        try {
            String nomeOriginal = obterNomeSeguro(arquivo);
            validarSegurancaGeral(nomeOriginal);
            exigirFormatoDeImagem(nomeOriginal);

            String nomeArquivo = gerarNomeUnico(nomeOriginal);
            Path destino = this.diretorioArmazenamento.resolve(nomeArquivo);

            Files.copy(arquivo.getInputStream(), destino);

            String uriDownloadArquivo = montarUriDeDownload(nomeArquivo);

            usuario.setUrlFotoPerfil(uriDownloadArquivo);
            repositorioUsuario.save(usuario);

            return uriDownloadArquivo;
        } catch (Exception e) {
            throw new RuntimeException("Erro ao salvar imagem: " + e.getMessage(), e);
        }
    }
}