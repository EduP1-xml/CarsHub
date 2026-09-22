package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.dashboards.DashboardMetricasResponseDTO;
import carshub.com.br.backend.models.dtos.dashboards.HistoricoEventoResponseDTO;
import carshub.com.br.backend.services.DashboardOrganizadorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/carshub/organizadores/dashboard")
public class DashboardOrganizadorController {

    private final DashboardOrganizadorService dashboardService;

    public DashboardOrganizadorController(DashboardOrganizadorService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/metricas")
    public ResponseEntity<DashboardMetricasResponseDTO> obterMetricas(
            @RequestHeader("user-id") UUID organizadorId) {
        
        DashboardMetricasResponseDTO metricas = dashboardService.obterMetricasGerais(organizadorId);
        return ResponseEntity.ok(metricas);
    }

    @GetMapping("/historico")
    public ResponseEntity<List<HistoricoEventoResponseDTO>> obterHistorico(
            @RequestHeader("user-id") UUID organizadorId) {
        
        List<HistoricoEventoResponseDTO> historico = dashboardService.obterHistoricoEventos(organizadorId);
        return ResponseEntity.ok(historico);
    }
}