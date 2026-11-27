package carshub.com.br.backend.services.clubes;

import carshub.com.br.backend.models.entities.Carro;
import carshub.com.br.backend.models.entities.FiltroCarro;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Regras de compatibilidade do gosto de um clube — funcoes PURAS, sem estado
 * nem banco, no estilo da CalculadoraMatch (testaveis com teste unitario).
 *
 * CLUBE x EVENTO (caracteristica por caracteristica; compativel = todas passam):
 *   - nao marcada no clube OU no evento -> passa (campo irrelevante);
 *   - enum/string marcados nos dois      -> conjunto do CLUBE contido no do EVENTO
 *                                           (strings: trim + sem diferenciar maiusculas);
 *   - faixa marcada nos dois             -> faixa do CLUBE dentro da do EVENTO
 *                                           (ponta nula = infinita; ponta aberta do clube
 *                                           so cabe se a mesma ponta do evento for aberta);
 *   - som marcado nos dois               -> passa.
 *   Evento sem filtro (null) e compativel com qualquer clube.
 *
 * CLUBE x CARRO (o carro tem UM valor por caracteristica, sempre preenchido):
 *   - nao marcada no clube -> passa;
 *   - enum/string          -> valor do carro esta no conjunto do clube;
 *   - faixa                -> valor do carro dentro da faixa (ponta nula = aberta);
 *   - som marcado          -> carro.som == true.
 */
public final class CompatibilidadeFiltro {

    private record CriterioConjunto(Function<FiltroCarro, Set<?>> doFiltro, Function<Carro, Object> doCarro) {}

    private record CriterioTexto(Function<FiltroCarro, Set<String>> doFiltro, Function<Carro, String> doCarro) {}

    private record CriterioFaixa<T extends Comparable<T>>(Function<FiltroCarro, T> minimo,
                                                          Function<FiltroCarro, T> maximo,
                                                          Function<Carro, T> doCarro) {}

    private static final List<CriterioTexto> textos = List.of(
            new CriterioTexto(FiltroCarro::getMarcas, Carro::getMarca),
            new CriterioTexto(FiltroCarro::getModelos, Carro::getModelo),
            new CriterioTexto(FiltroCarro::getCores, Carro::getCor));

    private static final List<CriterioConjunto> enums = List.of(
            new CriterioConjunto(FiltroCarro::getCarrocerias, Carro::getCarroceria),
            new CriterioConjunto(FiltroCarro::getMotores, Carro::getMotor),
            new CriterioConjunto(FiltroCarro::getInjecoes, Carro::getInjecao),
            new CriterioConjunto(FiltroCarro::getAlimentacoes, Carro::getAlimentacao),
            new CriterioConjunto(FiltroCarro::getCombustiveis, Carro::getCombustivel),
            new CriterioConjunto(FiltroCarro::getCambios, Carro::getCambio),
            new CriterioConjunto(FiltroCarro::getTracoes, Carro::getTracao),
            new CriterioConjunto(FiltroCarro::getSuspensoes, Carro::getSuspensao),
            new CriterioConjunto(FiltroCarro::getFreios, Carro::getFreio),
            new CriterioConjunto(FiltroCarro::getDirecoes, Carro::getDirecao),
            new CriterioConjunto(FiltroCarro::getRodas, Carro::getRoda),
            new CriterioConjunto(FiltroCarro::getMateriaisRevestimentoInterno, Carro::getMaterialRevestimentoInterno));

    private static final List<CriterioFaixa<?>> faixas = List.of(
            new CriterioFaixa<Integer>(FiltroCarro::getAnoMin, FiltroCarro::getAnoMax, Carro::getAno),
            new CriterioFaixa<Integer>(FiltroCarro::getQuantidadeCilindrosMin, FiltroCarro::getQuantidadeCilindrosMax,
                    Carro::getQuantidadeCilindros),
            new CriterioFaixa<BigDecimal>(FiltroCarro::getLitragemMotorMin, FiltroCarro::getLitragemMotorMax,
                    Carro::getLitragemMotor),
            new CriterioFaixa<Integer>(FiltroCarro::getAroMin, FiltroCarro::getAroMax, Carro::getAro),
            new CriterioFaixa<Integer>(FiltroCarro::getNumeroPortasMin, FiltroCarro::getNumeroPortasMax,
                    Carro::getNumeroPortas),
            new CriterioFaixa<Integer>(FiltroCarro::getNumeroAssentosMin, FiltroCarro::getNumeroAssentosMax,
                    Carro::getNumeroAssentos));

    private CompatibilidadeFiltro() {
        // utilitario estatico
    }

    // ==================================================================
    // CLUBE x EVENTO
    // ==================================================================

