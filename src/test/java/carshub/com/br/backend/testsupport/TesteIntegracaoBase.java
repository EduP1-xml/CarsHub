package carshub.com.br.backend.testsupport;

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
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Base dos testes de integracao ponta a ponta.
 *
 * - Contexto Spring COMPLETO + MockMvc (seguranca permitAll; identidade via header user-id).
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
     * No Spring Boot 4 o ObjectMapper nao e mais exposto como bean no contexto de
     * teste, entao a base cria o proprio (com modulos Java 8 registrados).
     * Usado apenas para LER ids das respostas JSON dos testes.
     */
    protected final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

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