package carshub.com.br.backend.services.seguranca;

import carshub.com.br.backend.models.entities.Auditoria;
import carshub.com.br.backend.repositories.AuditoriaRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.UUID;

/**
 * SPRINT 6 (11.9): registra acessos e acoes sensiveis na tabela Auditoria e
 * no log. REQUIRES_NEW: a auditoria sobrevive mesmo que a transacao da acao
 * original sofra rollback (ex.: login com senha errada nao tem transacao, mas
 * um aceite de proposta que deu conflito 422 ainda precisa ficar registrado).
 *
 * REGRA: nunca passar senha ou token em acao/recurso/detalhes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ServicoAuditoria {

    private final AuditoriaRepository repositorioAuditoria;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String acao, String recurso, String detalhes, UUID idUsuario, boolean sucesso) {
        String ip = ipDoRequisitante();
        repositorioAuditoria.save(Auditoria.novo(acao, recurso, detalhes, idUsuario, ip, sucesso));
        log.info("AUDITORIA [{}] recurso={} ip={} sucesso={}", acao, recurso, ip, sucesso);
    }

    private String ipDoRequisitante() {
        ServletRequestAttributes atributos =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (atributos == null) {
            return "desconhecido";
        }

        HttpServletRequest requisicao = atributos.getRequest();

        // Atras de proxy o IP real vem no X-Forwarded-For (primeiro da lista).
        String encaminhado = requisicao.getHeader("X-Forwarded-For");
        if (encaminhado != null && !encaminhado.isBlank()) {
            return encaminhado.split(",")[0].trim();
        }
        return requisicao.getRemoteAddr() != null ? requisicao.getRemoteAddr() : "desconhecido";
    }
}
