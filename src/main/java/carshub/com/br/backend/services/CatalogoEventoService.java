package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.models.dtos.eventos.CatalogoEventoResponseDTO;
import carshub.com.br.backend.models.entities.Evento;
import carshub.com.br.backend.models.enums.eventos.StatusEvento;
import carshub.com.br.backend.models.enums.inscricoes.StatusInscricao;
import carshub.com.br.backend.repositories.EventoRepository;
import carshub.com.br.backend.repositories.InscricaoRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CatalogoEventoService {

    private final EventoRepository eventoRepository;
    private final InscricaoRepository inscricaoRepository;

    public CatalogoEventoService(EventoRepository eventoRepository, InscricaoRepository inscricaoRepository) {
        this.eventoRepository = eventoRepository;
        this.inscricaoRepository = inscricaoRepository;
    }

    public List<CatalogoEventoResponseDTO> listarEventosAtivos(UUID idEntusiastaLogado) {
        return eventoRepository.findAllByStatus(StatusEvento.ATIVO).stream()
                .map(evento -> montarDTO(evento, idEntusiastaLogado))
                .collect(Collectors.toList());
    }

    public CatalogoEventoResponseDTO buscarDetalhesEvento(UUID idEvento, UUID idEntusiastaLogado) {
        Evento evento = eventoRepository.findById(idEvento)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));

        return montarDTO(evento, idEntusiastaLogado);
    }

    private CatalogoEventoResponseDTO montarDTO(Evento evento, UUID idEntusiastaLogado) {
        int vagasOcupadas = inscricaoRepository.countByEvento_IdAndStatus(evento.getId(), StatusInscricao.CONFIRMADA);
        int vagasDisponiveis = Math.max(0, evento.getLimiteParticipantes() - vagasOcupadas);

        boolean isInscrito = false;
        if (idEntusiastaLogado != null) {
            isInscrito = inscricaoRepository.existsByEntusiasta_IdAndEvento_IdAndStatus(
                    idEntusiastaLogado, evento.getId(), StatusInscricao.CONFIRMADA);
        }

        return new CatalogoEventoResponseDTO(
                evento.getId(),
                evento.getNome(),
                evento.getDescricao(),
                evento.getPreco(),
                evento.getRua(),
                evento.getNumero(),
                evento.getBairro(),
                evento.getOrganizadorEvento().getNome(),
                vagasDisponiveis,
                isInscrito,
                evento.getDataHoraInicio(),
                evento.getDataHoraFim()
        );
    }
}