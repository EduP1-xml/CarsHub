package carshub.com.br.backend.services.servicos;

/**
 * Motor de score do MATCH, funcoes PURAS e deterministicas, sem estado,
 * para a ordenacao ser sempre testavel e reproduzivel.
 *
 * FORMULAS:
 *   notaAjustada = (somaDasNotas + 4.0 * 5) / (quantidade + 5)     <- bayesiana
 *   notaMatch    = round(100 * (0.6 * notaNormalizada + 0.4 * distanciaNormalizada))
 *
 *   notaNormalizada      = (notaAjustada - 1) / 4                  <- [0..1]
 *   distanciaNormalizada = 1 - min(distanciaKm, 50) / 50           <- [0..1]
 *
 * Prestador sem avaliacoes parte do neutro (4.0) e nao e punido; a reputacao
 * so se distancia do neutro com volume de avaliacoes.
 */
public final class CalculadoraMatch {

    /** Peso da nota ajustada no score (0.6 nota / 0.4 distancia). */
    private static final double pesoNotaAjustada = 0.6;

    private static final double pesoDistancia = 0.4;

    /** Nota "a priori" de quem ainda nao tem avaliacoes (neutra, nao pune). */
    private static final double notaNeutraPrestador = 4.0;

    /** Avaliacoes virtuais equivalentes ao prior (suavizacao bayesiana). */
    private static final long avaliacoesEquivalentes = 5;

    /** Escala de referencia para normalizar a distancia (raio padrao do painel). */
    private static final double raioDeReferenciaKm = 50.0;

    /** Faixa das notas (1..5, validada na entidade). */
    private static final double notaMinima = 1.0;
    private static final double notaMaxima = 5.0;

    private CalculadoraMatch() {
        // utilitario estatico
    }

    /**
     * Media bayesiana: dilui amostras pequenas em direcao ao neutro 4.0.
     * null em notaMedia/quantidade vale "sem avaliacoes".
     */
    public static double notaAjustada(Double notaMedia, Long quantidade) {
        double media = notaMedia == null ? 0.0 : notaMedia;
        long quantidadeSegura = quantidade == null ? 0L : quantidade;

        double soma = media * quantidadeSegura;

        return (soma + notaNeutraPrestador * avaliacoesEquivalentes)
                / (quantidadeSegura + avaliacoesEquivalentes);
    }

    /** Nota ajustada pronta para apresentacao (2 casas decimais). */
    public static Double notaAjustadaArredondada(Double notaMedia, Long quantidade) {
        return arredondar(notaAjustada(notaMedia, quantidade));
    }

    /**
     * Score 0..100 do par (prestador, chamado). Devolve null quando a distancia
     * nao pode ser calculada (prestador sem coordenadas): sem distancia nao ha match.
     */
    public static Integer notaMatch(Double notaMedia, Long quantidade, Double distanciaKm) {
        if (distanciaKm == null) {
            return null;
        }

        double notaNormalizada = (notaAjustada(notaMedia, quantidade) - notaMinima) / (notaMaxima - notaMinima);
        double distanciaNormalizada = 1.0 - Math.min(Math.max(distanciaKm, 0.0), raioDeReferenciaKm) / raioDeReferenciaKm;
        double score = pesoNotaAjustada * notaNormalizada + pesoDistancia * distanciaNormalizada;

        return (int) Math.round(score * 100.0);
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}