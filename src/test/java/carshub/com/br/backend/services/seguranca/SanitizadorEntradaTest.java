package carshub.com.br.backend.services.seguranca;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SPRINT 6 (11.6): sanitizacao de texto livre contra scripts (XSS armazenado).
 */
class SanitizadorEntradaTest {

    private final SanitizadorEntrada sanitizador = new SanitizadorEntrada();

    @Test
    @DisplayName("script e removido e o texto restante e preservado")
    void scriptRemovido() {
        assertThat(sanitizador.sanitizar("<script>alert('x')</script>Fulano"))
                .isEqualTo("Fulano");
    }

    @Test
    @DisplayName("tags com handlers de evento sao removidas")
    void imgComOnerrorRemovida() {
        assertThat(sanitizador.sanitizar("<img src=x onerror=alert(1)>Texto"))
                .isEqualTo("Texto");
    }

    @Test
    @DisplayName("texto comum sem HTML passa intacto")
    void textoComumIntacto() {
        assertThat(sanitizador.sanitizar("Troca de óleo bem feita, recomendo!"))
                .isEqualTo("Troca de óleo bem feita, recomendo!");
    }

    @Test
    @DisplayName("caracteres residuais sao escapados (tradeoff documentado do Safelist.none)")
    void caracteresResiduaisEscapados() {
        assertThat(sanitizador.sanitizar("5 < 10"))
                .isEqualTo("5 &lt; 10");
    }

    @Test
    @DisplayName("nulo e vazio passam intactos")
    void nuloEVazioIntactos() {
        assertThat(sanitizador.sanitizar(null)).isNull();
        assertThat(sanitizador.sanitizar("")).isEmpty();
        assertThat(sanitizador.sanitizar("   ")).isEqualTo("   ");
    }
}