    public static boolean compativel(FiltroCarro clube, FiltroCarro evento) {
        if (evento == null || clube == null) {
            return true;
        }

        for (CriterioTexto criterio : textos) {
            Set<String> doClube = criterio.doFiltro().apply(clube);
            Set<String> doEvento = criterio.doFiltro().apply(evento);
            if (marcado(doClube) && marcado(doEvento) && !normalizar(doEvento).containsAll(normalizar(doClube))) {
                return false;
            }
        }

        for (CriterioConjunto criterio : enums) {
            Set<?> doClube = criterio.doFiltro().apply(clube);
            Set<?> doEvento = criterio.doFiltro().apply(evento);
            if (marcado(doClube) && marcado(doEvento) && !doEvento.containsAll(doClube)) {
                return false;
            }
        }

        for (CriterioFaixa<?> criterio : faixas) {
            if (!faixaDoClubeCabeNoEvento(criterio, clube, evento)) {
                return false;
            }
        }

        // som marcado nos dois sempre passa (so ha um valor possivel).
        return true;
    }

    private static <T extends Comparable<T>> boolean faixaDoClubeCabeNoEvento(CriterioFaixa<T> criterio,
                                                                            FiltroCarro clube, FiltroCarro evento) {
        T clubeMin = criterio.minimo().apply(clube);
        T clubeMax = criterio.maximo().apply(clube);
        T eventoMin = criterio.minimo().apply(evento);
        T eventoMax = criterio.maximo().apply(evento);

        boolean clubeMarcou = clubeMin != null || clubeMax != null;
        boolean eventoMarcou = eventoMin != null || eventoMax != null;
        if (!clubeMarcou || !eventoMarcou) {
            return true;
        }

        boolean inferiorCabe = eventoMin == null || (clubeMin != null && clubeMin.compareTo(eventoMin) >= 0);
        boolean superiorCabe = eventoMax == null || (clubeMax != null && clubeMax.compareTo(eventoMax) <= 0);
        return inferiorCabe && superiorCabe;
    }

    // ==================================================================
    // CLUBE x CARRO
    // ==================================================================

    public static boolean carroAtende(FiltroCarro clube, Carro carro) {
        if (clube == null) {
            return true;
        }

        for (CriterioTexto criterio : textos) {
            Set<String> doClube = criterio.doFiltro().apply(clube);
            if (marcado(doClube) && !normalizar(doClube).contains(normalizar(criterio.doCarro().apply(carro)))) {
                return false;
            }
        }

        for (CriterioConjunto criterio : enums) {
            Set<?> doClube = criterio.doFiltro().apply(clube);
            if (marcado(doClube) && !doClube.contains(criterio.doCarro().apply(carro))) {
                return false;
            }
        }

        for (CriterioFaixa<?> criterio : faixas) {
            if (!valorDentroDaFaixa(criterio, clube, carro)) {
                return false;
            }
        }

        return !Boolean.TRUE.equals(clube.getSom()) || Boolean.TRUE.equals(carro.getSom());
    }

    private static <T extends Comparable<T>> boolean valorDentroDaFaixa(CriterioFaixa<T> criterio,
                                                                      FiltroCarro clube, Carro carro) {
        T minimo = criterio.minimo().apply(clube);
        T maximo = criterio.maximo().apply(clube);
        if (minimo == null && maximo == null) {
            return true;
        }

        T valor = criterio.doCarro().apply(carro);
        if (valor == null) {
            return false;
        }
        return (minimo == null || valor.compareTo(minimo) >= 0)
                && (maximo == null || valor.compareTo(maximo) <= 0);
    }

    // ==================================================================
    // AUXILIARES
    // ==================================================================

    /** true quando o filtro tem pelo menos uma caracteristica marcada. */
    public static boolean possuiAlgumCriterio(FiltroCarro filtro) {
        if (filtro == null) {
            return false;
        }
        for (CriterioTexto criterio : textos) {
            if (marcado(criterio.doFiltro().apply(filtro))) {
                return true;
            }
        }
        for (CriterioConjunto criterio : enums) {
            if (marcado(criterio.doFiltro().apply(filtro))) {
                return true;
            }
        }
        for (CriterioFaixa<?> criterio : faixas) {
            if (criterio.minimo().apply(filtro) != null || criterio.maximo().apply(filtro) != null) {
                return true;
            }
        }
        return Boolean.TRUE.equals(filtro.getSom());
    }

    private static boolean marcado(Set<?> conjunto) {
        return conjunto != null && !conjunto.isEmpty();
    }

    private static Set<String> normalizar(Set<String> valores) {
        return valores.stream().map(CompatibilidadeFiltro::normalizar).collect(Collectors.toSet());
    }

    private static String normalizar(String valor) {
        return valor == null ? "" : valor.trim().toLowerCase(Locale.ROOT);
    }
}
