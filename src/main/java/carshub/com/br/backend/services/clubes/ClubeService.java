package carshub.com.br.backend.services.clubes;

import carshub.com.br.backend.exceptions.PerfilIncompativelException;
import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.exceptions.RegraDeNegocioException;
import carshub.com.br.backend.exceptions.SolicitacaoDuplicadaException;
import carshub.com.br.backend.models.dtos.PaginaRespostaDTO;
import carshub.com.br.backend.models.dtos.carros.CarroRespostaDTO;
import carshub.com.br.backend.models.dtos.clubes.CarroCompativelDTO;
import carshub.com.br.backend.models.dtos.clubes.CarroVinculadoResumoDTO;
import carshub.com.br.backend.models.dtos.clubes.ClubeRegistroDTO;
import carshub.com.br.backend.models.dtos.clubes.ClubeRespostaDTO;
import carshub.com.br.backend.models.dtos.clubes.EntradaClubeDTO;
import carshub.com.br.backend.models.dtos.clubes.MembroClubeRespostaDTO;
import carshub.com.br.backend.models.dtos.eventos.CatalogoEventoResponseDTO;
import carshub.com.br.backend.models.entities.Carro;
import carshub.com.br.backend.models.entities.Clube;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.FiltroCarro;
import carshub.com.br.backend.models.entities.MembroClube;
import carshub.com.br.backend.models.entities.MembroClubeId;
import carshub.com.br.backend.models.enums.eventos.StatusEvento;
import carshub.com.br.backend.repositories.CarroRepository;
import carshub.com.br.backend.repositories.ClubeRepository;
import carshub.com.br.backend.repositories.EntusiastaRepository;
import carshub.com.br.backend.repositories.EventoRepository;
import carshub.com.br.backend.repositories.MembroClubeRepository;
import carshub.com.br.backend.services.CarroService;
import carshub.com.br.backend.services.CatalogoEventoService;
import carshub.com.br.backend.services.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Clubes de entusiastas: CRUD do clube (admin), entrada livre, saida,
 * remocao de membro, transferencia de administracao, vinculo de carros e as
 * abas Membros / Carros / Eventos (so membros).
 *
 * Regras de compatibilidade: CompatibilidadeFiltro. Filtros de varios
 * clubes/eventos sao carregados EM LOTE pelo CarregadorFiltroCarro.
 *
 * Posse e papel (checklist (c) da SecurityConfigurations):
 *   - administrador -> PerfilIncompativelException (403) para os demais;
 *   - abas do clube  -> so membros (403);
 *   - vinculo        -> carro do proprio usuario (403) e compativel (422).
 */
@Service
@RequiredArgsConstructor
public class ClubeService {

    /** Limite de fotos por clube (validado aqui, como no EventoService). */
    private static final int maximoFotosPorClube = 5;

    private final ClubeRepository repositorioClube;
    private final MembroClubeRepository repositorioMembro;
    private final EntusiastaRepository repositorioEntusiasta;
    private final CarroRepository repositorioCarro;
    private final EventoRepository repositorioEvento;
    private final FileStorageService servicoArmazenamentoArquivo;
    private final CarregadorFiltroCarro carregadorFiltro;
    private final CatalogoEventoService servicoCatalogoEvento;

    // ==================================================================
    // CRUD DO CLUBE
    // ==================================================================

