package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.Mecanico;
import carshub.com.br.backend.models.entities.OrganizadorEvento;
import carshub.com.br.backend.models.entities.Usuario;
import carshub.com.br.backend.models.enums.tiposUsuarios.TipoUsuario;
import carshub.com.br.backend.services.seguranca.ServicoJwt;
import carshub.com.br.backend.testsupport.TesteIntegracaoBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados.TROCA_DE_OLEO;
import static carshub.com.br.backend.testsupport.FabricaDeDados.LATITUDE_PADRAO;
import static carshub.com.br.backend.testsupport.FabricaDeDados.LONGITUDE_PADRAO;
import static carshub.com.br.backend.testsupport.FabricaDeDados.SENHA_PADRAO;
import static carshub.com.br.backend.testsupport.FabricaDeDados.cnpjUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.cpfUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.criarMecanico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.criarOrganizador;
import static carshub.com.br.backend.testsupport.FabricaDeDados.emailUnico;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * SPRINT 6 (11.1/11.4/11.12): valida as camadas de seguranca com JWT
 * (URL fail-closed + @PreAuthorize + FiltroJwtAutenticacao). Substituiu o
 * cenario do header "user-id" (07/11/2026) pela semantica de token:
 *
 * Cobre: rota publica sem token; rota protegida sem token (401); token
 * adulterado (401); token assinado com outro segredo (401); refresh usado como
 * access (401); perfil errado (403 padronizado); perfil correto (200);
 * /colecionador com authority ENTUSIASTA; login bloqueado para conta nao
 * verificada; RENOVACAO bloqueada para conta nao verificada (o refresh consulta
 * o banco — o access de uma conta desativada expira sozinho em minutos);
 * renovacao valida (200); /uploads publico e rota inexistente (404).
 * Banco MySQL real: os dados criados PERMANECEM gravados apos o teste.
 */
class SegurancaAutorizacaoIntegrationTest extends TesteIntegracaoBase {

    private Entusiasta criarEntusiastaSemGaragem(String prefixo, boolean ativo) {
        Entusiasta entusiasta = new Entusiasta();
        entusiasta.setLogin(emailUnico(prefixo));
        entusiasta.setSenha(SENHA_PADRAO);
        entusiasta.setNome("Entusiasta " + prefixo);
        entusiasta.setDataNascimento(LocalDate.of(1990, 1, 15));
        entusiasta.setTelefone("11999999999");
        entusiasta.setTipoUsuario(TipoUsuario.ENTUSIASTA);
        entusiasta.setAtivo(ativo);
        entusiasta.setCpf(cpfUnico());
        return repositorioEntusiasta.saveAndFlush(entusiasta);
    }

