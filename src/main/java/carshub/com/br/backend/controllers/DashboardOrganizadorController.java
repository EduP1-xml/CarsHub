package carshub.com.br.backend.controllers;

import carshub.com.br.backend.config.UsuarioAutenticado;

import org.springframework.security.access.prepost.PreAuthorize;

import carshub.com.br.backend.models.dtos.PaginaRespostaDTO;
import carshub.com.br.backend.models.dtos.dashboards.DashboardMetricasResponseDTO;
import carshub.com.br.backend.models.dtos.dashboards.HistoricoEventoResponseDTO;
import carshub.com.br.backend.services.DashboardOrganizadorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/carshub/organizadores/dashboard")
@PreAuthorize("hasAuthority('ORGANIZADOR_EVENTO')")
public class DashboardOrganizadorController {

    private final DashboardOrganizadorService dashboardService;

    public DashboardOrganizadorController(DashboardOrganizadorService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/metricas")
    public ResponseEntity<DashboardMetricasResponseDTO> obterMetricas(
            @UsuarioAutenticado UUID organizadorId) {
        
        DashboardMetricasResponseDTO metricas = dashboardService.obterMetricasGerais(organizadorId);
        return ResponseEntity.ok(metricas);
    }

    @GetMapping("/historico")
    public ResponseEntity<PaginaRespostaDTO<HistoricoEventoResponseDTO>> obterHistorico(
            @UsuarioAutenticado UUID organizadorId,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamanho", defaultValue = "5") int tamanho) {

        return ResponseEntity.ok(dashboardService.obterHistoricoEventos(organizadorId, pagina, tamanho));
    }
}