    @Transactional
    public ClubeRespostaDTO criarClube(ClubeRegistroDTO dados, List<MultipartFile> arquivos, UUID idEntusiasta) {
        Entusiasta criador = buscarEntusiasta(idEntusiasta);
        String nome = dados.nome().trim();

        if (repositorioClube.existsByNomeIgnoreCase(nome)) {
            throw new SolicitacaoDuplicadaException("Já existe um clube com esse nome.");
        }
        validarQuantidadeDeFotos(arquivos);
        exigirAlgumCriterio(dados);

        Clube clube = new Clube();
        clube.setNome(nome);
        clube.setDescricao(textoOuNulo(dados.descricao()));
        clube.setDataCriacao(LocalDateTime.now());
        clube.setAdministrador(criador);
        clube.setFiltroCarro(FiltroCarroMapper.novo(dados.filtro()));
        clube.setFotosClube(salvarFotos(arquivos, idEntusiasta));

        Clube clubeSalvo = repositorioClube.save(clube);

        // O criador ja entra como membro (e administrador), com o proprio id de usuario.
        repositorioMembro.save(new MembroClube(clubeSalvo, criador, clubeSalvo.getDataCriacao()));

        return montarRespostas(List.of(clubeSalvo), idEntusiasta, 0).get(0);
    }

    @Transactional(readOnly = true)
    public Page<ClubeRespostaDTO> listarClubes(String busca, UUID idEntusiasta, int pagina, int tamanho) {
        String termo = busca == null || busca.isBlank() ? null : busca.trim();
        Page<Clube> clubes = repositorioClube.buscarPorNome(termo, PaginaRespostaDTO.pageRequest(pagina, tamanho));
        return paginaDeRespostas(clubes, idEntusiasta);
    }

    @Transactional(readOnly = true)
    public Page<ClubeRespostaDTO> listarMeusClubes(UUID idEntusiasta, int pagina, int tamanho) {
        Page<Clube> clubes = repositorioClube.buscarClubesDoEntusiasta(
                idEntusiasta, PaginaRespostaDTO.pageRequest(pagina, tamanho));
        return paginaDeRespostas(clubes, idEntusiasta);
    }

    @Transactional(readOnly = true)
    public ClubeRespostaDTO detalharClube(UUID idClube, UUID idEntusiasta) {
        return montarRespostas(List.of(buscarClube(idClube)), idEntusiasta, 0).get(0);
    }

    /**
     * PUT multipart (so o admin): nome, descricao, filtro e fotos. Depois de
     * aplicar o novo filtro, os carros vinculados que deixaram de atender ao
     * gosto do clube sao desvinculados na mesma transacao.
     */
    @Transactional
    public ClubeRespostaDTO editarClube(UUID idClube, ClubeRegistroDTO dados, List<MultipartFile> arquivos,
                                        UUID idEntusiasta) {
        Clube clube = buscarClube(idClube);
        exigirAdministrador(clube, idEntusiasta);

        String nome = dados.nome().trim();
        if (repositorioClube.existsByNomeIgnoreCaseAndIdNot(nome, idClube)) {
            throw new SolicitacaoDuplicadaException("Já existe um clube com esse nome.");
        }
        validarQuantidadeDeFotos(arquivos);
        exigirAlgumCriterio(dados);

        clube.setNome(nome);
        clube.setDescricao(textoOuNulo(dados.descricao()));
        FiltroCarroMapper.preencher(clube.getFiltroCarro(), dados.filtro());
        substituirFotosSeHouverArquivos(clube, arquivos, dados.fotosClube(), idEntusiasta);

        Clube clubeSalvo = repositorioClube.save(clube);
        int carrosDesvinculados = desvincularCarrosIncompativeis(clubeSalvo);

        return montarRespostas(List.of(clubeSalvo), idEntusiasta, carrosDesvinculados).get(0);
    }

    /** So o admin. A cascata leva membros, vinculos, filtro e fotos. */
    @Transactional
    public void excluirClube(UUID idClube, UUID idEntusiasta) {
        Clube clube = buscarClube(idClube);
        exigirAdministrador(clube, idEntusiasta);
        repositorioClube.delete(clube);
    }

    // ==================================================================
    // MEMBROS
    // ==================================================================

