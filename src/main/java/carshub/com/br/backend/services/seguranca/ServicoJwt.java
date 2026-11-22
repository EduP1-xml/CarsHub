package carshub.com.br.backend.services.seguranca;

import carshub.com.br.backend.exceptions.TokenJwtInvalidoException;
import carshub.com.br.backend.models.entities.PrestadorServico;
import carshub.com.br.backend.models.entities.Usuario;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * SPRINT 6 (11.1): emissao e validacao dos tokens JWT (access + refresh, HS256).
 *
 * Contrato de claims (CONGELADO na secao 5.6 do handoff):
 *   sub   = id do usuario (UUID);
 *   roles = as MESMAS authorities de Usuario.getAuthorities() — nomes exatos
 *           dos enums, SEM prefixo ROLE_ (ENTUSIASTA / ORGANIZADOR_EVENTO /
 *           PRESTADOR_SERVICO / MECANICO / GUINCHO; COLECIONADOR vira
 *           ENTUSIASTA). As regras de URL e os @PreAuthorize NAO mudaram.
 *   tipo  = "acesso" | "refresh" (refresh NAO abre rotas — so renova o par).
 *
 * A validacao do access token NAO consulta o banco: o custo de 1 findById por
 * requisicao desapareceu (objetivo do JWT). A desativacao de conta vale no
 * proximo login e na renovacao (que consulta o banco) — o access expira em
 * minutos (validadeAcesso), limitando a janela.
 */
@Service
public class ServicoJwt {

    public static final String CLAIM_ROLES = "roles";
    public static final String CLAIM_TIPO = "tipo";
    public static final String TIPO_ACESSO = "acesso";
    public static final String TIPO_REFRESH = "refresh";

    private final SecretKey chave;
    private final Duration validadeAcesso;
    private final Duration validadeRefresh;
    private final String emissor;

    /**
     * 11.10: o segredo vem SEMPRE do ambiente (JWT_SEGREDO, via application.yaml).
     * Não existe segredo embutido no código — sem a variável configurada a
     * aplicação NÃO inicia (fail-fast com mensagem clara).
     */
    public ServicoJwt(@Value("${app.seguranca.jwt.segredo}") String segredo,
                      @Value("${app.seguranca.jwt.validade-acesso-minutos}") long validadeAcessoMinutos,
                      @Value("${app.seguranca.jwt.validade-refresh-dias}") long validadeRefreshDias,
                      @Value("${app.seguranca.jwt.emissor}") String emissor) {
        if (segredo == null || segredo.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SEGREDO não configurado: defina a variável no .env antes de subir a aplicação (item 11.10).");
        }
        if (segredo.length() < 32) {
            throw new IllegalStateException(
                    "JWT_SEGREDO precisa ter no mínimo 32 caracteres (HS256 exige chave de 256 bits).");
        }

        this.chave = Keys.hmacShaKeyFor(segredo.getBytes(StandardCharsets.UTF_8));
        this.validadeAcesso = Duration.ofMinutes(validadeAcessoMinutos);
        this.validadeRefresh = Duration.ofDays(validadeRefreshDias);
        this.emissor = emissor;
    }

    public long validadeAcessoEmSegundos() {
        return validadeAcesso.toSeconds();
    }

    public String gerarTokenAcesso(Usuario usuario) {
        Instant agora = Instant.now();
        var builder = Jwts.builder()
                .issuer(emissor)
                .subject(usuario.getId().toString())
                .claim(CLAIM_TIPO, TIPO_ACESSO)
                .claim(CLAIM_ROLES, usuario.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .toList())
                .claim("tipoUsuario", usuario.getTipoUsuario().name())
                .claim("nome", usuario.getNome())
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(validadeAcesso)))
                .id(UUID.randomUUID().toString());

        if (usuario instanceof PrestadorServico prestador
                && prestador.getTipoPrestadorServico() != null) {
            builder.claim("tipoPrestador", prestador.getTipoPrestadorServico().name());
        }

        return builder.signWith(chave).compact();
    }

    public String gerarTokenRefresh(Usuario usuario) {
        Instant agora = Instant.now();
        return Jwts.builder()
                .issuer(emissor)
                .subject(usuario.getId().toString())
                .claim(CLAIM_TIPO, TIPO_REFRESH)
                .issuedAt(Date.from(agora))
                .expiration(Date.from(agora.plus(validadeRefresh)))
                .id(UUID.randomUUID().toString())
                .signWith(chave)
                .compact();
    }

    /**
     * Valida assinatura, emissor, validade e o tipo esperado do token.
     * Lanca TokenJwtInvalidoException (401) para qualquer token nao conforme —
     * nunca retorna dados de um token adulterado.
     */
    public TokenAutenticado validar(String token, String tipoEsperado) {
        if (token == null || token.isBlank()) {
            throw new TokenJwtInvalidoException("Token ausente.");
        }

        Claims claims;
        try {
            claims = Jwts.parser()
                    .verifyWith(chave)
                    .requireIssuer(emissor)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new TokenJwtInvalidoException("Token inválido ou expirado. Faça login novamente.");
        }

        String tipo = claims.get(CLAIM_TIPO, String.class);
        if (!tipoEsperado.equals(tipo)) {
            throw new TokenJwtInvalidoException("Tipo de token inválido para esta operação.");
        }

        UUID idUsuario;
        try {
            idUsuario = UUID.fromString(claims.getSubject());
        } catch (IllegalArgumentException ex) {
            throw new TokenJwtInvalidoException("Token inválido ou expirado. Faça login novamente.");
        }

        return new TokenAutenticado(idUsuario, extrairRoles(claims), tipo);
    }

    private List<String> extrairRoles(Claims claims) {
        Object valor = claims.get(CLAIM_ROLES);
        if (valor instanceof List<?> lista) {
            return lista.stream()
                    .filter(String.class::isInstance)
                    .map(Object::toString)
                    .toList();
        }
        return List.of();
    }

    /** Resultado da validacao de um token (dados confiaveis da assinatura). */
    public record TokenAutenticado(UUID idUsuario, List<String> roles, String tipo) {
    }
}
