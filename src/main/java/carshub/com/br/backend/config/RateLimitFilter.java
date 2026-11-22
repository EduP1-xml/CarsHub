package carshub.com.br.backend.config;

import carshub.com.br.backend.services.seguranca.ControleRateLimit;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.List;

/**
 * SPRINT 6 (11.8): rate limiting por IP com janela deslizante (antes dos
 * filtros de autenticacao, para contar tambem as tentativas de login).
 *
 * Baldes (configuraveis em app.seguranca.rate-limit.*):
 *   - login:    POST /autenticacao/login        (forca bruta);
 *   - refresh:  POST /autenticacao/refresh      (abuso de rotacao);
 *   - cadastro: POST /usuarios/cadastrar/**     (criacao de contas em massa);
 *   - global:   todas as demais requisicoes da API.
 *
 * Resposta 429 com Retry-After e corpo pt-BR, escrita DIRETO pelo filtro
 * (denials de filtro nao passam pelo GlobalException).
 * app.seguranca.rate-limit.habilitado=false desliga tudo (profile test).
 */
@Component
@Slf4j
public class RateLimitFilter extends OncePerRequestFilter {

    private record Regra(String nome, String prefixoRota, int limite, Duration janela, String mensagem) {
    }

    private final boolean habilitado;
    private final ControleRateLimit controle;
    private final List<Regra> regras;

    public RateLimitFilter(
            @Value("${app.seguranca.rate-limit.habilitado:true}") boolean habilitado,
            @Value("${app.seguranca.rate-limit.janela-segundos:60}") long janelaSegundos,
            @Value("${app.seguranca.rate-limit.requisicoes-por-janela:120}") int requisicoesPorJanela,
            @Value("${app.seguranca.rate-limit.login-por-minuto:10}") int loginPorMinuto,
            @Value("${app.seguranca.rate-limit.refresh-por-minuto:30}") int refreshPorMinuto,
            @Value("${app.seguranca.rate-limit.cadastro-por-hora:20}") int cadastroPorHora) {
        this.habilitado = habilitado;
        this.controle = new ControleRateLimit(Clock.systemUTC());

        this.regras = List.of(
                new Regra("login", "/api/carshub/autenticacao/login",
                        loginPorMinuto, Duration.ofMinutes(1),
                        "Muitas tentativas de login. Aguarde um minuto e tente novamente."),
                new Regra("refresh", "/api/carshub/autenticacao/refresh",
                        refreshPorMinuto, Duration.ofMinutes(1),
                        "Muitas renovações de token. Aguarde um minuto e tente novamente."),
                new Regra("cadastro", "/api/carshub/usuarios/cadastrar",
                        cadastroPorHora, Duration.ofHours(1),
                        "Muitos cadastros a partir deste endereço. Tente novamente mais tarde."),
                new Regra("global", "",
                        requisicoesPorJanela, Duration.ofSeconds(janelaSegundos),
                        "Muitas requisições em pouco tempo. Aguarde alguns instantes e tente novamente."));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!habilitado) {
            filterChain.doFilter(request, response);
            return;
        }

        String caminho = request.getRequestURI();
        Regra regra = regras.stream()
                .filter(r -> !r.prefixoRota().isEmpty() && caminho.startsWith(r.prefixoRota()))
                .findFirst()
                .orElseGet(() -> regras.get(regras.size() - 1)); // global

        String ip = ipDoCliente(request);
        if (!controle.permitir(ip + "|" + regra.nome(), regra.limite(), regra.janela())) {
            log.warn("Rate limit atingido [{}] ip={} rota={}", regra.nome(), ip, caminho);
            escreverLimiteExcedido(response, regra);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void escreverLimiteExcedido(HttpServletResponse response, Regra regra) throws IOException {
        // 429 = Too Many Requests (literal: o alias SC_TOO_MANY_REQUESTS nao
        // existe nesta versao do jakarta.servlet).
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(regra.janela().toSeconds()));
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(regra.mensagem());
    }

    private String ipDoCliente(HttpServletRequest request) {
        String encaminhado = request.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank()) {
            return encaminhado.split(",")[0].trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "desconhecido";
    }
}
