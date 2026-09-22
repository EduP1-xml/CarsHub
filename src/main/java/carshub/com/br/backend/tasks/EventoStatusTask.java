package carshub.com.br.backend.tasks;

import carshub.com.br.backend.models.entities.Evento;
import carshub.com.br.backend.models.enums.eventos.StatusEvento;
import carshub.com.br.backend.repositories.EventoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@Slf4j
public class EventoStatusTask {

    private final EventoRepository eventoRepository;

    public EventoStatusTask(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    // Roda automaticamente a cada 30 minutos (1800000 milissegundos)
    @Scheduled(fixedRate = 1800000)
    @Transactional
    public void concluirEventosExpirados() {
        LocalDateTime agora = LocalDateTime.now();
        
        // Busca todos os eventos que ainda constam como ATIVOS, mas a data fim já ficou para trás
        List<Evento> eventosExpirados = eventoRepository.findByStatusAndDataHoraFimBefore(StatusEvento.ATIVO, agora);

        if (!eventosExpirados.isEmpty()) {
            for (Evento evento : eventosExpirados) {
                evento.setStatus(StatusEvento.CONCLUIDO);
                log.info("Evento '{}' (ID: {}) concluído automaticamente pelo sistema.", evento.getNome(), evento.getIdEvento());
            }
            eventoRepository.saveAll(eventosExpirados); // Atualiza todos no banco de uma vez
        }
    }
}