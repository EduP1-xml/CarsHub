package carshub.com.br.backend.services.file;

import carshub.com.br.backend.exceptions.FormatoImagemInvalidoException;
import carshub.com.br.backend.exceptions.NomeArquivoInvalidoException;
import carshub.com.br.backend.models.entities.Guincho;
import carshub.com.br.backend.models.entities.Mecanico;
import carshub.com.br.backend.models.entities.Usuario;
import carshub.com.br.backend.models.enums.tiposUsuarios.TipoUsuario;
import carshub.com.br.backend.repositories.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.PutBucketPolicyRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

/**
 * 1.5 UPLOADS:
 *  - limite global de 100MB (application.yaml);
 *  - formatos indevidos -> FormatoImagemInvalidoException (415);
 *  - nomes indevidos (vazio/../) -> NomeArquivoInvalidoException (400);
 *  - storage COMPATIVEL COM S3 (RustFS/MinIO/AWS) quando app.storage.s3.endpoint
 *    esta configurado; sem configuracao, cai no disco local (uploads/) como antes;
 *  - estrutura de pastas: uploads/{tipoUsuario}/{idUsuario}/{nomeFoto} para cada
 *    endpoint que registra foto (colecionador/mecanico/guincho/organizador/
 *    entusiasta = o DONO do arquivo).
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class FileStorageService {

    private static final String RAIZ_STORAGE = "uploads";

    private final UsuarioRepository repositorioUsuario;

    // Diretorio fisico onde os arquivos sao gravados (fallback em disco).
    private Path diretorioArmazenamento;

    @Value("${app.upload.dir:uploads}")
    private String diretorioUpload;

    // ---- Storage S3 (opcional; vazio = desligado -> disco local) ----

    @Value("${app.storage.s3.endpoint:}")
    private String s3Endpoint;

    @Value("${app.storage.s3.bucket:carshub-uploads}")
    private String s3Bucket;

    @Value("${app.storage.s3.regiao:us-east-1}")
    private String s3Regiao;

    @Value("${app.storage.s3.chaveAcesso:}")
    private String s3ChaveAcesso;

    @Value("${app.storage.s3.chaveSecreta:}")
    private String s3ChaveSecreta;

    @Value("${app.storage.s3.urlBase:}")
    private String s3UrlBase;

    private volatile S3Client clienteS3;

    @PostConstruct
    public void inicializarDiretorio() {
        try {
            this.diretorioArmazenamento = Paths.get(diretorioUpload).toAbsolutePath().normalize();
            Files.createDirectories(this.diretorioArmazenamento);
        } catch (Exception ex) {
            throw new RuntimeException("Não foi possível criar o diretório de uploads.", ex);
        }

        if (s3Ativo()) {
            inicializarStorageS3();
        }
    }

    // --- STORAGE S3 (compativel: MinIO/AWS) ---

    private boolean s3Ativo() {
        return s3Endpoint != null && !s3Endpoint.isBlank();
    }

    private S3Client clienteS3() {
        S3Client atual = clienteS3;
        if (atual != null) {
            return atual;
        }
        synchronized (this) {
            if (clienteS3 == null) {
                clienteS3 = S3Client.builder()
                        .region(Region.of(s3Regiao))
                        .endpointOverride(URI.create(s3Endpoint))
                        .credentialsProvider(StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(s3ChaveAcesso, s3ChaveSecreta)))
                        .forcePathStyle(true) // obrigatorio para servidores S3 self-hosted (RustFS/MinIO)
                        .build();
            }
            return clienteS3;
        }
    }

    /**
     * 1.5 STORAGE S3 — bootstrap do storage compativel (RustFS/MinIO/AWS):
     * cria o bucket quando ele ainda nao existe e aplica a bucket policy de
     * LEITURA PUBLICA no prefixo uploads/ (as fotos sao acessadas direto do
     * storage pelo navegador). O RustFS nao implementa ACLs S3 — o acesso
     * anonimo e feito por bucket policy, que e o equivalente do antigo
     * "mc anonymous set download" do MinIO.
     *
     * Nao derruba a aplicacao: se o storage ainda nao respondeu (container
     * subindo), tenta 3 vezes com espera e apenas loga o erro — o envio de
     * fotos volta a funcionar assim que o storage ficar de pe.
     */
    private void inicializarStorageS3() {
        for (int tentativa = 1; tentativa <= 3; tentativa++) {
            try {
                S3Client s3 = clienteS3();
                if (!bucketExiste(s3)) {
                    s3.createBucket(CreateBucketRequest.builder().bucket(s3Bucket).build());
                    log.info("Storage S3: bucket '{}' criado em {}.", s3Bucket, s3Endpoint);
                }
                s3.putBucketPolicy(PutBucketPolicyRequest.builder()
                        .bucket(s3Bucket)
                        .policy(politicaLeituraPublicaUploads())
                        .build());
                log.info("Storage S3 pronto em {} (bucket '{}', leitura publica em uploads/).",
                        s3Endpoint, s3Bucket);
                return;
            } catch (RuntimeException ex) {
                if (tentativa == 3) {
                    log.error("Storage S3 indisponivel no bootstrap ({}): {}. "
                            + "O envio de fotos falhara ate o storage ficar disponivel.",
                            s3Endpoint, ex.getMessage());
                } else {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException interrompida) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }

    private boolean bucketExiste(S3Client s3) {
        try {
            s3.headBucket(HeadBucketRequest.builder().bucket(s3Bucket).build());
            return true;
        } catch (S3Exception ex) {
            if (ex.statusCode() == 404) {
                return false;
            }
            throw ex;
        }
    }

    /** Leitura anonima (publica) apenas do prefixo uploads/ dentro do bucket. */
    private String politicaLeituraPublicaUploads() {
        return """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Effect": "Allow",
                      "Principal": "*",
                      "Action": ["s3:GetObject"],
                      "Resource": ["arn:aws:s3:::%s/uploads/*"]
                    }
                  ]
                }
                """.formatted(s3Bucket);
    }

    /** Grava o arquivo no storage ativo e devolve a URI publica de acesso. */
    private String gravarArquivo(String caminhoRelativo, MultipartFile arquivo) {
        if (s3Ativo()) {
            try (InputStream stream = arquivo.getInputStream()) {
                clienteS3().putObject(PutObjectRequest.builder()
                                .bucket(s3Bucket)
                                .key(RAIZ_STORAGE + "/" + caminhoRelativo)
                                .contentType(arquivo.getContentType())
                                .build(),
                        RequestBody.fromInputStream(stream, arquivo.getSize()));
            } catch (IOException e) {
                throw new RuntimeException("Erro ao salvar arquivo no storage.", e);
            }

            String base = (s3UrlBase != null && !s3UrlBase.isBlank())
                    ? s3UrlBase
                    : s3Endpoint + "/" + s3Bucket;

            return base.replaceAll("/+$", "") + "/" + RAIZ_STORAGE + "/" + caminhoRelativo;
        }

        try {
            Path destino = this.diretorioArmazenamento.resolve(caminhoRelativo);
            Files.createDirectories(destino.getParent());
            Files.copy(arquivo.getInputStream(), destino);
        } catch (IOException e) {
            throw new RuntimeException("Erro ao salvar arquivo.", e);
        }

        return montarUriDeDownload(caminhoRelativo);
    }

    // --- METODOS DE SEGURANCA AUXILIARES ---

    private String obterNomeSeguro(MultipartFile arquivo) {
        String nomeOriginal = StringUtils.cleanPath(
                arquivo.getOriginalFilename() != null ? arquivo.getOriginalFilename() : "");

        if (nomeOriginal.isBlank() || ".".equals(nomeOriginal)) {
            throw new NomeArquivoInvalidoException("O nome do arquivo não foi informado ou é inválido.");
        }
        if (nomeOriginal.contains("..")) {
            throw new NomeArquivoInvalidoException("Nome de arquivo inválido: caracteres de navegação de diretório não são permitidos.");
        }
        return nomeOriginal;
    }

    private void exigirFormatoDeImagem(String nomeArquivo) {
        String extensao = nomeArquivo.toLowerCase();
        if (!extensao.endsWith(".jpg") && !extensao.endsWith(".jpeg") && !extensao.endsWith(".png")) {
            throw new FormatoImagemInvalidoException(
                    "Formato de arquivo inválido. Por favor, envie apenas imagens (JPG, JPEG ou PNG).");
        }
    }

    private String gerarNomeUnico(String nomeOriginal) {
        return UUID.randomUUID() + "_" + nomeOriginal;
    }

    private String montarUriDeDownload(String caminhoRelativo) {
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/" + RAIZ_STORAGE + "/")
                .path(caminhoRelativo)
                .toUriString();
    }

    /** Pasta do DONO do arquivo, conforme o tipo de usuario (1.5: raiz/tipoUsuario/idUsuario/nomeFoto). */
    private String pastaDoUsuario(Usuario usuario) {
        if (usuario.getTipoUsuario() == TipoUsuario.COLECIONADOR) {
            return "colecionador";
        }
        if (usuario.getTipoUsuario() == TipoUsuario.ORGANIZADOR_EVENTO) {
            return "organizador";
        }
        if (usuario.getTipoUsuario() == TipoUsuario.PRESTADOR_SERVICO) {
            return usuario instanceof Mecanico ? "mecanico"
                    : usuario instanceof Guincho ? "guincho" : "prestador";
        }
        return "entusiasta";
    }

    // --- METODOS PUBLICOS ---

    /**
     * Salva uma imagem (JPG, JPEG ou PNG) na pasta
     * uploads/{tipoUsuario}/{idUsuario}/ e devolve a URI publica de acesso.
     */
    public String salvarArquivo(MultipartFile arquivo, String tipoUsuario, UUID idUsuario) {
        String nomeOriginal = obterNomeSeguro(arquivo);
        exigirFormatoDeImagem(nomeOriginal);

        String nomeArquivo = gerarNomeUnico(nomeOriginal);
        String caminhoRelativo = tipoUsuario + "/" + idUsuario + "/" + nomeArquivo;

        return gravarArquivo(caminhoRelativo, arquivo);
    }

    /**
     * Salva a foto de perfil do usuario, atualiza a coluna urlFotoPerfil e
     * devolve a URI publica de acesso. A pasta usa o tipo do DONO
     * (colecionador/mecanico/guincho/organizador/entusiasta).
     */
    public String salvarFotoPerfil(UUID id, MultipartFile arquivo) {
        Usuario usuario = repositorioUsuario.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado."));

        String nomeOriginal = obterNomeSeguro(arquivo);
        exigirFormatoDeImagem(nomeOriginal);

        String nomeArquivo = gerarNomeUnico(nomeOriginal);
        String caminhoRelativo = pastaDoUsuario(usuario) + "/" + id + "/" + nomeArquivo;

        String uriDownloadArquivo = gravarArquivo(caminhoRelativo, arquivo);

        usuario.setUrlFotoPerfil(uriDownloadArquivo);
        repositorioUsuario.save(usuario);

        return uriDownloadArquivo;
    }
}
