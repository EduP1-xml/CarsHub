package carshub.com.br.backend.models.dtos.dashboards;

public record DashboardMetricasResponseDTO(
        Integer totalEventosCriados,
        Integer totalParticipantesMobilizados
) {}