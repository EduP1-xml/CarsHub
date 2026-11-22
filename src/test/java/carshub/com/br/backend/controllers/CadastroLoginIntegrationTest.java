package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.entities.Usuario;
import carshub.com.br.backend.testsupport.TesteIntegracaoBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static carshub.com.br.backend.testsupport.FabricaDeDados.emailUnico;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo de cadastro em 3 etapas + login (EmailService mockado: nenhum e-mail real).
 * SPRINT 6 (11.1): o login devolve o PAR de tokens JWT (antes: mensagem em texto)
 * e a identidade das demais rotas e o access token no header Authorization.
 * Banco MySQL real: as contas criadas PERMANECEM gravadas apos o teste.
 */
class CadastroLoginIntegrationTest extends TesteIntegracaoBase {

    private String corpoCadastro(String email) {
        return """
                {"nome":"Fulano de Tal","login":"%s","senha":"Senha@123","dataNascimento":"1990-01-15",
                 "telefone":"11999999999","tipoUsuario":"ENTUSIASTA"}
                """.formatted(email).replace("\n", "");
    }

    @Test
    @DisplayName("cadastro em 3 etapas ativa a conta e o login devolve o par de tokens JWT")
    void cadastroCompletoAtivaContaELogin() throws Exception {
        String email = emailUnico("cad");

        mockMvc.perform(post("/api/carshub/usuarios/cadastrar").with(csrfToken())
                        .contentType("application/json").content(corpoCadastro(email)))
                .andExpect(status().isCreated());

        // O codigo de verificacao fica salvo no banco (EmailService e mock)
        Usuario usuario = repositorioUsuario.findByLogin(email).orElseThrow();
        assertThat(usuario.getAtivo()).isFalse();
        assertThat(usuario.getCodigoVerificacao()).isNotBlank();

        mockMvc.perform(post("/api/carshub/usuarios/cadastrar/individual").with(csrfToken())
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","cpf":"%s"}
                                """.formatted(email, carshub.com.br.backend.testsupport.FabricaDeDados.cpfUnico())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/carshub/usuarios/cadastrar/verificarRegistro").with(csrfToken())
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","codigo":"%s"}
                                """.formatted(email, usuario.getCodigoVerificacao())))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("verificado")));

        Usuario ativado = repositorioUsuario.findByLogin(email).orElseThrow();
        assertThat(ativado.getAtivo()).isTrue();
        assertThat(ativado.getCodigoVerificacao()).isNull();

        // SPRINT 6 (11.1): login devolve accessToken + refreshToken + dados do usuario.
        String respostaLogin = mockMvc.perform(post("/api/carshub/autenticacao/login").with(csrfToken())
                        .contentType("application/json")
                        .content("""
                                {"login":"%s","senha":"Senha@123","tipoUsuarioSelecionado":"ENTUSIASTA"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tipoToken").value("Bearer"))
                .andExpect(jsonPath("$.idUsuario").value(ativado.getId().toString()))
                .andReturn().getResponse().getContentAsString();

        JsonNode corpo = objectMapper.readTree(respostaLogin);

        // O access token emitido no login abre rota protegida (fim do header user-id).
        mockMvc.perform(get("/api/carshub/entusiastas/perfil")
                        .header("Authorization", "Bearer " + corpo.get("accessToken").asText()))
                .andExpect(status().isOk());

        // E o refresh token renova o par (rotacao).
        mockMvc.perform(post("/api/carshub/autenticacao/refresh").with(csrfToken())
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"%s\"}".formatted(corpo.get("refreshToken").asText())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    @DisplayName("e-mail duplicado retorna 409")
    void emailDuplicado409() throws Exception {
        String email = emailUnico("dup");

        mockMvc.perform(post("/api/carshub/usuarios/cadastrar").with(csrfToken())
                        .contentType("application/json").content(corpoCadastro(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/carshub/usuarios/cadastrar").with(csrfToken())
                        .contentType("application/json").content(corpoCadastro(email)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("senha fora da politica retorna 400")
    void senhaFraca400() throws Exception {
        mockMvc.perform(post("/api/carshub/usuarios/cadastrar").with(csrfToken())
                        .contentType("application/json")
                        .content("""
                                {"nome":"Fulano","login":"%s","senha":"123","dataNascimento":"1990-01-15",
                                 "telefone":"11999999999","tipoUsuario":"ENTUSIASTA"}
                                """.formatted(emailUnico("fraca")).replace("\n", "")))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("login com senha errada retorna 401")
    void loginSenhaErrada401() throws Exception {
        String email = emailUnico("login");

        mockMvc.perform(post("/api/carshub/usuarios/cadastrar").with(csrfToken())
                        .contentType("application/json").content(corpoCadastro(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/carshub/autenticacao/login").with(csrfToken())
                        .contentType("application/json")
                        .content("""
                                {"login":"%s","senha":"SenhaErrada@1","tipoUsuarioSelecionado":"ENTUSIASTA"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("reenvio imediato do codigo esbarra no cooldown de 60 segundos (422)")
    void reenvioComCooldown422() throws Exception {
        String email = emailUnico("reen");

        mockMvc.perform(post("/api/carshub/usuarios/cadastrar").with(csrfToken())
                        .contentType("application/json").content(corpoCadastro(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/carshub/usuarios/cadastrar/reenviarCodigo").with(csrfToken())
                        .contentType("application/json")
                        .content("{\"email\":\"%s\"}".formatted(email)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("tipoUsuario COLECIONADOR nao pode ser selecionado no cadastro (422)")
    void cadastroNaoAceitaColecionador422() throws Exception {
        String corpo = """
                {"nome":"Fulano de Tal","login":"%s","senha":"Senha@123","dataNascimento":"1990-01-15",
                 "telefone":"11999999999","tipoUsuario":"COLECIONADOR"}
                """.formatted(emailUnico("colsel")).replace("\n", "");

        mockMvc.perform(post("/api/carshub/usuarios/cadastrar").with(csrfToken())
                        .contentType("application/json").content(corpo))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Colecionador")));
    }

    @Test
    @DisplayName("perfil do entusiasta atualizado via PUT responde 200")
    void perfilPut200() throws Exception {
        String email = emailUnico("perf");
        mockMvc.perform(post("/api/carshub/usuarios/cadastrar").with(csrfToken())
                        .contentType("application/json").content(corpoCadastro(email)))
                .andExpect(status().isCreated());

        Usuario usuario = repositorioUsuario.findByLogin(email).orElseThrow();

        // AUTORIZACAO (07/11/2026): contas nao verificadas nao entram,
        // entao o e-mail precisa ser confirmado antes de usar o perfil.
        mockMvc.perform(post("/api/carshub/usuarios/cadastrar/verificarRegistro").with(csrfToken())
                        .contentType("application/json")
                        .content("{\"email\":\"%s\",\"codigo\":\"%s\"}"
                                .formatted(email, usuario.getCodigoVerificacao())))
                .andExpect(status().isOk());

        // SPRINT 6 (11.4): identidade pelo token JWT (header user-id removido).
        mockMvc.perform(put("/api/carshub/entusiastas/perfil").with(csrfToken())
                        .header("Authorization", tokenDe(usuario))
                        .contentType("application/json")
                        .content("{\"nome\":\"Novo Nome\",\"telefone\":\"1188888888\"}"))
                .andExpect(status().isOk());

        // REGRA 4.1: GET do perfil devolve PerfilUsuarioDTO (sem latitude/longitude)
        mockMvc.perform(get("/api/carshub/entusiastas/perfil")
                        .header("Authorization", tokenDe(usuario)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome").value("Novo Nome"))
                .andExpect(jsonPath("$.login").value(email))
                .andExpect(jsonPath("$.latitude").doesNotExist());

        mockMvc.perform(post("/api/carshub/entusiastas/senha/solicitar-codigo").with(csrfToken())
                        .header("Authorization", tokenDe(usuario)))
                .andExpect(status().isOk());
    }
}