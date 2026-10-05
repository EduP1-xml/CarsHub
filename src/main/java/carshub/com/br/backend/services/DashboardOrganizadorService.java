package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.models.dtos.dashboards.DashboardMetricasResponseDTO;
import carshub.com.br.backend.models.dtos.dashboards.HistoricoEventoResponseDTO;
import carshub.com.br.backend.models.entities.Evento;
import carshub.com.br.backend.models.enums.eventos.StatusEvento;
import carshub.com.br.backend.models.enums.inscricoes.StatusInscricao;
import carshub.com.br.backend.repositories.EventoRepository;
import carshub.com.br.backend.repositories.InscricaoRepository;
import carshub.com.br.backend.repositories.OrganizadorEventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Service
public class DashboardOrganizadorService {

    private final EventoRepository eventoRepository;
    private final InscricaoRepository inscricaoRepository;
    private final OrganizadorEventoRepository organizadorRepository;

    // 1. Gera os números gerais do painel
    public DashboardMetricasResponseDTO obterMetricasGerais(UUID organizadorId) {
        if (!organizadorRepository.existsById(organizadorId)) {
            throw new RecursoNaoEncontradoException("Sessão inválida: Organizador não encontrado.");
        }

        int totalEventos = eventoRepository.countByOrganizadorEvento_Id(organizadorId);
        
        Integer totalParticipantes = inscricaoRepository.countTotalParticipantesByOrganizador(organizadorId, StatusInscricao.CONFIRMADA);

        return new DashboardMetricasResponseDTO(totalEventos, totalParticipantes);
    }

    // 2. Gera a lista de eventos que já passaram
    public List<HistoricoEventoResponseDTO> obterHistoricoEventos(UUID organizadorId) {
        if (!organizadorRepository.existsById(organizadorId)) {
            throw new RecursoNaoEncontradoException("Sessão inválida.");
        }

        // Busca eventos concluídos e cancelados, concatenando as duas listas
        List<Evento> concluidos = eventoRepository.findAllByOrganizadorEvento_IdAndStatus(organizadorId, StatusEvento.CONCLUIDO);
        List<Evento> cancelados = eventoRepository.findAllByOrganizadorEvento_IdAndStatus(organizadorId, StatusEvento.CANCELADO);

        return Stream.concat(concluidos.stream(), cancelados.stream())
                .map(this::mapearParaHistoricoDTO)
                .collect(Collectors.toList());
    }

    private HistoricoEventoResponseDTO mapearParaHistoricoDTO(Evento evento) {
        // Para o histórico, contamos todas as inscrições feitas no evento (seja confirmada ou não)
        int participantes = inscricaoRepository.countByEvento_Id(evento.getId());

        return new HistoricoEventoResponseDTO(
                evento.getId(),
                evento.getNome(),
                evento.getStatus(),
                evento.getLimiteParticipantes(),
                participantes
        );
    }
}