    /**
     * Entrada livre. O colecionador pode ja informar os carros a vincular; se
     * algum falhar na validacao, a entrada inteira e recusada (transacao).
     */
    @Transactional
    public ClubeRespostaDTO entrarNoClube(UUID idClube, EntradaClubeDTO dados, UUID idEntusiasta) {
        Clube clube = buscarClube(idClube);
        Entusiasta entusiasta = buscarEntusiasta(idEntusiasta);

        if (repositorioMembro.existsById(new MembroClubeId(idClube, idEntusiasta))) {
            throw new SolicitacaoDuplicadaException("Você já é membro deste clube.");
        }

        MembroClube membro = new MembroClube(clube, entusiasta, LocalDateTime.now());
        Set<UUID> idsCarros = dados == null || dados.idsCarros() == null ? Set.of() : dados.idsCarros();
        for (UUID idCarro : idsCarros) {
            membro.getCarrosVinculados().add(validarCarroParaVinculo(clube, idCarro, idEntusiasta));
        }
        repositorioMembro.save(membro);

        return montarRespostas(List.of(clube), idEntusiasta, 0).get(0);
    }

    /** O admin nao sai sem antes transferir a administracao ou excluir o clube. */
    @Transactional
    public void sairDoClube(UUID idClube, UUID idEntusiasta) {
        Clube clube = buscarClube(idClube);
        MembroClube membro = repositorioMembro.findById(new MembroClubeId(idClube, idEntusiasta))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Você não é membro deste clube."));

        if (ehAdministrador(clube, idEntusiasta)) {
            throw new RegraDeNegocioException("Transfira a administração para outro membro ou exclua o clube.");
        }
        repositorioMembro.delete(membro);
    }

    /** So o admin; nao pode remover a si mesmo. Os vinculos do membro saem junto. */
    @Transactional
    public void removerMembro(UUID idClube, UUID idMembro, UUID idEntusiasta) {
        Clube clube = buscarClube(idClube);
        exigirAdministrador(clube, idEntusiasta);

        if (idMembro.equals(idEntusiasta)) {
            throw new RegraDeNegocioException("O administrador não pode remover a si mesmo do clube.");
        }
        MembroClube membro = repositorioMembro.findById(new MembroClubeId(idClube, idMembro))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Membro não encontrado neste clube."));
        repositorioMembro.delete(membro);
    }

    /**
     * So o admin atual; o destino precisa ser membro. O @Version do clube barra
     * duas transferencias simultaneas (409). O admin anterior continua membro.
     */
    @Transactional
    public ClubeRespostaDTO transferirAdministracao(UUID idClube, UUID idNovoAdministrador, UUID idEntusiasta) {
        Clube clube = buscarClube(idClube);
        exigirAdministrador(clube, idEntusiasta);

        if (idNovoAdministrador.equals(idEntusiasta)) {
            throw new RegraDeNegocioException("Você já é o administrador deste clube.");
        }
        MembroClube novoAdministrador = repositorioMembro.findById(new MembroClubeId(idClube, idNovoAdministrador))
                .orElseThrow(() -> new RegraDeNegocioException("O novo administrador precisa ser membro do clube."));

        clube.setAdministrador(novoAdministrador.getEntusiasta());
        Clube clubeSalvo = repositorioClube.saveAndFlush(clube);

        return montarRespostas(List.of(clubeSalvo), idEntusiasta, 0).get(0);
    }

