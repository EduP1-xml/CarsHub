package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.PerfilIncompativelException;
import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.exceptions.RegraDeNegocioException;
import carshub.com.br.backend.models.dtos.PaginaRespostaDTO;
import carshub.com.br.backend.models.dtos.eventos.EventoPatchDTO;
import carshub.com.br.backend.models.dtos.eventos.EventoPutDTO;
import carshub.com.br.backend.models.dtos.eventos.EventoRegistroDTO;
import carshub.com.br.backend.models.dtos.eventos.EventoRespostaDTO;
import carshub.com.br.backend.models.dtos.geocodificacao.LocalizacaoGeocodificadaDTO;
import carshub.com.br.backend.models.entities.Evento;
import carshub.com.br.backend.models.entities.OrganizadorEvento;
import carshub.com.br.backend.models.enums.eventos.StatusEvento;
import carshub.com.br.backend.repositories.EventoRepository;
import carshub.com.br.backend.repositories.OrganizadorEventoRepository;
import carshub.com.br.backend.services.file.FileStorageService;
import carshub.com.br.backend.services.geocodificacao.GeocodificacaoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * CRUD do evento pelo organizador.
 *
 * GEOLOCALIZACAO: o endereco do evento e SEMPRE digitado a mao (rua/numero/bairro),
 * entao as coordenadas do pino no mapa vem da Geocoding API (chamada do BACK).
 * A geocodificacao roda no create, no PUT e no PATCH quando o endereco muda; ela e
 * BLOQUEANTE de proposito: sem coordenada o evento nao teria pino, que e requisito
 * do produto. Falha de endereco -> 422; falha do servico de mapas -> 502/503/504.
 *
 * A chamada acontece ANTES de gravar qualquer coisa e antes do upload das fotos,
 * para nao deixar arquivo orfao no disco quando o endereco nao for resolvido.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventoService {

    private final EventoRepository repositorioEvento;
    private final OrganizadorEventoRepository repositorioOrganizador;
    private final FileStorageService servicoArmazenamentoArquivo;
    private final GeocodificacaoService servicoGeocodificacao;

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

        // Antes do upload: se o endereco nao for resolvido, nenhum arquivo fica no disco.
        preencherCoordenadasDoEvento(evento);

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

    /**
     * AJUSTE 1.1: meus eventos paginados NATIVO (Page no SQL), do mais recente
     * (data de inicio) para o mais antigo.
     */
    @Transactional(readOnly = true)
    public Page<EventoRespostaDTO> listarMeusEventos(UUID idOrganizador, int pagina, int tamanho) {
        if (!repositorioOrganizador.existsById(idOrganizador)) {
            throw new RecursoNaoEncontradoException("Sessão inválida.");
        }

        return repositorioEvento
                .findAllByOrganizadorEvento_IdOrderByDataHoraInicioDesc(
                        idOrganizador, PaginaRespostaDTO.pageRequest(pagina, tamanho))
                .map(this::mapearEntidadeParaResponse);
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

        // O PUT substitui o endereco inteiro: as coordenadas sao recalculadas sempre.
        preencherCoordenadasDoEvento(evento);

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

        // Só regeocodifica quando algum campo do ENDERECO realmente mudou: assim um
        // PATCH que mexe apenas no preco nao gasta cota nem depende do Google.
        boolean enderecoAlterado = false;

        if (dados.rua() != null && !dados.rua().isBlank()) {
            enderecoAlterado = enderecoAlterado || !dados.rua().equals(evento.getRua());
            evento.setRua(dados.rua());
        }
        if (dados.numero() != null && !dados.numero().isBlank()) {
            enderecoAlterado = enderecoAlterado || !dados.numero().equals(evento.getNumero());
            evento.setNumero(dados.numero());
        }
        if (dados.bairro() != null && !dados.bairro().isBlank()) {
            enderecoAlterado = enderecoAlterado || !dados.bairro().equals(evento.getBairro());
            evento.setBairro(dados.bairro());
        }

        if (dados.nome() != null && !dados.nome().isBlank()) evento.setNome(dados.nome());
        if (dados.descricao() != null && !dados.descricao().isBlank()) evento.setDescricao(dados.descricao());
        if (dados.preco() != null) evento.setPreco(dados.preco());
        if (dados.limiteParticipantes() != null) evento.setLimiteParticipantes(dados.limiteParticipantes());
        if (dados.complemento() != null) evento.setComplemento(dados.complemento());
        if (dados.dataHoraFim() != null) evento.setDataHoraFim(dados.dataHoraFim());
        if (dados.urlFotoEvento() != null) evento.setUrlFotoEvento(dados.urlFotoEvento());

        if (enderecoAlterado) {
            preencherCoordenadasDoEvento(evento);
        }

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
    // GEOLOCALIZACAO
    // ========================================================

    /**
     * Traduz o endereco digitado pelo organizador em coordenadas para o pino do mapa.
     * BLOQUEANTE: endereco nao encontrado -> EnderecoNaoEncontradoException (422);
     * servico de mapas fora/timeout/sem chave -> 503/504/502. Se voce preferir que o
     * evento seja salvo mesmo sem pino, envolva o corpo num try/catch e logue WARN
     * (as coordenadas ficam nulas e o evento aparece sem marcador ate ser editado).
     */
    private void preencherCoordenadasDoEvento(Evento evento) {
        String enderecoCompleto = montarEnderecoDoEvento(evento);

        LocalizacaoGeocodificadaDTO localizacao = servicoGeocodificacao.geocodificarEndereco(enderecoCompleto);

        evento.setLatitude(localizacao.latitude());
        evento.setLongitude(localizacao.longitude());

        log.info("Evento '{}' geocodificado: {} -> ({}, {})",
                evento.getNome(), enderecoCompleto, localizacao.latitude(), localizacao.longitude());
    }

    /** O complemento fica de fora: "Apto 12" piora a precisao do geocodificador. */
    private String montarEnderecoDoEvento(Evento evento) {
        StringBuilder endereco = new StringBuilder()
                .append(evento.getRua())
                .append(", ")
                .append(evento.getNumero());

        if (evento.getBairro() != null && !evento.getBairro().isBlank()) {
            endereco.append(" - ").append(evento.getBairro());
        }

        return endereco.toString();
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
                evento.getId(),
                evento.getNome(),
                evento.getPreco(),
                evento.getDescricao(),
                evento.getLimiteParticipantes(),
                evento.getRua(),
                evento.getNumero(),
                evento.getComplemento(),
                evento.getBairro(),
                evento.getLatitude(),
                evento.getLongitude(),
                evento.getStatus(),
                evento.getOrganizadorEvento().getNome(),
                evento.getDataHoraInicio(),
                evento.getDataHoraFim()
        );
    }
}