package carshub.com.br.backend.services.clubes;

import carshub.com.br.backend.exceptions.RegraDeNegocioException;
import carshub.com.br.backend.models.dtos.clubes.FiltroCarroDTO;
import carshub.com.br.backend.models.entities.FiltroCarro;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Conversao FiltroCarroDTO <-> FiltroCarro.
 *
 * Normalizacao das strings: trim, remove vazias e remove duplicadas sem
 * diferenciar maiusculas NEM acentos (mantem a primeira grafia). Os acentos
 * entram na deduplicacao porque a collation do MySQL (utf8mb4_0900_ai_ci) os
 * ignora na chave das tabelas filhas: "Citroen" e "Citroën" no mesmo filtro
 * colidiriam no banco.
 *
 * Faixa invertida (min > max) -> RegraDeNegocioException (422).
 */
public final class FiltroCarroMapper {

    private FiltroCarroMapper() {
        // utilitario estatico
    }

    /** Novo FiltroCarro a partir do DTO (null -> filtro vazio). */
    public static FiltroCarro novo(FiltroCarroDTO dto) {
        FiltroCarro filtro = new FiltroCarro();
        preencher(filtro, dto);
        return filtro;
    }

    /** Substitui o conteudo inteiro do filtro pelo do DTO (null -> esvazia). */
    public static void preencher(FiltroCarro filtro, FiltroCarroDTO dto) {
        FiltroCarroDTO dados = dto != null ? dto : vazio();

        validarFaixa("ano", dados.anoMin(), dados.anoMax());
        validarFaixa("quantidade de cilindros", dados.quantidadeCilindrosMin(), dados.quantidadeCilindrosMax());
        validarFaixa("litragem do motor", dados.litragemMotorMin(), dados.litragemMotorMax());
        validarFaixa("aro", dados.aroMin(), dados.aroMax());
        validarFaixa("número de portas", dados.numeroPortasMin(), dados.numeroPortasMax());
        validarFaixa("número de assentos", dados.numeroAssentosMin(), dados.numeroAssentosMax());

        substituir(filtro.getMarcas(), normalizarTextos(dados.marcas()));
        substituir(filtro.getModelos(), normalizarTextos(dados.modelos()));
        substituir(filtro.getCores(), normalizarTextos(dados.cores()));

        substituir(filtro.getCarrocerias(), dados.carrocerias());
        substituir(filtro.getMotores(), dados.motores());
        substituir(filtro.getInjecoes(), dados.injecoes());
        substituir(filtro.getAlimentacoes(), dados.alimentacoes());
        substituir(filtro.getCombustiveis(), dados.combustiveis());
        substituir(filtro.getCambios(), dados.cambios());
        substituir(filtro.getTracoes(), dados.tracoes());
        substituir(filtro.getSuspensoes(), dados.suspensoes());
        substituir(filtro.getFreios(), dados.freios());
        substituir(filtro.getDirecoes(), dados.direcoes());
        substituir(filtro.getRodas(), dados.rodas());
        substituir(filtro.getMateriaisRevestimentoInterno(), dados.materiaisRevestimentoInterno());

        filtro.setAnoMin(dados.anoMin());
        filtro.setAnoMax(dados.anoMax());
        filtro.setQuantidadeCilindrosMin(dados.quantidadeCilindrosMin());
        filtro.setQuantidadeCilindrosMax(dados.quantidadeCilindrosMax());
        filtro.setLitragemMotorMin(dados.litragemMotorMin());
        filtro.setLitragemMotorMax(dados.litragemMotorMax());
        filtro.setAroMin(dados.aroMin());
        filtro.setAroMax(dados.aroMax());
        filtro.setNumeroPortasMin(dados.numeroPortasMin());
        filtro.setNumeroPortasMax(dados.numeroPortasMax());
        filtro.setNumeroAssentosMin(dados.numeroAssentosMin());
        filtro.setNumeroAssentosMax(dados.numeroAssentosMax());

        // som: so "true" e criterio; false/null = nao marcado.
        filtro.setSom(Boolean.TRUE.equals(dados.som()) ? Boolean.TRUE : null);
    }

    /** true quando o DTO tem pelo menos uma caracteristica marcada (depois da normalizacao). */
    public static boolean possuiAlgumCriterio(FiltroCarroDTO dto) {
        return dto != null && CompatibilidadeFiltro.possuiAlgumCriterio(novo(dto));
    }

    /** Entidade -> DTO de resposta (listas ordenadas; null -> null). */
    public static FiltroCarroDTO paraDTO(FiltroCarro filtro) {
        if (filtro == null) {
            return null;
        }
        return new FiltroCarroDTO(
                ordenarTextos(filtro.getMarcas()),
                ordenarTextos(filtro.getModelos()),
                ordenarTextos(filtro.getCores()),
                new TreeSet<>(filtro.getCarrocerias()),
                new TreeSet<>(filtro.getMotores()),
                new TreeSet<>(filtro.getInjecoes()),
                new TreeSet<>(filtro.getAlimentacoes()),
                new TreeSet<>(filtro.getCombustiveis()),
                new TreeSet<>(filtro.getCambios()),
                new TreeSet<>(filtro.getTracoes()),
                new TreeSet<>(filtro.getSuspensoes()),
                new TreeSet<>(filtro.getFreios()),
                new TreeSet<>(filtro.getDirecoes()),
                new TreeSet<>(filtro.getRodas()),
                new TreeSet<>(filtro.getMateriaisRevestimentoInterno()),
                filtro.getAnoMin(), filtro.getAnoMax(),
                filtro.getQuantidadeCilindrosMin(), filtro.getQuantidadeCilindrosMax(),
                filtro.getLitragemMotorMin(), filtro.getLitragemMotorMax(),
                filtro.getAroMin(), filtro.getAroMax(),
                filtro.getNumeroPortasMin(), filtro.getNumeroPortasMax(),
                filtro.getNumeroAssentosMin(), filtro.getNumeroAssentosMax(),
                filtro.getSom());
    }

    // ==================================================================
    // AUXILIARES
    // ==================================================================

    private static FiltroCarroDTO vazio() {
        return new FiltroCarroDTO(null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private static <T extends Comparable<T>> void validarFaixa(String nome, T minimo, T maximo) {
        if (minimo != null && maximo != null && minimo.compareTo(maximo) > 0) {
            throw new RegraDeNegocioException(
                    "A faixa de " + nome + " está invertida: o mínimo não pode ser maior que o máximo.");
        }
    }

    private static <T> void substituir(Set<T> destino, Collection<T> origem) {
        destino.clear();
        if (origem != null) {
            origem.stream().filter(java.util.Objects::nonNull).forEach(destino::add);
        }
    }

    private static List<String> normalizarTextos(List<String> valores) {
        if (valores == null) {
            return List.of();
        }
        Map<String, String> unicos = new LinkedHashMap<>();
        for (String valor : valores) {
            if (valor == null || valor.isBlank()) {
                continue;
            }
            String limpo = valor.trim();
            unicos.putIfAbsent(chaveDeComparacao(limpo), limpo);
        }
        return new ArrayList<>(unicos.values());
    }

    private static String chaveDeComparacao(String valor) {
        String semAcento = Normalizer.normalize(valor, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT);
    }

    private static List<String> ordenarTextos(Set<String> valores) {
        return valores.stream().sorted(Comparator.comparing(v -> v.toLowerCase(Locale.ROOT))).toList();
    }
}