    @Transactional(readOnly = true)
    public Page<MembroClubeRespostaDTO> listarMembros(UUID idClube, UUID idEntusiasta, int pagina, int tamanho) {
        Clube clube = buscarClube(idClube);
        exigirMembro(idClube, idEntusiasta);

        Page<MembroClube> membros = repositorioMembro.listarMembros(idClube, PaginaRespostaDTO.pageRequest(pagina, tamanho));
        List<UUID> idsEntusiastas = membros.getContent().stream().map(m -> m.getId().getIdEntusiasta()).toList();

        // Carros vinculados de TODOS os membros da pagina em 1 consulta + fotos em 1 consulta.
        List<Object[]> linhasCarros = idsEntusiastas.isEmpty() ? List.of()
                : repositorioMembro.buscarCarrosVinculadosDosMembros(idClube, idsEntusiastas);
        Map<UUID, String> fotoCapaPorCarro = fotoCapaDosCarros(linhasCarros.stream().map(l -> (UUID) l[1]).toList());

        Map<UUID, List<CarroVinculadoResumoDTO>> carrosPorMembro = new HashMap<>();
        for (Object[] linha : linhasCarros) {
            UUID idCarro = (UUID) linha[1];
            carrosPorMembro.computeIfAbsent((UUID) linha[0], id -> new ArrayList<>())
                    .add(new CarroVinculadoResumoDTO(idCarro, (String) linha[2], (String) linha[3],
                            (Integer) linha[4], (String) linha[5], fotoCapaPorCarro.get(idCarro)));
        }

        UUID idAdministrador = clube.getAdministrador().getId();
        return membros.map(membro -> {
            Entusiasta entusiasta = membro.getEntusiasta();
            return new MembroClubeRespostaDTO(
                    entusiasta.getId(),
                    entusiasta.getNome(),
                    entusiasta.getUrlFotoPerfil(),
                    entusiasta.getId().equals(idAdministrador),
                    membro.getDataEntrada(),
                    carrosPorMembro.getOrDefault(entusiasta.getId(), List.of()));
        });
    }

    // ==================================================================
    // CARROS VINCULADOS
    // ==================================================================

    /** So o proprio membro: carro dele, compativel e ainda nao vinculado. */
    @Transactional
    public void vincularCarro(UUID idClube, UUID idCarro, UUID idEntusiasta) {
        Clube clube = buscarClube(idClube);
        MembroClube membro = buscarMembroOuNegar(idClube, idEntusiasta);
        Carro carro = validarCarroParaVinculo(clube, idCarro, idEntusiasta);

        if (repositorioMembro.carroVinculadoAoClube(idClube, idCarro)) {
            throw new SolicitacaoDuplicadaException("Este carro já está vinculado ao clube.");
        }
        membro.getCarrosVinculados().add(carro);
    }

    @Transactional
    public void desvincularCarro(UUID idClube, UUID idCarro, UUID idEntusiasta) {
        buscarClube(idClube);
        MembroClube membro = buscarMembroOuNegar(idClube, idEntusiasta);

        boolean removido = membro.getCarrosVinculados().removeIf(carro -> carro.getId().equals(idCarro));
        if (!removido) {
            throw new RecursoNaoEncontradoException("Este carro não está vinculado ao clube.");
        }
    }

    /**
     * Carros da garagem do logado que atendem ao gosto do clube, marcando os ja
     * vinculados. Nao exige ser membro: alimenta a selecao no "Entrar".
     */
    @Transactional(readOnly = true)
    public List<CarroCompativelDTO> listarMeusCarrosCompativeis(UUID idClube, UUID idEntusiasta) {
        Clube clube = buscarClube(idClube);
        FiltroCarro filtroDoClube = carregadorFiltro.carregar(idDoFiltro(clube));

        List<Carro> compativeis = repositorioCarro.findAllByColecionador_IdOrderByMarcaAscModeloAsc(idEntusiasta)
                .stream()
                .filter(carro -> CompatibilidadeFiltro.carroAtende(filtroDoClube, carro))
                .toList();
        if (compativeis.isEmpty()) {
            return List.of();
        }

        Set<UUID> vinculados = new HashSet<>(repositorioMembro.buscarIdsCarrosVinculados(idClube, idEntusiasta));
        Map<UUID, String> fotoCapa = fotoCapaDosCarros(compativeis.stream().map(Carro::getId).toList());

        return compativeis.stream()
                .map(carro -> new CarroCompativelDTO(carro.getId(), carro.getMarca(), carro.getModelo(),
                        carro.getAno(), carro.getCor(), fotoCapa.get(carro.getId()),
                        vinculados.contains(carro.getId())))
                .toList();
    }

