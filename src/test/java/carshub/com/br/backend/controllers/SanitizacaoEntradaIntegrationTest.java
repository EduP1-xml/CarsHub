package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.entities.Usuario;
import carshub.com.br.backend.testsupport.TesteIntegracaoBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static carshub.com.br.backend.testsupport.FabricaDeDados.emailUnico;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPRINT 6 (11.6): o texto livre recebido no JSON e sanitizado ANTES da
 * validacao e da persistencia (XSS armazenado). Banco MySQL real: a conta
 * criada PERMANECE gravada apos o teste.
 */
class SanitizacaoEntradaIntegrationTest extends TesteIntegracaoBase {

    @Test
    @DisplayName("script no nome do cadastro e removido antes de persistir")
    void scriptNoNomeDoCadastroERemovido() throws Exception {
        String email = emailUnico("xss");

        mockMvc.perform(post("/api/carshub/usuarios/cadastrar")
                        .with(csrfToken())
                        .contentType("application/json")
                        .content("""
                                {"nome":"<script>alert('x')</script>Fulano","login":"%s","senha":"Senha@123",
                                 "dataNascimento":"1990-01-15","telefone":"11999999999","tipoUsuario":"ENTUSIASTA"}
                                """.formatted(email).replace("\n", "")))
                .andExpect(status().isCreated());

        Usuario usuario = repositorioUsuario.findByLogin(email).orElseThrow();
        assertThat(usuario.getNome()).isEqualTo("Fulano");
    }
}
