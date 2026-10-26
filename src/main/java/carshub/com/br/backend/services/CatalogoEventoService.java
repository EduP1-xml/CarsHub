package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.models.dtos.PaginaRespostaDTO;
import carshub.com.br.backend.models.dtos.eventos.CatalogoEventoProjecaoDTO;
import carshub.com.br.backend.models.dtos.eventos.CatalogoEventoResponseDTO;
import carshub.com.br.backend.models.entities.Evento;
import carshub.com.br.backend.models.enums.eventos.StatusEvento;
import carshub.com.br.backend.models.enums.inscricoes.StatusInscricao;
import carshub.com.br.backend.repositories.EventoRepository;
import carshub.com.br.backend.repositories.InscricaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CatalogoEventoService {

    private final EventoRepository eventoRepository;
    private final InscricaoRepository inscricaoRepository;

    /**
     * AJUSTE 1.1b: catalogo paginado NATIVO + PROJECAO de escalares — o SQL
     * devolve o CatalogoEventoProjecaoDTO sem instanciar entidades; os dois
     * agregados por evento (vagas ocupadas e flag inscrito) vem em UMA consulta
     * cada para a pagina inteira — sem N+1.
     */
    @Transactional(readOnly = true)
    public Page<CatalogoEventoResponseDTO> listarEventosAtivos(UUID idEntusiastaLogado, int pagina, int tamanho) {
        PageRequest pageRequest = PaginaRespostaDTO.pageRequest(pagina, tamanho);

        Page<CatalogoEventoProjecaoDTO> paginaEventos = eventoRepository
                .projetarCatalogoAtivos(StatusEvento.ATIVO, pageRequest);

        List<UUID> idsDosEventos = paginaEventos.getContent().stream()
                .map(CatalogoEventoProjecaoDTO::idEvento)
                .toList();

        Map<UUID, Long> vagasOcupadasPorEvento = new HashMap<>();
        for (Object[] linha : inscricaoRepository.contarConfirmadasPorEventos(idsDosEventos, StatusInscricao.CONFIRMADA)) {
            vagasOcupadasPorEvento.put((UUID) linha[0], (Long) linha[1]);
        }

        Set<UUID> eventosInscritos = new HashSet<>(idEntusiastaLogado == null
                ? List.of()
                : inscricaoRepository.buscarIdsDeEventosInscritos(idEntusiastaLogado, idsDosEventos, StatusInscricao.CONFIRMADA));

        return paginaEventos.map(projecao -> montarDTO(projecao, vagasOcupadasPorEvento, eventosInscritos));
    }

    @Transactional(readOnly = true)
    public CatalogoEventoResponseDTO buscarDetalhesEvento(UUID idEvento, UUID idEntusiastaLogado) {
        Evento evento = eventoRepository.findById(idEvento)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));

        long vagasOcupadas = inscricaoRepository.countByEvento_IdAndStatus(evento.getId(), StatusInscricao.CONFIRMADA);
        int vagasDisponiveis = Math.max(0, evento.getLimiteParticipantes() - (int) vagasOcupadas);

        boolean isInscrito = idEntusiastaLogado != null && inscricaoRepository.existsByEntusiasta_IdAndEvento_IdAndStatus(
                idEntusiastaLogado, evento.getId(), StatusInscricao.CONFIRMADA);

        return montarDTO(evento, vagasDisponiveis, isInscrito);
    }

    private CatalogoEventoResponseDTO montarDTO(CatalogoEventoProjecaoDTO projecao, Map<UUID, Long> vagasOcupadasPorEvento,
                                                Set<UUID> eventosInscritos) {
        long vagasOcupadas = vagasOcupadasPorEvento.getOrDefault(projecao.idEvento(), 0L);
        int vagasDisponiveis = Math.max(0, projecao.limiteParticipantes() - (int) vagasOcupadas);

        return new CatalogoEventoResponseDTO(
                projecao.idEvento(),
                projecao.nome(),
                projecao.descricao(),
                projecao.preco(),
                projecao.rua(),
                projecao.numero(),
                projecao.bairro(),
                projecao.latitude(),
                projecao.longitude(),
                projecao.nomeOrganizador(),
                vagasDisponiveis,
                eventosInscritos.contains(projecao.idEvento()),
                projecao.dataHoraInicio(),
                projecao.dataHoraFim()
        );
    }

    private CatalogoEventoResponseDTO montarDTO(Evento evento, int vagasDisponiveis, boolean isInscrito) {
        return new CatalogoEventoResponseDTO(
                evento.getId(),
                evento.getNome(),
                evento.getDescricao(),
                evento.getPreco(),
                evento.getRua(),
                evento.getNumero(),
                evento.getBairro(),
                evento.getLatitude(),
                evento.getLongitude(),
                evento.getOrganizadorEvento().getNome(),
                vagasDisponiveis,
                isInscrito,
                evento.getDataHoraInicio(),
                evento.getDataHoraFim()
        );
    }
}