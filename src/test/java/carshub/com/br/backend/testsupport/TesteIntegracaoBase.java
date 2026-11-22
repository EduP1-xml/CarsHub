package carshub.com.br.backend.testsupport;

import carshub.com.br.backend.models.entities.Usuario;
import carshub.com.br.backend.repositories.CarroRepository;
import carshub.com.br.backend.repositories.EntusiastaRepository;
import carshub.com.br.backend.repositories.EventoRepository;
import carshub.com.br.backend.repositories.GuinchoRepository;
import carshub.com.br.backend.repositories.InscricaoRepository;
import carshub.com.br.backend.repositories.MecanicoRepository;
import carshub.com.br.backend.repositories.OrganizadorEventoRepository;
import carshub.com.br.backend.repositories.UsuarioRepository;
import carshub.com.br.backend.repositories.servicos.ServicoGuinchoRepository;
import carshub.com.br.backend.repositories.servicos.ServicoMecanicoRepository;
import carshub.com.br.backend.services.email.EmailService;
import carshub.com.br.backend.services.geocodificacao.GeocodificacaoService;
import carshub.com.br.backend.services.seguranca.ServicoJwt;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Base dos testes de integracao ponta a ponta.
 *
 * - Contexto Spring COMPLETO + MockMvc (seguranca por perfil: regras de URL +
 *   @PreAuthorize).
 * - SPRINT 6 (11.12): a identidade e o JWT — {@link #tokenDe(Usuario)} gera um
 *   access token real para a fixture e os testes usam
 *   .header("Authorization", tokenDe(usuario)) no lugar do antigo header
 *   "user-id" (removido no item 11.4).
 * - SPRINT 6 (11.13): o CSRF esta HABILITADO (CookieCsrfTokenRepository). As
 *   chamadas mutantes usam .with({@link #csrfToken()}) para satisfazer o
 *   double-submit (cookie XSRF-TOKEN + parametro _csrf com o mesmo valor) — o
 *   .with(csrf()) do Spring Security Test NAO funciona com o cookie repository,
 *   pois o token esperado e lido do cookie da requisicao.
 * - Banco MySQL REAL (localhost:3307, ddl-auto: update): os dados criados pelos
 *   testes PERMANECEM no banco depois da execucao (decisao do time) - por isso
 *   todas as fixtures usam emails/CPF/CNPJ/placas unicos.
 * - EmailService e GeocodificacaoService sao MOCKADOS: os testes nao disparam
 *   e-mail real nem consomem a API do Google Maps.
 * - Profile "test" desliga o agendamento (@Scheduled) para as tasks nao
 *   interferirem durante a execucao.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class TesteIntegracaoBase {

    @Autowired
    protected MockMvc mockMvc;

    /**
     * SPRINT 6 (11.13): satisfaz o CSRF por double-submit (CookieCsrfTokenRepository)
     * nas chamadas mutantes: grava o cookie XSRF-TOKEN e o parametro _csrf com o
     * MESMO valor. Em GET o filtro CSRF nao valida nada (o helper nao interfere).
     */
    protected RequestPostProcessor csrfToken() {
        return request -> {
            String token = "token-csrf-de-teste";
            request.setParameter("_csrf", token);
            request.setAttribute(CsrfToken.class.getName(),
                    new DefaultCsrfToken("X-XSRF-TOKEN", "_csrf", token));
            request.setCookies(new Cookie("XSRF-TOKEN", token));
            return request;
        };
    }

    /**
     * No Spring Boot 4 o ObjectMapper nao e mais exposto como bean no contexto de
     * teste, entao a base cria o proprio (com modulos Java 8 registrados).
     * Usado apenas para LER ids das respostas JSON dos testes.
     */
    protected final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    /**
     * SPRINT 6 (11.12): emite o access token JWT do perfil da fixture.
     * Uso: mockMvc.perform(...).header("Authorization", tokenDe(usuario)).
     */
    @Autowired
    protected ServicoJwt servicoJwt;

    protected String tokenDe(Usuario usuario) {
        return "Bearer " + servicoJwt.gerarTokenAcesso(usuario);
    }

    @Autowired
    protected UsuarioRepository repositorioUsuario;
    @Autowired
    protected EntusiastaRepository repositorioEntusiasta;
    @Autowired
    protected MecanicoRepository repositorioMecanico;
    @Autowired
    protected GuinchoRepository repositorioGuincho;
    @Autowired
    protected OrganizadorEventoRepository repositorioOrganizadorEvento;
    @Autowired
    protected CarroRepository repositorioCarro;
    @Autowired
    protected EventoRepository repositorioEvento;
    @Autowired
    protected InscricaoRepository repositorioInscricao;
    @Autowired
    protected ServicoMecanicoRepository repositorioServicoMecanico;
    @Autowired
    protected ServicoGuinchoRepository repositorioServicoGuincho;

    @MockitoBean
    protected EmailService servicoEmail;

    @MockitoBean
    protected GeocodificacaoService servicoGeocodificacao;
}