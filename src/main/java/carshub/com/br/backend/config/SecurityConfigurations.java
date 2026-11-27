package carshub.com.br.backend.config;


import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * AUTORIZACAO (07/11/2026) - tres camadas:
 *   1. URL (abaixo): regra grossa por prefixo de rota, fail-closed;
 *   2. @PreAuthorize na classe de cada controller (redundancia de perfil);
 *   3. Services: posse (403) e garagem (422) - NAO sao substituiveis por papel.
 *
 * CONTRATO DE AUTHORITIES (congelado - sera a claim "roles" do JWT na Sprint 6):
 * nomes exatos dos enums, sem prefixo ROLE_:
 *   ENTUSIASTA · ORGANIZADOR_EVENTO · PRESTADOR_SERVICO · MECANICO · GUINCHO.
 * COLECIONADOR NAO e authority: rotas /colecionador/** exigem ENTUSIASTA e a
 * garagem e validada no service (422).
 *
 * CHECKLIST DE MANUTENCAO (toda rota nova):
 *   (a) regra de URL aqui; (b) @PreAuthorize na classe do controller;
 *   (c) checagem de posse no service quando houver recurso de dono;
 *   (d) teste de 401 (sem header) e 403 (perfil errado).
 *
 * Sprint 6 (JWT — 11.1/11.4, implementada em 08/11/2026): o filtro do header
 * user-id (UserPrincipalAuthenticationFilter) foi REMOVIDO e substituido pelo
 * FiltroJwtAutenticacao, que valida o JWT do header Authorization e materializa
 * as MESMAS authorities a partir da claim "roles" (enum.name(), sem prefixo).
 * As regras abaixo e as anotacoes NAO mudaram.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity //habilita @PreAuthorize nos controllers
public class SecurityConfigurations {

    private final List<String> origensFrontend;

    private final FiltroJwtAutenticacao filtroJwt;

    private final RateLimitFilter filtroRateLimit;

    public SecurityConfigurations(@Value("${frontend.url}") String frontendUrl,
                                  FiltroJwtAutenticacao filtroJwt,
                                  RateLimitFilter filtroRateLimit) {
        // SPRINT 6 (11.7): varias origens separadas por virgula. REGRA DE
        // PRODUCAO: back e front DEVEM compartilhar o mesmo dominio/subdominio
        // e trafegar por HTTPS (documentado no handoff, secao 11.7).
        this.origensFrontend = Arrays.stream(frontendUrl.split(","))
                .map(String::trim)
                .filter(origem -> !origem.isBlank())
                .toList();
        this.filtroJwt = filtroJwt;
        this.filtroRateLimit = filtroRateLimit;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
        String cspDirectives = "default-src 'self'; " +
                "script-src 'self'; " +
                "style-src 'self' 'unsafe-inline'; " +
                "img-src 'self' data:; " +
                "font-src 'self'; " +
                "connect-src 'self' " + origensFrontend.stream()
                .map(origem -> origem.replace("http://", "ws://").replace("https://", "wss://"))
                .collect(java.util.stream.Collectors.joining(" ")) + ";";

        return httpSecurity
                .headers(headers -> headers
                        .addHeaderWriter(new ContentSecurityPolicyHeaderWriter(cspDirectives))
                )
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // SPRINT 6 (11.13): CSRF HABILITADO com double-submit por cookie
                // (XSRF-TOKEN, SameSite=Lax) + header X-XSRF-TOKEN na requisicao.
                // O cookie com SameSite=Lax nao acompanha requisicoes iniciadas por
                // sites de terceiros, e o header so pode ser preenchido por JS da
                // MESMA origem (que consegue ler o cookie nao-HttpOnly).
                .csrf(csrf -> csrf
                        .csrfTokenRepository(cookieCsrfTokenRepository())
                        .csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler())
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        // 1. CORS Preflight + infraestrutura publica (sem isso fotos/erros quebram)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/uploads/**", "/error").permitAll()
                        // 2. Rotas publicas da API
                        .requestMatchers("/api/carshub/usuarios/cadastrar/**").permitAll()
                        .requestMatchers("/api/carshub/autenticacao/**").permitAll()
                        .requestMatchers("/api/carshub/eventos/**").permitAll()
                        // 3. Perfis (authorities = nomes dos enums; COLECIONADOR usa ENTUSIASTA)
                        .requestMatchers("/api/carshub/entusiastas/**").hasAuthority("ENTUSIASTA")
                        .requestMatchers("/api/carshub/colecionador/**").hasAuthority("ENTUSIASTA")
                        .requestMatchers("/api/carshub/clubes/**").hasAuthority("ENTUSIASTA")
                        .requestMatchers("/api/carshub/organizadores/**").hasAuthority("ORGANIZADOR_EVENTO")
                        .requestMatchers("/api/carshub/mecanicos/**").hasAuthority("MECANICO")
                        .requestMatchers("/api/carshub/guinchos/**").hasAuthority("GUINCHO")
                        // 4. Fail-closed: rota nova sem regra explicita exige autenticacao
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception -> exception
                        // Denials DA CADEIA DE FILTROS nao passam pelo GlobalException
                        // (@RestControllerAdvice so ve os 403 do @PreAuthorize) - corpos manuais.
                        .authenticationEntryPoint((request, response, erro) ->
                                escreverErro(response, HttpStatus.UNAUTHORIZED,
                                        "Acesso negado: autenticação necessária."))
                        .accessDeniedHandler((request, response, erro) ->
                                escreverErro(response, HttpStatus.FORBIDDEN,
                                        "Acesso negado: você não tem permissão para executar esta operação."))
                )
                // SPRINT 6 (11.1/11.8): o JWT precisa ser registrado PRIMEIRO —
                // o addFilterBefore do rate limit usa FiltroJwtAutenticacao como
                // referencia de ordem, que so existe depois do registro dele.
                .addFilterBefore(filtroJwt, UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(filtroRateLimit, FiltroJwtAutenticacao.class)
                .build();
    }

    private void escreverErro(HttpServletResponse response, HttpStatus status, String mensagem) throws IOException {
        response.setStatus(status.value());
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(mensagem);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    /**
     * SPRINT 6 (11.13): repositorio CSRF por cookie. SameSite=Lax impede o
     * navegador de enviar o cookie em requisicoes cross-site; o front le o
     * cookie (nao-HttpOnly de proposito) e devolve o valor no header
     * X-XSRF-TOKEN — double submit, sem sessao (compativel com STATELESS).
     * Em producao adicionar .secure(true) no customizer (HTTPS — item 11.7).
     */
    private CookieCsrfTokenRepository cookieCsrfTokenRepository() {
        CookieCsrfTokenRepository repositorio = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repositorio.setCookieCustomizer(cookie -> cookie.sameSite("Lax"));
        return repositorio;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(origensFrontend);
        // REGRA TRANSVERSAL 7.5: PATCH incluido nos metodos permitidos do CORS.
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Adicionado "X-XSRF-TOKEN" para o frontend poder enviar o token CSRF de volta
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept", "Origin", "Admin-Token", "X-XSRF-TOKEN"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(); //Criptografar senha shar256
    }
}
