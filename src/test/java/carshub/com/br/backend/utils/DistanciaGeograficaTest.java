package carshub.com.br.backend.utils;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class DistanciaGeograficaTest {

    @Test
    @DisplayName("Haversine: par conhecido de Sao Paulo ~2.65 km")
    void distanciaParConhecido() {
        Double distancia = DistanciaGeografica.distanciaKm(-23.5505, -46.6333, -23.5614, -46.6559);
        assertThat(distancia).isNotNull();
        assertThat(distancia).isCloseTo(2.65, within(0.10));
    }

    @Test
    @DisplayName("Haversine: mesmo ponto = 0 km")
    void distanciaZeroNoMesmoPonto() {
        assertThat(DistanciaGeografica.distanciaKm(-23.55, -46.63, -23.55, -46.63))
                .isCloseTo(0.0, within(0.0001));
    }

    @Test
    @DisplayName("Haversine: destino sem coordenadas devolve null")
    void distanciaSemDestinoENula() {
        assertThat(DistanciaGeografica.distanciaKm(-23.55, -46.63, null, null)).isNull();
        assertThat(DistanciaGeografica.distanciaKm(-23.55, -46.63, -23.55, null)).isNull();
    }

    @Test
    @DisplayName("caixaDeBusca: envolve o circulo do raio")
    void caixaDeBuscaEnvolveCirculo() {
        DistanciaGeografica.CaixaCoordenadas caixa = DistanciaGeografica.caixaDeBusca(-23.55, -46.63, 50.0);
        assertThat(caixa.latitudeMinima()).isLessThan(-23.55);
        assertThat(caixa.latitudeMaxima()).isGreaterThan(-23.55);
        assertThat(caixa.longitudeMinima()).isLessThan(-46.63);
        assertThat(caixa.longitudeMaxima()).isGreaterThan(-46.63);
    }

    @Test
    @DisplayName("caixaDeBusca: raio 0 fecha a caixa no ponto")
    void caixaRaioZero() {
        DistanciaGeografica.CaixaCoordenadas caixa = DistanciaGeografica.caixaDeBusca(-23.55, -46.63, 0.0);
        assertThat(caixa.latitudeMinima()).isEqualTo(-23.55);
        assertThat(caixa.latitudeMaxima()).isEqualTo(-23.55);
        assertThat(caixa.longitudeMinima()).isEqualTo(-46.63);
        assertThat(caixa.longitudeMaxima()).isEqualTo(-46.63);
    }

    @Test
    @DisplayName("caixaDeBusca: no polo a longitude abre 180 graus")
    void caixaNoPoloAbreLongitude() {
        DistanciaGeografica.CaixaCoordenadas caixa = DistanciaGeografica.caixaDeBusca(90.0, 0.0, 50.0);
        assertThat(caixa.longitudeMinima()).isEqualTo(-180.0);
        assertThat(caixa.longitudeMaxima()).isEqualTo(180.0);
        assertThat(caixa.latitudeMaxima()).isEqualTo(90.0);
    }

    @Test
    @DisplayName("envelopeWkt: poligono fechado, 7 casas, convencao latitude longitude")
    void envelopeWktFormato() {
        DistanciaGeografica.CaixaCoordenadas caixa = DistanciaGeografica.caixaDeBusca(-23.55, -46.63, 10.0);
        String wkt = DistanciaGeografica.envelopeWkt(caixa);

        assertThat(wkt).startsWith("POLYGON((").endsWith("))");
        assertThat(wkt).containsPattern("-23\\.\\d{7} -46\\.\\d{7}");

        String[] pontos = wkt.replace("POLYGON((", "").replace("))", "").split(",");
        assertThat(pontos).hasSize(5);
        assertThat(pontos[0]).isEqualTo(pontos[pontos.length - 1]);
    }
}