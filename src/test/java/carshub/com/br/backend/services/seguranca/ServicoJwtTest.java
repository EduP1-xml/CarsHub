package carshub.com.br.backend.services.seguranca;

import carshub.com.br.backend.exceptions.TokenJwtInvalidoException;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.Mecanico;
import carshub.com.br.backend.models.enums.tiposUsuarios.TipoPrestadorServico;
import carshub.com.br.backend.models.enums.tiposUsuarios.TipoUsuario;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados.TROCA_DE_OLEO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SPRINT 6 (11.1): emissao e validacao do JWT (access + refresh).
 * Unitario: nao precisa de banco nem de contexto Spring.
 */
class ServicoJwtTest {

    private static final String SEGREDO = "segredo-de-teste-com-mais-de-32-caracteres-ok";

    private final ServicoJwt servicoJwt = new ServicoJwt(SEGREDO, 15, 7, "carshub-api");

    private Entusiasta criarEntusiasta(TipoUsuario tipo) {
        Entusiasta entusiasta = new Entusiasta();
        entusiasta.setId(UUID.randomUUID());
        entusiasta.setLogin("entusiasta@teste.com");
        entusiasta.setSenha("Senha@123");
        entusiasta.setNome("Entusiasta Teste");
        entusiasta.setTipoUsuario(tipo);
        entusiasta.setAtivo(true);
        return entusiasta;
    }

    private Mecanico criarMecanico() {
        Mecanico mecanico = new Mecanico();
        mecanico.setId(UUID.randomUUID());
        mecanico.setLogin("mecanico@teste.com");
        mecanico.setSenha("Senha@123");
        mecanico.setNome("Mecanico Teste");
        mecanico.setTipoUsuario(TipoUsuario.PRESTADOR_SERVICO);
        mecanico.setTipoPrestadorServico(TipoPrestadorServico.MECANICO);
        mecanico.setServicos(List.of(TROCA_DE_OLEO));
        mecanico.setAtivo(true);
        return mecanico;
    }

    @Test
    @DisplayName("access token do colecionador carrega ENTUSIASTA (e nunca COLECIONADOR)")
    void tokenDeColecionadorUsaAuthorityEntusiasta() {
        Entusiasta colecionador = criarEntusiasta(TipoUsuario.COLECIONADOR);

        ServicoJwt.TokenAutenticado dados =
                servicoJwt.validar(servicoJwt.gerarTokenAcesso(colecionador), ServicoJwt.TIPO_ACESSO);

        assertThat(dados.roles()).containsExactly("ENTUSIASTA");
        assertThat(dados.roles()).doesNotContain("COLECIONADOR");
    }

    @Test
    @DisplayName("access token do mecanico carrega PRESTADOR_SERVICO + MECANICO")
    void tokenDeMecanicoCarregaAuthoritiesDoPrestador() {
        Mecanico mecanico = criarMecanico();

        ServicoJwt.TokenAutenticado dados =
                servicoJwt.validar(servicoJwt.gerarTokenAcesso(mecanico), ServicoJwt.TIPO_ACESSO);

        assertThat(dados.roles()).containsExactlyInAnyOrder("PRESTADOR_SERVICO", "MECANICO");
    }

    @Test
    @DisplayName("validacao devolve o subject correto e o tipo do token")
    void validacaoDevolveSubjectETipo() {
        Entusiasta entusiasta = criarEntusiasta(TipoUsuario.ENTUSIASTA);

        ServicoJwt.TokenAutenticado dados =
                servicoJwt.validar(servicoJwt.gerarTokenAcesso(entusiasta), ServicoJwt.TIPO_ACESSO);

        assertThat(dados.idUsuario()).isEqualTo(entusiasta.getId());
        assertThat(dados.tipo()).isEqualTo(ServicoJwt.TIPO_ACESSO);
    }

    @Test
    @DisplayName("token adulterado e rejeitado com TokenJwtInvalidoException")
    void tokenAdulteradoRejeitado() {
        Entusiasta entusiasta = criarEntusiasta(TipoUsuario.ENTUSIASTA);
        String token = servicoJwt.gerarTokenAcesso(entusiasta) + "adulterado";

        assertThatThrownBy(() -> servicoJwt.validar(token, ServicoJwt.TIPO_ACESSO))
                .isInstanceOf(TokenJwtInvalidoException.class);
    }

    @Test
    @DisplayName("refresh token usado como access e rejeitado (tipos distintos)")
    void refreshNaoAbreRotas() {
        Entusiasta entusiasta = criarEntusiasta(TipoUsuario.ENTUSIASTA);

        assertThatThrownBy(() -> servicoJwt.validar(
                servicoJwt.gerarTokenRefresh(entusiasta), ServicoJwt.TIPO_ACESSO))
                .isInstanceOf(TokenJwtInvalidoException.class);
    }

    @Test
    @DisplayName("token expirado e rejeitado")
    void tokenExpiradoRejeitado() {
        ServicoJwt emissorComValidadeNegativa = new ServicoJwt(SEGREDO, -1, 7, "carshub-api");
        Entusiasta entusiasta = criarEntusiasta(TipoUsuario.ENTUSIASTA);

        assertThatThrownBy(() -> servicoJwt.validar(
                emissorComValidadeNegativa.gerarTokenAcesso(entusiasta), ServicoJwt.TIPO_ACESSO))
                .isInstanceOf(TokenJwtInvalidoException.class);
    }

    @Test
    @DisplayName("token assinado com outro segredo e rejeitado")
    void assinaturaDeOutroSegredoRejeitada() {
        ServicoJwt outroEmissor = new ServicoJwt("outro-segredo-de-teste-com-mais-de-32-caracteres", 15, 7, "carshub-api");
        Entusiasta entusiasta = criarEntusiasta(TipoUsuario.ENTUSIASTA);

        assertThatThrownBy(() -> servicoJwt.validar(
                outroEmissor.gerarTokenAcesso(entusiasta), ServicoJwt.TIPO_ACESSO))
                .isInstanceOf(TokenJwtInvalidoException.class);
    }

    @Test
    @DisplayName("segredo curto demais derruba a inicializacao (HS256 exige 256 bits)")
    void segredoCurtoFalhaNaCriacao() {
        assertThatThrownBy(() -> new ServicoJwt("curto", 15, 7, "carshub-api"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 caracteres");
    }
}
