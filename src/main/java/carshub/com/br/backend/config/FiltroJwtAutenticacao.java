package carshub.com.br.backend.config;

import carshub.com.br.backend.exceptions.TokenJwtInvalidoException;
import carshub.com.br.backend.services.seguranca.ServicoJwt;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * SPRINT 6 (11.1): SUBSTITUI o UserPrincipalAuthenticationFilter (header
 * user-id, removido no item 11.4). Valida o JWT access do header
 * Authorization: Bearer e materializa o SecurityContext SEM consultar o banco:
 * principal = UUID do subject e authorities = claim "roles" (os MESMOS nomes
 * congelados na secao 5.6 — regras de URL e @PreAuthorize NAO mudaram).
 *
 * Regras:
 *   - header ausente -> segue ANONIMO (rota publica funciona; protegida recebe
 *     401 do authenticationEntryPoint);
 *   - header presente mas sem o esquema Bearer, token vazio, invalido,
 *     adulterado, expirado ou do tipo errado -> 401 com corpo pt-BR direto do
 *     filtro (denials de filtro nao passam pelo GlobalException);
 *   - caso contrario -> UsernamePasswordAuthenticationToken(uuid, token, roles).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FiltroJwtAutenticacao extends OncePerRequestFilter {

    private static final String PREFIXO_BEARER = "Bearer ";

    private final ServicoJwt servicoJwt;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String autorizacao = request.getHeader(HttpHeaders.AUTHORIZATION);

        if (autorizacao == null || autorizacao.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!autorizacao.startsWith(PREFIXO_BEARER)) {
            escreverNaoAutorizado(response,
                    "O cabeçalho Authorization deve usar o esquema \"Bearer <token>\".");
            return;
        }

        String token = autorizacao.substring(PREFIXO_BEARER.length()).trim();
        if (token.isBlank()) {
            escreverNaoAutorizado(response, "Token ausente no cabeçalho Authorization.");
            return;
        }

        try {
            ServicoJwt.TokenAutenticado dados = servicoJwt.validar(token, ServicoJwt.TIPO_ACESSO);

            List<SimpleGrantedAuthority> authorities = dados.roles().stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            UsernamePasswordAuthenticationToken autenticacao =
                    new UsernamePasswordAuthenticationToken(dados.idUsuario(), token, authorities);

            SecurityContextHolder.getContext().setAuthentication(autenticacao);
            filterChain.doFilter(request, response);
        } catch (TokenJwtInvalidoException ex) {
            escreverNaoAutorizado(response, ex.getMessage());
        }
    }

    private void escreverNaoAutorizado(HttpServletResponse response, String mensagem) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(mensagem);
    }
}
