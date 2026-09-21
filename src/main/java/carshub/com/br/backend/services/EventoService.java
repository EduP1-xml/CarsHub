package carshub.com.br.backend.services;
;
import carshub.com.br.backend.exceptions.PerfilIncompativelException;
import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.exceptions.RegraDeNegocioException;
import carshub.com.br.backend.models.dtos.eventos.EventoPatchDTO;
import carshub.com.br.backend.models.dtos.eventos.EventoRegistroDTO;
import carshub.com.br.backend.models.dtos.eventos.EventoRespostaDTO;
import carshub.com.br.backend.models.entities.Evento;
import carshub.com.br.backend.models.entities.OrganizadorEvento;
import carshub.com.br.backend.models.enums.eventos.StatusEvento;
import carshub.com.br.backend.repositories.EventoRepository;
import carshub.com.br.backend.repositories.OrganizadorEventoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventoService {

    private final EventoRepository eventoRepository;
    private final OrganizadorEventoRepository organizadorRepository;

    @Transactional
    public EventoRespostaDTO criarEvento(EventoRegistroDTO dto, UUID organizadorId) {
<<<<<<< HEAD
        validarConflitoDeDatas(dto.dataHoraInicio(), dto.dataHoraFim());

        OrganizadorEvento organizador = organizadorRepository.findById(organizadorId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão inválida: Organizador não encontrado."));
=======
        OrganizadorEvento organizador = organizadorRepository.findById(organizadorId).orElseThrow(() -> new RecursoNaoEncontradoException("Sessão inválida: Organizador não encontrado."));
>>>>>>> f65249c7e34be2122f542a11bc66338e92c0d478

        Evento evento = new Evento();
        mapearRegistroDtoParaEntidade(evento, dto);
        evento.setOrganizadorEvento(organizador);

        Evento eventoSalvo = eventoRepository.save(evento);
        return mapearEntidadeParaResponse(eventoSalvo);
    }

    public List<EventoRespostaDTO> listarMeusEventos(UUID organizadorId) {
        if (!organizadorRepository.existsById(organizadorId)) {
            throw new RecursoNaoEncontradoException("Sessão inválida.");
        }

        return eventoRepository.findAllByOrganizadorEvento_Id(organizadorId).stream()
                .map(this::mapearEntidadeParaResponse)
                .collect(Collectors.toList());
    }

    public EventoRespostaDTO buscarMeuEventoPorId(UUID eventoId, UUID organizadorId) {
        Evento evento = verificarPropriedadeDoEvento(eventoId, organizadorId);
        return mapearEntidadeParaResponse(evento);
    }

    @Transactional
    public EventoRespostaDTO substituirEvento(UUID eventoId, EventoRegistroDTO dto, UUID organizadorId) {
        validarConflitoDeDatas(dto.dataHoraInicio(), dto.dataHoraFim());

        Evento evento = verificarPropriedadeDoEvento(eventoId, organizadorId);

        mapearRegistroDtoParaEntidade(evento, dto);
        Evento eventoAtualizado = eventoRepository.save(evento);

        return mapearEntidadeParaResponse(eventoAtualizado);
    }

    @Transactional
    public EventoRespostaDTO atualizarEventoCondicional(UUID eventoId, EventoPatchDTO dto, UUID organizadorId) {
        Evento evento = verificarPropriedadeDoEvento(eventoId, organizadorId);

        LocalDateTime novaDataInicio = dto.dataHoraInicio() != null ? dto.dataHoraInicio() : evento.getDataHoraInicio();
        LocalDateTime novaDataFim = dto.dataHoraFim() != null ? dto.dataHoraFim() : evento.getDataHoraFim();

        if (dto.dataHoraInicio() != null || dto.dataHoraFim() != null) {
            validarConflitoDeDatas(novaDataInicio, novaDataFim);
        }

        if (dto.nome() != null && !dto.nome().isBlank()) evento.setNome(dto.nome());
        if (dto.descricao() != null && !dto.descricao().isBlank()) evento.setDescricao(dto.descricao());
        if (dto.preco() != null) evento.setPreco(dto.preco());
        if (dto.limiteParticipantes() != null) evento.setLimiteParticipantes(dto.limiteParticipantes());
        if (dto.rua() != null && !dto.rua().isBlank()) evento.setRua(dto.rua());
        if (dto.numero() != null && !dto.numero().isBlank()) evento.setNumero(dto.numero());
        if (dto.complemento() != null) evento.setComplemento(dto.complemento());
        if (dto.bairro() != null && !dto.bairro().isBlank()) evento.setBairro(dto.bairro());

        if (dto.dataHoraInicio() != null) evento.setDataHoraInicio(dto.dataHoraInicio());
        if (dto.dataHoraFim() != null) evento.setDataHoraFim(dto.dataHoraFim());

        Evento eventoAtualizado = eventoRepository.save(evento);
        return mapearEntidadeParaResponse(eventoAtualizado);
    }

    @Transactional
    public EventoRespostaDTO alterarStatusEvento(UUID eventoId, StatusEvento novoStatus, UUID organizadorId) {
        Evento evento = verificarPropriedadeDoEvento(eventoId, organizadorId);

        evento.setStatus(novoStatus);
        Evento eventoAtualizado = eventoRepository.save(evento);

        return mapearEntidadeParaResponse(eventoAtualizado);
    }

    @Transactional
    public void excluirEvento(UUID eventoId, UUID organizadorId) {
        Evento evento = verificarPropriedadeDoEvento(eventoId, organizadorId);
        eventoRepository.delete(evento);
    }

    // ========================================================
    // MÉTODOS AUXILIARES
    // ========================================================

    private Evento verificarPropriedadeDoEvento(UUID eventoId, UUID organizadorId) {
        Evento evento = eventoRepository.findById(eventoId).orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado no sistema."));

        if (!evento.getOrganizadorEvento().getId().equals(organizadorId)) {
            throw new PerfilIncompativelException("Acesso Negado: Você não tem permissão para modificar um evento que pertence a outro organizador.");
        }
        return evento;
    }

    private void validarConflitoDeDatas(LocalDateTime inicio, LocalDateTime fim) {
        if (fim.isBefore(inicio) || fim.isEqual(inicio)) {
            throw new RegraDeNegocioException("A data/hora de término do evento não pode ser igual ou anterior à data de início.");
        }
    }

    private void mapearRegistroDtoParaEntidade(Evento evento, EventoRegistroDTO dto) {
        evento.setNome(dto.nome());
        evento.setDescricao(dto.descricao());
        evento.setPreco(dto.preco());
        evento.setLimiteParticipantes(dto.limiteParticipantes());
        evento.setRua(dto.rua());
        evento.setNumero(dto.numero());
        evento.setComplemento(dto.complemento());
        evento.setBairro(dto.bairro());
        evento.setDataHoraInicio(dto.dataHoraInicio());
        evento.setDataHoraFim(dto.dataHoraFim());
    }

    private EventoRespostaDTO mapearEntidadeParaResponse(Evento evento) {
        return new EventoRespostaDTO(
                evento.getIdEvento(),
                evento.getNome(),
                evento.getPreco(),
                evento.getDescricao(),
                evento.getLimiteParticipantes(),
                evento.getRua(),
                evento.getNumero(),
                evento.getComplemento(),
                evento.getBairro(),
                evento.getStatus(),
                evento.getOrganizadorEvento().getNome(),
                evento.getDataHoraInicio(),
                evento.getDataHoraFim()
        );
    }
}