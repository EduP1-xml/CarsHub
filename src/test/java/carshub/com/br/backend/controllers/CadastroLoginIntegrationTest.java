package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.entities.Usuario;
import carshub.com.br.backend.testsupport.TesteIntegracaoBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static carshub.com.br.backend.testsupport.FabricaDeDados.emailUnico;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo de cadastro em 3 etapas + login (EmailService mockado: nenhum e-mail real).
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
    @DisplayName("cadastro em 3 etapas ativa a conta e o login devolve o ID do usuario")
    void cadastroCompletoAtivaContaELogin() throws Exception {
        String email = emailUnico("cad");

        mockMvc.perform(post("/api/carshub/usuarios/cadastrar")
                        .contentType("application/json").content(corpoCadastro(email)))
                .andExpect(status().isCreated());

        // O codigo de verificacao fica salvo no banco (EmailService e mock)
        Usuario usuario = repositorioUsuario.findByLogin(email).orElseThrow();
        assertThat(usuario.getAtivo()).isFalse();
        assertThat(usuario.getCodigoVerificacao()).isNotBlank();

        mockMvc.perform(post("/api/carshub/usuarios/cadastrar/individual")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","cpf":"%s"}
                                """.formatted(email, carshub.com.br.backend.testsupport.FabricaDeDados.cpfUnico())))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/carshub/usuarios/cadastrar/verificarRegistro")
                        .contentType("application/json")
                        .content("""
                                {"email":"%s","codigo":"%s"}
                                """.formatted(email, usuario.getCodigoVerificacao())))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("verificado")));

        Usuario ativado = repositorioUsuario.findByLogin(email).orElseThrow();
        assertThat(ativado.getAtivo()).isTrue();
        assertThat(ativado.getCodigoVerificacao()).isNull();

        String respostaLogin = mockMvc.perform(post("/api/carshub/autenticacao/login")
                        .contentType("application/json")
                        .content("""
                                {"login":"%s","senha":"Senha@123","tipoUsuarioSelecionado":"ENTUSIASTA"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(respostaLogin).contains("ID: " + ativado.getId());
    }

    @Test
    @DisplayName("e-mail duplicado retorna 409")
    void emailDuplicado409() throws Exception {
        String email = emailUnico("dup");

        mockMvc.perform(post("/api/carshub/usuarios/cadastrar")
                        .contentType("application/json").content(corpoCadastro(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/carshub/usuarios/cadastrar")
                        .contentType("application/json").content(corpoCadastro(email)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("senha fora da politica retorna 400")
    void senhaFraca400() throws Exception {
        mockMvc.perform(post("/api/carshub/usuarios/cadastrar")
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

        mockMvc.perform(post("/api/carshub/usuarios/cadastrar")
                        .contentType("application/json").content(corpoCadastro(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/carshub/autenticacao/login")
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

        mockMvc.perform(post("/api/carshub/usuarios/cadastrar")
                        .contentType("application/json").content(corpoCadastro(email)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/carshub/usuarios/cadastrar/reenviarCodigo")
                        .contentType("application/json")
                        .content("{\"email\":\"%s\"}".formatted(email)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("perfil do entusiasta atualizado via PUT responde 200")
    void perfilPut200() throws Exception {
        String email = emailUnico("perf");
        mockMvc.perform(post("/api/carshub/usuarios/cadastrar")
                        .contentType("application/json").content(corpoCadastro(email)))
                .andExpect(status().isCreated());

        UUID id = repositorioUsuario.findByLogin(email).orElseThrow().getId();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/carshub/entusiastas/perfil")
                        .header("user-id", id)
                        .contentType("application/json")
                        .content("{\"nome\":\"Novo Nome\",\"telefone\":\"1188888888\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/carshub/entusiastas/senha/solicitar-codigo")
                        .header("user-id", id))
                .andExpect(status().isOk());
    }
}