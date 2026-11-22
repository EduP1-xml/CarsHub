package carshub.com.br.backend.tasks;

import carshub.com.br.backend.repositories.servicos.ServicoGuinchoRepository;
import carshub.com.br.backend.repositories.servicos.ServicoMecanicoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * SPRINT 6 (11.11 — LGPD): anonimizacao periodica dos COMENTARIOS de
 * avaliacao (texto livre do titular) dos servicos FINALIZADOS ha mais que a
 * retencao configurada (app.lgpd.dias-retencao-comentarios-servicos,
 * default 365 dias). As notas permanecem (agregados de reputacao); fotos e
 * coordenadas seguem a politica definida em Deepseek/LGPD-POLITICA.md.
 *
 * UPDATE condicional em massa (mesmo padrao da EventoStatusTask) e rodada
 * diaria as 03:00 — desligada junto com as demais tasks no profile "test"
 * (spring.task.scheduling.enabled=false), alem do gatilho
 * app.lgpd.anonimizacao-habilitada.
 */
@Component
@Slf4j
@ConditionalOnProperty(name = "app.lgpd.anonimizacao-habilitada", havingValue = "true", matchIfMissing = true)
public class LgpdAnonimizacaoTask {

    private final ServicoMecanicoRepository repositorioServicoMecanico;
    private final ServicoGuinchoRepository repositorioServicoGuincho;
    private final long diasRetencao;

    public LgpdAnonimizacaoTask(ServicoMecanicoRepository repositorioServicoMecanico,
                                ServicoGuinchoRepository repositorioServicoGuincho,
                                @Value("${app.lgpd.dias-retencao-comentarios-servicos:365}") long diasRetencao) {
        this.repositorioServicoMecanico = repositorioServicoMecanico;
        this.repositorioServicoGuincho = repositorioServicoGuincho;
        this.diasRetencao = diasRetencao;
    }

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void anonimizarComentariosDeServicosAntigos() {
        LocalDateTime limite = LocalDateTime.now().minusDays(diasRetencao);

        int mecanicos = repositorioServicoMecanico.anonimizarComentariosFinalizados(limite,
                carshub.com.br.backend.models.enums.servicos.StatusServico.FINALIZADO);
        int guinchos = repositorioServicoGuincho.anonimizarComentariosFinalizados(limite,
                carshub.com.br.backend.models.enums.servicos.StatusServico.FINALIZADO);

        if (mecanicos > 0 || guinchos > 0) {
            log.info("LGPD: {} comentario(s) de servico mecanico e {} de guincho anonimizados "
                            + "(FINALIZADO ha mais de {} dia(s)).",
                    mecanicos, guinchos, diasRetencao);
        }
    }
}