    /** Aba Carros (so membros): so os carros VINCULADOS, sem placa, com fotos em lote. */
    @Transactional(readOnly = true)
    public Page<CarroRespostaDTO> listarCarrosVinculados(UUID idClube, UUID idEntusiasta, int pagina, int tamanho) {
        buscarClube(idClube);
        exigirMembro(idClube, idEntusiasta);

        var projecoes = repositorioCarro.projetarCarrosVinculadosDoClube(
                idClube, PaginaRespostaDTO.pageRequest(pagina, tamanho));

        Map<UUID, List<String>> fotosPorCarro = new HashMap<>();
        List<UUID> idsCarros = projecoes.getContent().stream().map(p -> p.idCarro()).toList();
        if (!idsCarros.isEmpty()) {
            for (Object[] linha : repositorioCarro.buscarFotosDosCarros(idsCarros)) {
                fotosPorCarro.computeIfAbsent((UUID) linha[0], id -> new ArrayList<>()).add((String) linha[1]);
            }
        }

        return projecoes.map(projecao -> CarroService.mapearProjecaoParaResponse(
                projecao, null, fotosPorCarro.getOrDefault(projecao.idCarro(), List.of())));
    }

    // ==================================================================
    // ABA EVENTOS (opcao A + lote)
    // ==================================================================

    /**
     * 1) candidatos ATIVOS (idEvento, idFiltro), sem entidades;
     * 2) filtros dos candidatos + o do clube EM LOTE (16 consultas fixas);
     * 3) CompatibilidadeFiltro em Java (evento sem filtro ja e compativel);
     * 4) pagina pelos ids compativeis + vagas/inscrito/fotos do catalogo.
     */
    @Transactional(readOnly = true)
    public Page<CatalogoEventoResponseDTO> listarEventosCompativeis(UUID idClube, UUID idEntusiasta,
                                                                    int pagina, int tamanho) {
        Clube clube = buscarClube(idClube);
        exigirMembro(idClube, idEntusiasta);
        PageRequest pageRequest = PaginaRespostaDTO.pageRequest(pagina, tamanho);

        List<Object[]> candidatos = repositorioEvento.listarCandidatosAtivos(StatusEvento.ATIVO);

        UUID idFiltroDoClube = idDoFiltro(clube);
        Set<UUID> idsFiltros = new LinkedHashSet<>();
        idsFiltros.add(idFiltroDoClube);
        for (Object[] candidato : candidatos) {
            if (candidato[1] != null) {
                idsFiltros.add((UUID) candidato[1]);
            }
        }
        Map<UUID, FiltroCarro> filtros = carregadorFiltro.carregar(idsFiltros);
        FiltroCarro filtroDoClube = filtros.get(idFiltroDoClube);

        List<UUID> idsCompativeis = new ArrayList<>();
        for (Object[] candidato : candidatos) {
            FiltroCarro filtroDoEvento = candidato[1] == null ? null : filtros.get((UUID) candidato[1]);
            if (CompatibilidadeFiltro.compativel(filtroDoClube, filtroDoEvento)) {
                idsCompativeis.add((UUID) candidato[0]);
            }
        }

        if (idsCompativeis.isEmpty()) {
            return Page.empty(pageRequest);
        }
        return servicoCatalogoEvento.enriquecerPagina(
                repositorioEvento.projetarCatalogoPorIds(idsCompativeis, pageRequest), idEntusiasta);
    }

    // ==================================================================
    // AUXILIARES
    // ==================================================================

    /**
     * Reavalia todos os carros vinculados do clube com o filtro ATUAL (ja
     * atualizado em memoria) e remove os que nao atendem. removeIf (iterador)
     * em vez de remove(carro): o hashCode do Carro (@Data) nao e estavel.
     */
    private int desvincularCarrosIncompativeis(Clube clube) {
        int desvinculados = 0;
        for (Object[] vinculo : repositorioMembro.buscarVinculosDoClube(clube.getId())) {
            MembroClube membro = (MembroClube) vinculo[0];
            Carro carro = (Carro) vinculo[1];
            if (!CompatibilidadeFiltro.carroAtende(clube.getFiltroCarro(), carro)) {
                membro.getCarrosVinculados().removeIf(vinculado -> vinculado.getId().equals(carro.getId()));
                desvinculados++;
            }
        }
        return desvinculados;
    }

