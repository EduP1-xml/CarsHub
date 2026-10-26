package carshub.com.br.backend.tasks;

import carshub.com.br.backend.models.enums.eventos.StatusEvento;
import carshub.com.br.backend.repositories.EventoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@Slf4j
public class EventoStatusTask {

    private final EventoRepository eventoRepository;

    public EventoStatusTask(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    /**
     * Roda automaticamente a cada 30 minutos (1800000 milissegundos).
     *
     * AJUSTE 1.3: UPDATE condicional em MASSA em vez de carregar + saveAll. Com o
     * @Version nas entidades, o saveAll disputaria linha com edicoes humanas e
     * estouraria ObjectOptimisticLockingFailureException (409) na propria task.
     * O UPDATE em massa nao carrega entidade, nao disputa versao e so conclui o
     * que ainda esta ATIVO — se alguem mudou o status no meio, nada acontece.
     */
    @Scheduled(fixedRate = 1800000)
    @Transactional
    public void concluirEventosExpirados() {
        int concluidos = eventoRepository.concluirEventosExpirados(
                StatusEvento.CONCLUIDO, StatusEvento.ATIVO, LocalDateTime.now());

        if (concluidos > 0) {
            log.info("{} evento(s) com data de fim no passado concluido(s) automaticamente pelo sistema.", concluidos);
        }
    }
}
