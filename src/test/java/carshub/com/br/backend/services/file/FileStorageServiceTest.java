package carshub.com.br.backend.services.file;

import carshub.com.br.backend.exceptions.FormatoImagemInvalidoException;
import carshub.com.br.backend.exceptions.NomeArquivoInvalidoException;
import carshub.com.br.backend.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 1.5 UPLOADS: validacoes de nome e formato de arquivo (S3 desligado nos
 * cenarios de erro — a validacao acontece antes de gravar em qualquer storage).
 */
@ExtendWith(MockitoExtension.class)
class FileStorageServiceTest {

    @Mock
    private UsuarioRepository repositorioUsuario;

    private FileStorageService servico;

    @BeforeEach
    void preparar() {
        servico = new FileStorageService(repositorioUsuario);
        ReflectionTestUtils.setField(servico, "diretorioUpload", "uploads");
        ReflectionTestUtils.setField(servico, "s3Endpoint", "");
    }

    @Test
    @DisplayName("nome com '..' (path traversal) gera NomeArquivoInvalidoException")
    void nomeComPathTraversal() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "../etc/passwd.png", "image/png", new byte[]{1});

        assertThatThrownBy(() -> servico.salvarArquivo(arquivo, "colecionador", UUID.randomUUID()))
                .isInstanceOf(NomeArquivoInvalidoException.class);
    }

    @Test
    @DisplayName("arquivo sem nome gera NomeArquivoInvalidoException")
    void arquivoSemNome() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "   ", "image/png", new byte[]{1});

        assertThatThrownBy(() -> servico.salvarArquivo(arquivo, "colecionador", UUID.randomUUID()))
                .isInstanceOf(NomeArquivoInvalidoException.class);
    }

    @Test
    @DisplayName("extensao fora de JPG/JPEG/PNG gera FormatoImagemInvalidoException")
    void extensaoNaoPermitida() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "script.html", "text/html", new byte[]{1});

        assertThatThrownBy(() -> servico.salvarArquivo(arquivo, "colecionador", UUID.randomUUID()))
                .isInstanceOf(FormatoImagemInvalidoException.class);
    }
}
