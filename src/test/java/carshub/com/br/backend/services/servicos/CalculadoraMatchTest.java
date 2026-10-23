package carshub.com.br.backend.services.servicos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class CalculadoraMatchTest {

    @Test
    @DisplayName("notaAjustada: sem historico parte do neutro 4.0")
    void notaAjustadaSemHistoricoENeutra() {
        assertThat(CalculadoraMatch.notaAjustada(null, null)).isEqualTo(4.0);
        assertThat(CalculadoraMatch.notaAjustada(null, 0L)).isEqualTo(4.0);
    }

    @Test
    @DisplayName("notaAjustada: 1 avaliacao 5 nao vale 5.0 (diluida em direcao ao neutro)")
    void notaAjustadaDiluiAmostraPequena() {
        assertThat(CalculadoraMatch.notaAjustada(5.0, 1L)).isCloseTo(4.17, within(0.01));
    }

    @Test
    @DisplayName("notaAjustada: volume grande se aproxima da media crua")
    void notaAjustadaComVolume() {
        assertThat(CalculadoraMatch.notaAjustada(4.8, 20L)).isCloseTo(4.64, within(0.01));
    }

    @Test
    @DisplayName("notaAjustadaArredondada: devolve 2 casas decimais")
    void notaAjustadaArredondadaDuasCasas() {
        assertThat(CalculadoraMatch.notaAjustadaArredondada(5.0, 1L)).isEqualTo(4.17);
    }

    @Test
    @DisplayName("notaMatch: sem distancia nao existe match")
    void notaMatchSemDistanciaENulo() {
        assertThat(CalculadoraMatch.notaMatch(4.5, 3L, null)).isNull();
    }

    @Test
    @DisplayName("notaMatch: neutro a zero km = 85 (0.6*0.75 + 0.4*1.0)")
    void notaMatchNeutroZeroKm() {
        assertThat(CalculadoraMatch.notaMatch(null, null, 0.0)).isEqualTo(85);
    }

    @Test
    @DisplayName("notaMatch: 25 km corta metade do peso da distancia")
    void notaMatchMetadeDoRaio() {
        assertThat(CalculadoraMatch.notaMatch(null, null, 25.0)).isEqualTo(65);
    }

    @Test
    @DisplayName("notaMatch: no raio maximo (50 km) a distancia nao pontua")
    void notaMatchNoRaioMaximo() {
        assertThat(CalculadoraMatch.notaMatch(null, null, 50.0)).isEqualTo(45);
    }

    @Test
    @DisplayName("notaMatch: alem do raio a distancia zera e nao fica negativa")
    void notaMatchAlemDoRaio() {
        assertThat(CalculadoraMatch.notaMatch(null, null, 100.0)).isEqualTo(45);
    }

    @Test
    @DisplayName("notaMatch: distancia negativa e tratada como zero")
    void notaMatchDistanciaNegativa() {
        assertThat(CalculadoraMatch.notaMatch(null, null, -10.0)).isEqualTo(85);
    }

    @Test
    @DisplayName("notaMatch: nota melhor sobe o score na mesma distancia")
    void notaMatchNotaMelhorSobeScore() {
        int neutro = CalculadoraMatch.notaMatch(null, null, 10.0);
        int experiente = CalculadoraMatch.notaMatch(4.8, 20L, 10.0);
        assertThat(experiente).isGreaterThan(neutro);
    }

    @Test
    @DisplayName("notaMatch: mesma nota, mais perto sobe o score")
    void notaMatchMaisPertoSobeScore() {
        int perto = CalculadoraMatch.notaMatch(4.0, 0L, 2.0);
        int longe = CalculadoraMatch.notaMatch(4.0, 0L, 40.0);
        assertThat(perto).isGreaterThan(longe);
    }
}