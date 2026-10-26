package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.models.dtos.PaginaRespostaDTO;
import carshub.com.br.backend.models.dtos.dashboards.DashboardMetricasResponseDTO;
import carshub.com.br.backend.models.dtos.dashboards.EventoHistoricoProjecaoDTO;
import carshub.com.br.backend.models.dtos.dashboards.HistoricoEventoResponseDTO;
import carshub.com.br.backend.models.enums.eventos.StatusEvento;
import carshub.com.br.backend.models.enums.inscricoes.StatusInscricao;
import carshub.com.br.backend.repositories.EventoRepository;
import carshub.com.br.backend.repositories.InscricaoRepository;
import carshub.com.br.backend.repositories.OrganizadorEventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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

    /**
     * 2. Historico de eventos que ja passaram. AJUSTE 1.1b: paginado com a
     * tecnica da JANELA e com PROJECAO de escalares — as duas fontes (CONCLUIDO
     * e CANCELADO) sao paginadas no SQL devolvendo o EventoHistoricoProjecaoDTO
     * (que carrega a dataHoraFim, chave da mesclagem) e os participantes de
     * todos os eventos da janela vêm em UMA consulta (sem N+1).
     */
    @Transactional(readOnly = true)
    public PaginaRespostaDTO<HistoricoEventoResponseDTO> obterHistoricoEventos(UUID organizadorId, int pagina, int tamanho) {
        if (!organizadorRepository.existsById(organizadorId)) {
            throw new RecursoNaoEncontradoException("Sessão inválida.");
        }

        PageRequest pageRequest = PaginaRespostaDTO.pageRequest(pagina, tamanho);
        PageRequest janela = PageRequest.of(0, (pageRequest.getPageNumber() + 1) * pageRequest.getPageSize());

        Page<EventoHistoricoProjecaoDTO> paginaConcluidos = eventoRepository
                .projetarHistoricoPorOrganizador(organizadorId, StatusEvento.CONCLUIDO, janela);
        Page<EventoHistoricoProjecaoDTO> paginaCancelados = eventoRepository
                .projetarHistoricoPorOrganizador(organizadorId, StatusEvento.CANCELADO, janela);

        List<EventoHistoricoProjecaoDTO> eventos = Stream.concat(
                        paginaConcluidos.getContent().stream(), paginaCancelados.getContent().stream())
                .sorted(Comparator.comparing(EventoHistoricoProjecaoDTO::dataHoraFim,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());

        List<UUID> ids = eventos.stream().map(EventoHistoricoProjecaoDTO::idEvento).toList();

        Map<UUID, Long> participantesPorEvento = new HashMap<>();
        for (Object[] linha : inscricaoRepository.contarTodasPorEventos(ids)) {
            participantesPorEvento.put((UUID) linha[0], (Long) linha[1]);
        }

        List<HistoricoEventoResponseDTO> conteudo = eventos.stream()
                .map(evento -> mapearParaHistoricoDTO(evento, participantesPorEvento))
                .collect(Collectors.toList());

        int inicio = pageRequest.getPageNumber() * pageRequest.getPageSize();
        int fim = Math.min(inicio + pageRequest.getPageSize(), conteudo.size());
        List<HistoricoEventoResponseDTO> recorte =
                (inicio >= conteudo.size()) ? List.of() : conteudo.subList(inicio, fim);

        long total = paginaConcluidos.getTotalElements() + paginaCancelados.getTotalElements();

        return PaginaRespostaDTO.de(recorte, total, pageRequest.getPageNumber(), pageRequest.getPageSize());
    }

    private HistoricoEventoResponseDTO mapearParaHistoricoDTO(EventoHistoricoProjecaoDTO evento,
                                                              Map<UUID, Long> participantesPorEvento) {
        long participantes = participantesPorEvento.getOrDefault(evento.idEvento(), 0L);

        return new HistoricoEventoResponseDTO(
                evento.idEvento(),
                evento.nome(),
                evento.status(),
                evento.limiteVagas(),
                (int) participantes
        );
    }
}