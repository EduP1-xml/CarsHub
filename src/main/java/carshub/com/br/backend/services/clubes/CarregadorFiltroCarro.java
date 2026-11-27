package carshub.com.br.backend.services.clubes;

import carshub.com.br.backend.models.entities.FiltroCarro;
import carshub.com.br.backend.repositories.FiltroCarroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;

/**
 * Monta varios FiltroCarro de uma vez, EM LOTE (opcao A + lote): 1 consulta
 * para os escalares + 1 por colecao (15), qualquer que seja o numero de
 * filtros. Os objetos devolvidos sao TRANSITORIOS (new FiltroCarro, fora do
 * contexto de persistencia) e servem apenas para LEITURA/comparacao — nunca
 * devem ser salvos. Ids inexistentes sao ignorados.
 *
 * Listas de ids muito grandes sao divididas em blocos para o "IN" nao crescer
 * sem limite.
 */
@Component
@RequiredArgsConstructor
public class CarregadorFiltroCarro {

    /** Tamanho maximo de cada bloco do "IN :ids". */
    private static final int tamanhoDoBloco = 1000;

    private final FiltroCarroRepository repositorioFiltro;

    private record Colecao(Function<Collection<UUID>, List<Object[]>> consulta,
                           Function<FiltroCarro, Set<?>> destino) {}

    @Transactional(readOnly = true)
    public Map<UUID, FiltroCarro> carregar(Collection<UUID> idsInformados) {
        List<UUID> ids = idsInformados == null ? List.of()
                : new ArrayList<>(new LinkedHashSet<>(idsInformados.stream().filter(Objects::nonNull).toList()));

        Map<UUID, FiltroCarro> filtros = new HashMap<>();
        for (int inicio = 0; inicio < ids.size(); inicio += tamanhoDoBloco) {
            List<UUID> bloco = ids.subList(inicio, Math.min(inicio + tamanhoDoBloco, ids.size()));
            carregarBloco(bloco, filtros);
        }
        return filtros;
    }

    /** Atalho para um unico filtro (null quando o id e nulo ou inexistente). */
    @Transactional(readOnly = true)
    public FiltroCarro carregar(UUID id) {
        return id == null ? null : carregar(List.of(id)).get(id);
    }

    private void carregarBloco(List<UUID> bloco, Map<UUID, FiltroCarro> filtros) {
        for (Object[] linha : repositorioFiltro.buscarEscalares(bloco)) {
            FiltroCarro filtro = new FiltroCarro();
            filtro.setId((UUID) linha[0]);
            filtro.setAnoMin((Integer) linha[1]);
            filtro.setAnoMax((Integer) linha[2]);
            filtro.setQuantidadeCilindrosMin((Integer) linha[3]);
            filtro.setQuantidadeCilindrosMax((Integer) linha[4]);
            filtro.setLitragemMotorMin((BigDecimal) linha[5]);
            filtro.setLitragemMotorMax((BigDecimal) linha[6]);
            filtro.setAroMin((Integer) linha[7]);
            filtro.setAroMax((Integer) linha[8]);
            filtro.setNumeroPortasMin((Integer) linha[9]);
            filtro.setNumeroPortasMax((Integer) linha[10]);
            filtro.setNumeroAssentosMin((Integer) linha[11]);
            filtro.setNumeroAssentosMax((Integer) linha[12]);
            filtro.setSom((Boolean) linha[13]);
            filtros.put(filtro.getId(), filtro);
        }

        if (filtros.isEmpty()) {
            return;
        }

        for (Colecao colecao : colecoes()) {
            for (Object[] linha : colecao.consulta().apply(bloco)) {
                FiltroCarro filtro = filtros.get((UUID) linha[0]);
                if (filtro != null) {
                    adicionar(colecao.destino().apply(filtro), linha[1]);
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void adicionar(Set<?> destino, Object valor) {
        ((Set<Object>) destino).add(valor);
    }

    private List<Colecao> colecoes() {
        return List.of(
                new Colecao(repositorioFiltro::buscarMarcas, FiltroCarro::getMarcas),
                new Colecao(repositorioFiltro::buscarModelos, FiltroCarro::getModelos),
                new Colecao(repositorioFiltro::buscarCores, FiltroCarro::getCores),
                new Colecao(repositorioFiltro::buscarCarrocerias, FiltroCarro::getCarrocerias),
                new Colecao(repositorioFiltro::buscarMotores, FiltroCarro::getMotores),
                new Colecao(repositorioFiltro::buscarInjecoes, FiltroCarro::getInjecoes),
                new Colecao(repositorioFiltro::buscarAlimentacoes, FiltroCarro::getAlimentacoes),
                new Colecao(repositorioFiltro::buscarCombustiveis, FiltroCarro::getCombustiveis),
                new Colecao(repositorioFiltro::buscarCambios, FiltroCarro::getCambios),
                new Colecao(repositorioFiltro::buscarTracoes, FiltroCarro::getTracoes),
                new Colecao(repositorioFiltro::buscarSuspensoes, FiltroCarro::getSuspensoes),
                new Colecao(repositorioFiltro::buscarFreios, FiltroCarro::getFreios),
                new Colecao(repositorioFiltro::buscarDirecoes, FiltroCarro::getDirecoes),
                new Colecao(repositorioFiltro::buscarRodas, FiltroCarro::getRodas),
                new Colecao(repositorioFiltro::buscarMateriaisRevestimentoInterno,
                        FiltroCarro::getMateriaisRevestimentoInterno));
    }
}
