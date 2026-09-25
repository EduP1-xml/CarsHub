package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.PerfilIncompativelException;
import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.exceptions.RegraDeNegocioException;
import carshub.com.br.backend.models.dtos.eventos.EventoPatchDTO;
import carshub.com.br.backend.models.dtos.eventos.EventoPutDTO;
import carshub.com.br.backend.models.dtos.eventos.EventoRegistroDTO;
import carshub.com.br.backend.models.dtos.eventos.EventoRespostaDTO;
import carshub.com.br.backend.models.entities.Evento;
import carshub.com.br.backend.models.entities.OrganizadorEvento;
import carshub.com.br.backend.models.enums.eventos.StatusEvento;
import carshub.com.br.backend.repositories.EventoRepository;
import carshub.com.br.backend.repositories.OrganizadorEventoRepository;
import carshub.com.br.backend.services.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventoService {

    private final EventoRepository repositorioEvento;
    private final OrganizadorEventoRepository repositorioOrganizador;
    private final FileStorageService servicoArmazenamentoArquivo;

    @Transactional
    public EventoRespostaDTO criarEvento(EventoRegistroDTO dados, List<MultipartFile> arquivos, UUID idOrganizador) {
        validarConflitoDeDatas(dados.dataHoraInicio(), dados.dataHoraFim());

        OrganizadorEvento organizador = repositorioOrganizador.findById(idOrganizador)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão inválida: Organizador não encontrado."));

        if (arquivos != null && arquivos.size() > 3) {
            throw new IllegalArgumentException("É permitido enviar no máximo 3 fotos por evento.");
        }

        Evento evento = new Evento();
        mapearRegistroDtoParaEntidade(evento, dados);
        evento.setOrganizadorEvento(organizador);

        List<String> urlsFotos = new ArrayList<>();
        if (arquivos != null && !arquivos.isEmpty()) {
            for (MultipartFile arquivo : arquivos) {
                if (arquivo != null && !arquivo.isEmpty()) {
                    String uriDownloadArquivo = servicoArmazenamentoArquivo.salvarArquivo(arquivo);
                    urlsFotos.add(uriDownloadArquivo);
                }
            }
        }
        evento.setUrlFotoEvento(urlsFotos);

        Evento eventoSalvo = repositorioEvento.save(evento);
        return mapearEntidadeParaResponse(eventoSalvo);
    }

    public List<EventoRespostaDTO> listarMeusEventos(UUID idOrganizador) {
        if (!repositorioOrganizador.existsById(idOrganizador)) {
            throw new RecursoNaoEncontradoException("Sessão inválida.");
        }

        return repositorioEvento.findAllByOrganizadorEvento_Id(idOrganizador).stream()
                .map(this::mapearEntidadeParaResponse)
                .collect(Collectors.toList());
    }

    public EventoRespostaDTO buscarMeuEventoPorId(UUID idEvento, UUID idOrganizador) {
        Evento evento = verificarPropriedadeDoEvento(idEvento, idOrganizador);
        return mapearEntidadeParaResponse(evento);
    }

    @Transactional
    public EventoRespostaDTO substituirEvento(UUID idEvento, EventoPutDTO dados, UUID idOrganizador) {
        validarConflitoDeDatas(dados.dataHoraInicio(), dados.dataHoraFim());

        Evento evento = verificarPropriedadeDoEvento(idEvento, idOrganizador);

        // Bloqueia a alteração da data de início se o evento já começou e a pessoa tentou mudar essa data
        if (evento.getDataHoraInicio().isBefore(LocalDateTime.now()) && !evento.getDataHoraInicio().equals(dados.dataHoraInicio())) {
            throw new RegraDeNegocioException("Não é possível alterar a data de início de um evento que já começou.");
        }

        mapearPutDtoParaEntidade(evento, dados);
        Evento eventoAtualizado = repositorioEvento.save(evento);

        return mapearEntidadeParaResponse(eventoAtualizado);
    }

    @Transactional
    public EventoRespostaDTO atualizarEventoCondicional(UUID idEvento, EventoPatchDTO dados, UUID idOrganizador) {
        Evento evento = verificarPropriedadeDoEvento(idEvento, idOrganizador);

        LocalDateTime novaDataInicio = dados.dataHoraInicio() != null ? dados.dataHoraInicio() : evento.getDataHoraInicio();
        LocalDateTime novaDataFim = dados.dataHoraFim() != null ? dados.dataHoraFim() : evento.getDataHoraFim();

        if (dados.dataHoraInicio() != null || dados.dataHoraFim() != null) {
            validarConflitoDeDatas(novaDataInicio, novaDataFim);
        }

        // Bloqueia a alteração parcial da data de início se o evento já começou
        if (dados.dataHoraInicio() != null && !evento.getDataHoraInicio().equals(dados.dataHoraInicio())) {
            if (evento.getDataHoraInicio().isBefore(LocalDateTime.now())) {
                throw new RegraDeNegocioException("Não é possível alterar a data de início de um evento que já começou.");
            }
            evento.setDataHoraInicio(dados.dataHoraInicio());
        }

        if (dados.nome() != null && !dados.nome().isBlank()) evento.setNome(dados.nome());
        if (dados.descricao() != null && !dados.descricao().isBlank()) evento.setDescricao(dados.descricao());
        if (dados.preco() != null) evento.setPreco(dados.preco());
        if (dados.limiteParticipantes() != null) evento.setLimiteParticipantes(dados.limiteParticipantes());
        if (dados.rua() != null && !dados.rua().isBlank()) evento.setRua(dados.rua());
        if (dados.numero() != null && !dados.numero().isBlank()) evento.setNumero(dados.numero());
        if (dados.complemento() != null) evento.setComplemento(dados.complemento());
        if (dados.bairro() != null && !dados.bairro().isBlank()) evento.setBairro(dados.bairro());
        if (dados.dataHoraFim() != null) evento.setDataHoraFim(dados.dataHoraFim());
        if (dados.urlFotoEvento() != null) evento.setUrlFotoEvento(dados.urlFotoEvento());

        Evento eventoAtualizado = repositorioEvento.save(evento);
        return mapearEntidadeParaResponse(eventoAtualizado);
    }

    @Transactional
    public EventoRespostaDTO alterarStatusEvento(UUID idEvento, StatusEvento novoStatus, UUID idOrganizador) {
        Evento evento = verificarPropriedadeDoEvento(idEvento, idOrganizador);

        evento.setStatus(novoStatus);
        Evento eventoAtualizado = repositorioEvento.save(evento);

        return mapearEntidadeParaResponse(eventoAtualizado);
    }

    @Transactional
    public void excluirEvento(UUID idEvento, UUID idOrganizador) {
        Evento evento = verificarPropriedadeDoEvento(idEvento, idOrganizador);
        repositorioEvento.delete(evento);
    }

    // ========================================================
    // MÉTODOS AUXILIARES
    // ========================================================

    private Evento verificarPropriedadeDoEvento(UUID idEvento, UUID idOrganizador) {
        Evento evento = repositorioEvento.findById(idEvento)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado no sistema."));

        if (!evento.getOrganizadorEvento().getId().equals(idOrganizador)) {
            throw new PerfilIncompativelException("Acesso Negado: Você não tem permissão para modificar um evento que pertence a outro organizador.");
        }
        return evento;
    }

    private void validarConflitoDeDatas(LocalDateTime inicio, LocalDateTime fim) {
        if (fim.isBefore(inicio) || fim.isEqual(inicio)) {
            throw new RegraDeNegocioException("A data/hora de término do evento não pode ser igual ou anterior à data de início.");
        }
    }

    private void mapearRegistroDtoParaEntidade(Evento evento, EventoRegistroDTO dados) {
        evento.setNome(dados.nome());
        evento.setDescricao(dados.descricao());
        evento.setPreco(dados.preco());
        evento.setLimiteParticipantes(dados.limiteParticipantes());
        evento.setRua(dados.rua());
        evento.setNumero(dados.numero());
        evento.setComplemento(dados.complemento());
        evento.setBairro(dados.bairro());
        evento.setDataHoraInicio(dados.dataHoraInicio());
        evento.setDataHoraFim(dados.dataHoraFim());
    }

    private void mapearPutDtoParaEntidade(Evento evento, EventoPutDTO dados) {
        evento.setNome(dados.nome());
        evento.setDescricao(dados.descricao());
        evento.setPreco(dados.preco());
        evento.setLimiteParticipantes(dados.limiteParticipantes());
        evento.setRua(dados.rua());
        evento.setNumero(dados.numero());
        evento.setComplemento(dados.complemento());
        evento.setBairro(dados.bairro());
        evento.setDataHoraInicio(dados.dataHoraInicio());
        evento.setDataHoraFim(dados.dataHoraFim());
        if (dados.urlFotoEvento() != null) {
            evento.setUrlFotoEvento(dados.urlFotoEvento());
        }
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