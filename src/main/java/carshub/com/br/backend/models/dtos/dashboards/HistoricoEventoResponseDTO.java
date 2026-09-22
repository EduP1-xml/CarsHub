package carshub.com.br.backend.models.dtos.dashboards;

import carshub.com.br.backend.models.enums.eventos.StatusEvento;
import java.util.UUID;

public record HistoricoEventoResponseDTO(
        UUID idEvento,
        String nome,
        StatusEvento status,
        Integer limiteVagas,
        Integer totalParticipantes
) {}