    private Carro validarCarroParaVinculo(Clube clube, UUID idCarro, UUID idEntusiasta) {
        Carro carro = repositorioCarro.findById(idCarro)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carro não encontrado."));

        if (!carro.getColecionador().getId().equals(idEntusiasta)) {
            throw new PerfilIncompativelException("Acesso Negado: Este veículo não pertence à sua coleção.");
        }
        if (!CompatibilidadeFiltro.carroAtende(clube.getFiltroCarro(), carro)) {
            throw new RegraDeNegocioException("Este carro não atende ao gosto do clube.");
        }
        return carro;
    }

    private Page<ClubeRespostaDTO> paginaDeRespostas(Page<Clube> clubes, UUID idEntusiasta) {
        Map<UUID, ClubeRespostaDTO> porId = new HashMap<>();
        for (ClubeRespostaDTO resposta : montarRespostas(clubes.getContent(), idEntusiasta, 0)) {
            porId.put(resposta.idClube(), resposta);
        }
        return clubes.map(clube -> porId.get(clube.getId()));
    }

    /**
     * Monta as respostas de varios clubes com agregados EM LOTE: contagem de
     * membros e de carros (GROUP BY), flag souMembro, fotos e filtros.
     */
    private List<ClubeRespostaDTO> montarRespostas(List<Clube> clubes, UUID idEntusiasta, int carrosDesvinculados) {
        if (clubes.isEmpty()) {
            return List.of();
        }
        List<UUID> ids = clubes.stream().map(Clube::getId).toList();

        Map<UUID, Long> membrosPorClube = contagem(repositorioMembro.contarMembrosPorClubes(ids));
        Map<UUID, Long> carrosPorClube = contagem(repositorioMembro.contarCarrosVinculadosPorClubes(ids));
        Set<UUID> clubesDoUsuario = idEntusiasta == null ? Set.of()
                : new HashSet<>(repositorioMembro.buscarIdsDeClubesDoMembro(idEntusiasta, ids));

        Map<UUID, List<String>> fotosPorClube = new HashMap<>();
        for (Object[] linha : repositorioClube.buscarFotosDosClubes(ids)) {
            fotosPorClube.computeIfAbsent((UUID) linha[0], id -> new ArrayList<>()).add((String) linha[1]);
        }

        Map<UUID, UUID> filtroPorClube = new HashMap<>();
        for (Object[] linha : repositorioClube.buscarIdsDosFiltros(ids)) {
            filtroPorClube.put((UUID) linha[0], (UUID) linha[1]);
        }
        Map<UUID, FiltroCarro> filtros = carregadorFiltro.carregar(filtroPorClube.values());

        return clubes.stream().map(clube -> {
            Entusiasta administrador = clube.getAdministrador();
            return new ClubeRespostaDTO(
                    clube.getId(),
                    clube.getNome(),
                    clube.getDescricao(),
                    administrador.getId(),
                    administrador.getNome(),
                    membrosPorClube.getOrDefault(clube.getId(), 0L),
                    carrosPorClube.getOrDefault(clube.getId(), 0L),
                    clubesDoUsuario.contains(clube.getId()),
                    administrador.getId().equals(idEntusiasta),
                    FiltroCarroMapper.paraDTO(filtros.get(filtroPorClube.get(clube.getId()))),
                    fotosPorClube.getOrDefault(clube.getId(), List.of()),
                    clube.getDataCriacao(),
                    carrosDesvinculados);
        }).toList();
    }

    private static Map<UUID, Long> contagem(List<Object[]> linhas) {
        Map<UUID, Long> resultado = new HashMap<>();
        for (Object[] linha : linhas) {
            resultado.put((UUID) linha[0], (Long) linha[1]);
        }
        return resultado;
    }

