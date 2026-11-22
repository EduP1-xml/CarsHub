package carshub.com.br.backend.controllers;

import carshub.com.br.backend.testsupport.TesteIntegracaoBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPRINT 6 (11.8): rate limit de LOGIN com janela deslizante. Contexto proprio
 * (@SpringBootTest com properties) porque a suite inteira desliga o filtro
 * (application-test.yaml) — aqui ele volta LIGADO com limite apertado:
 * 2 tentativas de login por minuto por IP; a 3a devolve 429 + Retry-After.
 * O MockMvc dispara sempre do mesmo IP (127.0.0.1), o que torna o cenario
 * determinista.
 */
@SpringBootTest(properties = {
        "app.seguranca.rate-limit.habilitado=true",
        "app.seguranca.rate-limit.login-por-minuto=2",
        "app.seguranca.rate-limit.refresh-por-minuto=1000",
        "app.seguranca.rate-limit.cadastro-por-hora=1000",
        "app.seguranca.rate-limit.requisicoes-por-janela=1000"
})
class RateLimitIntegrationTest extends TesteIntegracaoBase {

    private static final String CORPO_LOGIN = """
            {"login":"nao-existe@teste.com","senha":"Senha@123","tipoUsuarioSelecionado":"ENTUSIASTA"}
            """;

    @Test
    @DisplayName("3a tentativa de login no minuto devolve 429 com Retry-After (mesmo IP)")
    void terceiroLoginNoMinutoRetorna429() throws Exception {
        for (int tentativa = 0; tentativa < 2; tentativa++) {
            mockMvc.perform(post("/api/carshub/autenticacao/login")
                            .with(csrfToken())
                            .contentType("application/json")
                            .content(CORPO_LOGIN))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/carshub/autenticacao/login")
                        .with(csrfToken())
                        .contentType("application/json")
                        .content(CORPO_LOGIN))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"));
    }
}