    @Test
    @DisplayName("rota publica do catalogo funciona sem token")
    void catalogoPublicoSemToken200() throws Exception {
        mockMvc.perform(get("/api/carshub/eventos").param("tamanho", "5"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("rota protegida sem token retorna 401 (fail-closed)")
    void rotaProtegidaSemToken401() throws Exception {
        mockMvc.perform(get("/api/carshub/entusiastas/inscricoes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("token adulterado retorna 401 com mensagem util")
    void tokenAdulterado401() throws Exception {
        mockMvc.perform(get("/api/carshub/entusiastas/inscricoes")
                        .header("Authorization", "Bearer token-adulterado-ou-expirado"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("inválido ou expirado")));
    }

    @Test
    @DisplayName("token assinado com outro segredo retorna 401 (assinatura verificada)")
    void tokenComOutroSegredo401() throws Exception {
        Entusiasta entusiasta = criarEntusiastaSemGaragem("secas", true);

        ServicoJwt outroEmissor = new ServicoJwt(
                "outro-segredo-de-teste-com-mais-de-32-caracteres", 15, 7, "carshub-api");

        mockMvc.perform(get("/api/carshub/entusiastas/inscricoes")
                        .header("Authorization", "Bearer " + outroEmissor.gerarTokenAcesso(entusiasta)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("refresh token usado como access retorna 401 (tipos distintos)")
    void refreshTokenUsadoComoAccess401() throws Exception {
        Entusiasta entusiasta = criarEntusiastaSemGaragem("secti", true);

        mockMvc.perform(get("/api/carshub/entusiastas/inscricoes")
                        .header("Authorization", "Bearer " + servicoJwt.gerarTokenRefresh(entusiasta)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("Tipo de token inválido")));
    }

    @Test
    @DisplayName("token valido de outro perfil retorna 403 (organizador em rota de entusiasta)")
    void perfilErrado403OrganizadorEmEntusiasta() throws Exception {
        OrganizadorEvento organizador = criarOrganizador(repositorioOrganizadorEvento,
                emailUnico("secorg"), cnpjUnico());

        mockMvc.perform(get("/api/carshub/entusiastas/inscricoes")
                        .header("Authorization", tokenDe(organizador)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("token valido de outro perfil retorna 403 (entusiasta em rota de mecanico)")
    void perfilErrado403EntusiastaEmMecanico() throws Exception {
        Entusiasta entusiasta = criarEntusiastaSemGaragem("secent", true);

        mockMvc.perform(get("/api/carshub/mecanicos/servicos")
                        .header("Authorization", tokenDe(entusiasta)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("token valido de outro perfil retorna 403 padronizado (mecanico em rota de guincho)")
    void perfilErrado403MecanicoEmGuincho() throws Exception {
        Mecanico mecanico = criarMecanico(repositorioMecanico, emailUnico("secmec"), cnpjUnico(),
                List.of(TROCA_DE_OLEO), LATITUDE_PADRAO, LONGITUDE_PADRAO);

        mockMvc.perform(get("/api/carshub/guinchos/servicos")
                        .header("Authorization", tokenDe(mecanico)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("token do perfil correto acessa a propria rota (mecanico -> 200)")
    void perfilCorretoMecanico200() throws Exception {
        Mecanico mecanico = criarMecanico(repositorioMecanico, emailUnico("secmec2"), cnpjUnico(),
                List.of(TROCA_DE_OLEO), LATITUDE_PADRAO, LONGITUDE_PADRAO);

        mockMvc.perform(get("/api/carshub/mecanicos/servicos")
                        .header("Authorization", tokenDe(mecanico)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("token do perfil correto acessa a propria rota (organizador -> 200)")
    void perfilCorretoOrganizador200() throws Exception {
        OrganizadorEvento organizador = criarOrganizador(repositorioOrganizadorEvento,
                emailUnico("secorg2"), cnpjUnico());

        mockMvc.perform(get("/api/carshub/organizadores/dashboard/metricas")
                        .header("Authorization", tokenDe(organizador)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("/colecionador usa a authority ENTUSIASTA (entusiasta sem garagem acessa com 200)")
    void colecionadorUsaAuthorityEntusiasta200() throws Exception {
        Entusiasta entusiasta = criarEntusiastaSemGaragem("seccol", true);

        mockMvc.perform(get("/api/carshub/colecionador/carros").param("tamanho", "5")
                        .header("Authorization", tokenDe(entusiasta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo.length()").value(0));
    }

    // ==================================================================
    // CLUBES (checklist (d)): 401 sem token, 403 para outros perfis, 200 para entusiasta
    // ==================================================================

    @Test
    @DisplayName("/clubes sem token retorna 401")
    void clubesSemToken401() throws Exception {
        mockMvc.perform(get("/api/carshub/clubes"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("/clubes com token de organizador ou mecanico retorna 403")
    void clubesPerfilErrado403() throws Exception {
        OrganizadorEvento organizador = criarOrganizador(repositorioOrganizadorEvento,
                emailUnico("secclorg"), cnpjUnico());
        mockMvc.perform(get("/api/carshub/clubes").header("Authorization", tokenDe(organizador)))
                .andExpect(status().isForbidden());

        Mecanico mecanico = criarMecanico(repositorioMecanico, emailUnico("secclmec"), cnpjUnico(),
                List.of(TROCA_DE_OLEO), LATITUDE_PADRAO, LONGITUDE_PADRAO);
        mockMvc.perform(get("/api/carshub/clubes").header("Authorization", tokenDe(mecanico)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("/clubes com token de entusiasta retorna 200")
    void clubesEntusiasta200() throws Exception {
        Entusiasta entusiasta = criarEntusiastaSemGaragem("secclent", true);
        mockMvc.perform(get("/api/carshub/clubes").header("Authorization", tokenDe(entusiasta)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("login bloqueado para conta nao verificada (401)")
    void loginContaNaoVerificada401() throws Exception {
        String email = emailUnico("secver");

        mockMvc.perform(post("/api/carshub/usuarios/cadastrar").with(csrfToken())
                        .contentType("application/json")
                        .content("""
                                {"nome":"Fulano","login":"%s","senha":"Senha@123","dataNascimento":"1990-01-15",
                                 "telefone":"11999999999","tipoUsuario":"ENTUSIASTA"}
                                """.formatted(email).replace("\n", "")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/carshub/autenticacao/login").with(csrfToken())
                        .contentType("application/json")
                        .content("""
                                {"login":"%s","senha":"Senha@123","tipoUsuarioSelecionado":"ENTUSIASTA"}
                                """.formatted(email)))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("não verificada")));
    }

    @Test
    @DisplayName("renovacao de token bloqueada para conta nao verificada (401 — refresh consulta o banco)")
    void renovacaoContaNaoVerificada401() throws Exception {
        String email = emailUnico("secref");

        mockMvc.perform(post("/api/carshub/usuarios/cadastrar").with(csrfToken())
                        .contentType("application/json")
                        .content("""
                                {"nome":"Fulano","login":"%s","senha":"Senha@123","dataNascimento":"1990-01-15",
                                 "telefone":"11999999999","tipoUsuario":"ENTUSIASTA"}
                                """.formatted(email).replace("\n", "")))
                .andExpect(status().isCreated());

        Usuario usuario = repositorioUsuario.findByLogin(email).orElseThrow();

        mockMvc.perform(post("/api/carshub/autenticacao/refresh").with(csrfToken())
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"%s\"}"
                                .formatted(servicoJwt.gerarTokenRefresh(usuario))))
                .andExpect(status().isUnauthorized())
                .andExpect(content().string(containsString("não verificada")));
    }

    @Test
    @DisplayName("renovacao com refresh valido devolve novo par de tokens (200)")
    void renovacaoComRefreshValido200() throws Exception {
        Entusiasta entusiasta = criarEntusiastaSemGaragem("secret2", true);

        mockMvc.perform(post("/api/carshub/autenticacao/refresh").with(csrfToken())
                        .contentType("application/json")
                        .content("{\"refreshToken\":\"%s\"}"
                                .formatted(servicoJwt.gerarTokenRefresh(entusiasta))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.idUsuario").value(entusiasta.getId().toString()));
    }

    @Test
    @DisplayName("POST sem token CSRF retorna 403 (CSRF habilitado — 11.13)")
    void postSemTokenCsrf403() throws Exception {
        mockMvc.perform(post("/api/carshub/autenticacao/login")
                        .contentType("application/json")
                        .content("""
                                {"login":"a@b.com","senha":"Senha@123","tipoUsuarioSelecionado":"ENTUSIASTA"}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("arquivos de uploads permanecem publicos (404, nao 401/403)")
    void uploadsPermanecePublico() throws Exception {
        // Sem a regra permitAll para /uploads a resposta seria 401. Arquivo
        // inexistente passa pelo resource handler -> NoResourceFoundException -> 404.
        mockMvc.perform(get("/uploads/arquivo-inexistente.png"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Recurso não encontrado")));
    }

    @Test
    @DisplayName("rota publica inexistente retorna 404 (nao 500)")
    void rotaPublicaInexistente404() throws Exception {
        mockMvc.perform(get("/api/carshub/eventos/inexistente/extra"))
                .andExpect(status().isNotFound());
    }
}