    /** Primeira foto nao nula de cada carro (fotos em 1 consulta). */
    private Map<UUID, String> fotoCapaDosCarros(List<UUID> idsCarros) {
        Map<UUID, String> capa = new HashMap<>();
        if (idsCarros.isEmpty()) {
            return capa;
        }
        for (Object[] linha : repositorioCarro.buscarFotosDosCarros(idsCarros)) {
            if (linha[1] != null) {
                capa.putIfAbsent((UUID) linha[0], (String) linha[1]);
            }
        }
        return capa;
    }

    /** Id do filtro do clube sem inicializar o proxy lazy. */
    private static UUID idDoFiltro(Clube clube) {
        return clube.getFiltroCarro().getId();
    }

    private Clube buscarClube(UUID idClube) {
        return repositorioClube.findById(idClube)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Clube não encontrado."));
    }

    private Entusiasta buscarEntusiasta(UUID idEntusiasta) {
        return repositorioEntusiasta.findById(idEntusiasta)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão inválida: Entusiasta não encontrado."));
    }

    private static boolean ehAdministrador(Clube clube, UUID idEntusiasta) {
        return clube.getAdministrador().getId().equals(idEntusiasta);
    }

    private static void exigirAdministrador(Clube clube, UUID idEntusiasta) {
        if (!ehAdministrador(clube, idEntusiasta)) {
            throw new PerfilIncompativelException(
                    "Acesso Negado: apenas o administrador do clube pode executar esta operação.");
        }
    }

    private void exigirMembro(UUID idClube, UUID idEntusiasta) {
        if (!repositorioMembro.existsById(new MembroClubeId(idClube, idEntusiasta))) {
            throw new PerfilIncompativelException("Acesso Negado: apenas membros do clube podem ver esta informação.");
        }
    }

    private MembroClube buscarMembroOuNegar(UUID idClube, UUID idEntusiasta) {
        return repositorioMembro.findById(new MembroClubeId(idClube, idEntusiasta))
                .orElseThrow(() -> new PerfilIncompativelException(
                        "Acesso Negado: apenas membros do clube podem vincular carros."));
    }

    private static void exigirAlgumCriterio(ClubeRegistroDTO dados) {
        if (!FiltroCarroMapper.possuiAlgumCriterio(dados.filtro())) {
            throw new RegraDeNegocioException("O clube precisa de pelo menos uma característica de carro no seu gosto.");
        }
    }

    private static String textoOuNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }

    private static void validarQuantidadeDeFotos(List<MultipartFile> arquivos) {
        if (arquivos != null && arquivos.size() > maximoFotosPorClube) {
            throw new IllegalArgumentException("É permitido enviar no máximo 5 fotos por clube.");
        }
    }

    private List<String> salvarFotos(List<MultipartFile> arquivos, UUID idEntusiasta) {
        List<String> urls = new ArrayList<>();
        if (arquivos != null) {
            for (MultipartFile arquivo : arquivos) {
                if (arquivo != null && !arquivo.isEmpty()) {
                    urls.add(servicoArmazenamentoArquivo.salvarArquivo(arquivo, "entusiasta", idEntusiasta));
                }
            }
        }
        return urls;
    }

    /**
     * Mesmo comportamento do Evento: com arquivos, as fotos sao substituidas;
     * sem arquivos, vale a lista de URLs do JSON (quando informada).
     */
    private void substituirFotosSeHouverArquivos(Clube clube, List<MultipartFile> arquivos, List<String> urlsDoJson,
                                                 UUID idEntusiasta) {
        if (arquivos != null && !arquivos.isEmpty()) {
            clube.setFotosClube(salvarFotos(arquivos, idEntusiasta));
            return;
        }
        if (urlsDoJson != null) {
            if (urlsDoJson.size() > maximoFotosPorClube) {
                throw new IllegalArgumentException("É permitido enviar no máximo 5 fotos por clube.");
            }
            clube.setFotosClube(new ArrayList<>(urlsDoJson));
        }
    }
}
