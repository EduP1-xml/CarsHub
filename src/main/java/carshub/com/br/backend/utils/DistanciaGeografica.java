package carshub.com.br.backend.utils;

import java.util.Locale;

/**
 * Geometria da busca por proximidade, inteiramente em Java.
 *
 * O banco recebe apenas o pre-filtro barato por caixa de coordenadas; o
 * calculo exato da distancia e o corte do raio ficam aqui. Assim o SQL nao
 * depende de funcao de dialeto (radians/least) nem de function(...) generica,
 * que o Hibernate nao consegue tipar em HQL.
 */
public final class DistanciaGeografica {

    /** Raio medio da Terra (km): o mesmo 6371 da Haversine. */
    private static final double radioTerraKm = 6371.0;

    /** 1 grau de latitude ~= 111,32 km: base do pre-filtro por caixa. */
    private static final double quilometrosPorGrau = 111.32;

    private DistanciaGeografica() {
    }

    /** Caixa alinhada aos eixos que contem todo ponto dentro do raio. */
    public record CaixaCoordenadas(double latitudeMinima, double latitudeMaxima,
                                   double longitudeMinima, double longitudeMaxima) {
    }

    /**
     * Caixa de coordenadas que envolve o circulo do raio em volta da origem.
     * Perto dos polos o grau de longitude encolhe; quando a conta degenera, a
     * caixa abre 180 graus e quem corta o raio de verdade continua sendo o Java.
     */
    public static CaixaCoordenadas caixaDeBusca(double latitude, double longitude, double raioKm) {
        double alcanceLatitude = raioKm / quilometrosPorGrau;
        double grausPorLongitude = quilometrosPorGrau * Math.cos(Math.toRadians(latitude));
        double alcanceLongitude = (grausPorLongitude <= 0.0) ? 180.0 : raioKm / grausPorLongitude;

        return new CaixaCoordenadas(
                Math.max(-90.0, latitude - alcanceLatitude),
                Math.min(90.0, latitude + alcanceLatitude),
                Math.max(-180.0, longitude - alcanceLongitude),
                Math.min(180.0, longitude + alcanceLongitude));
    }

    /**
     * Distancia Haversine em km entre a origem e um destino. Devolve null quando
     * o destino nao tem coordenada (chamado antigo), e quem chama decide o que
     * fazer. O cosseno e limitado a [-1, 1] nos DOIS lados: erro de ponto
     * flutuante fora dessa faixa faria o Math.acos devolver NaN e o chamado
     * desapareceria do painel — era exatamente o risco que o least(1.0, ...)
     * cobria no SQL antigo, so que agora sem depender do banco.
     */
    public static Double distanciaKm(double latitudeOrigem, double longitudeOrigem,
                                     Double latitudeDestino, Double longitudeDestino) {
        if (latitudeDestino == null || longitudeDestino == null) {
            return null;
        }

        double latitudeOrigemRadianos = Math.toRadians(latitudeOrigem);
        double latitudeDestinoRadianos = Math.toRadians(latitudeDestino);
        double deltaLongitudeRadianos = Math.toRadians(longitudeDestino - longitudeOrigem);

        double cossenoDoAnguloCentral =
                Math.cos(latitudeOrigemRadianos) * Math.cos(latitudeDestinoRadianos) * Math.cos(deltaLongitudeRadianos)
                        + Math.sin(latitudeOrigemRadianos) * Math.sin(latitudeDestinoRadianos);

        return radioTerraKm * Math.acos(Math.max(-1.0, Math.min(1.0, cossenoDoAnguloCentral)));
    }

    /**
     * WKT do retangulo da caixa, na mesma convencao da coluna gerada
     * (latitude longitude) e em SRID 0, para o ST_Within usar o indice espacial.
     */
    public static String envelopeWkt(CaixaCoordenadas caixa) {
        return "POLYGON(("
                + ponto(caixa.latitudeMinima(), caixa.longitudeMinima()) + ","
                + ponto(caixa.latitudeMaxima(), caixa.longitudeMinima()) + ","
                + ponto(caixa.latitudeMaxima(), caixa.longitudeMaxima()) + ","
                + ponto(caixa.latitudeMinima(), caixa.longitudeMaxima()) + ","
                + ponto(caixa.latitudeMinima(), caixa.longitudeMinima())
                + "))";
    }

    private static String ponto(double latitude, double longitude) {
        return String.format(Locale.ROOT, "%.7f %.7f", latitude, longitude);
    }
}