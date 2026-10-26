package carshub.com.br.backend.models.dtos.dashboards;

import carshub.com.br.backend.models.enums.eventos.StatusEvento;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AJUSTE 1.1b: projecao de leitura do historico do dashboard. Carrega a
 * dataHoraFim (chave da mesclagem das duas fontes CONCLUIDO/CANCELADO) que o
 * DTO de resposta nao expoe. Nao e contrato de API: nunca sai como resposta.
 */
public record EventoHistoricoProjecaoDTO(
        UUID idEvento,
        String nome,
        StatusEvento status,
        Integer limiteVagas,
        LocalDateTime dataHoraFim
) {}
