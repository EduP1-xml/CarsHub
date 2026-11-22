package carshub.com.br.backend.services.seguranca;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SPRINT 6 (11.8): janela deslizante do rate limit. Relogio manual permite
 * avancar o tempo sem dormir.
 */
class ControleRateLimitTest {

    /** Relogio controlado pelo teste. */
    private static final class RelogioManual extends Clock {

        private long millis = 0L;

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis);
        }

        void avancar(long milissegundos) {
            millis += milissegundos;
        }
    }

    @Test
    @DisplayName("aceita ate o limite e bloqueia o pedido seguinte")
    void bloqueiaAposOLimite() {
        RelogioManual relogio = new RelogioManual();
        ControleRateLimit controle = new ControleRateLimit(relogio);

        assertThat(controle.permitir("ip-1", 2, Duration.ofMinutes(1))).isTrue();
        assertThat(controle.permitir("ip-1", 2, Duration.ofMinutes(1))).isTrue();
        assertThat(controle.permitir("ip-1", 2, Duration.ofMinutes(1))).isFalse();
    }

    @Test
    @DisplayName("a janela desliza: eventos antigos saem e o pedido volta a passar")
    void janelaDeslizanteLiberaDepoisDoTempo() {
        RelogioManual relogio = new RelogioManual();
        ControleRateLimit controle = new ControleRateLimit(relogio);

        controle.permitir("ip-2", 1, Duration.ofSeconds(10));
        assertThat(controle.permitir("ip-2", 1, Duration.ofSeconds(10))).isFalse();

        relogio.avancar(10_001L);
        assertThat(controle.permitir("ip-2", 1, Duration.ofSeconds(10))).isTrue();
    }

    @Test
    @DisplayName("chaves diferentes tem janelas independentes")
    void chavesIndependentes() {
        ControleRateLimit controle = new ControleRateLimit(new RelogioManual());

        assertThat(controle.permitir("ip-a", 1, Duration.ofMinutes(1))).isTrue();
        assertThat(controle.permitir("ip-a", 1, Duration.ofMinutes(1))).isFalse();
        assertThat(controle.permitir("ip-b", 1, Duration.ofMinutes(1))).isTrue();
    }

    @Test
    @DisplayName("limpar zera as janelas acumuladas")
    void limparZeraAsJanelas() {
        ControleRateLimit controle = new ControleRateLimit(new RelogioManual());

        controle.permitir("ip-c", 1, Duration.ofMinutes(1));
        assertThat(controle.permitir("ip-c", 1, Duration.ofMinutes(1))).isFalse();

        controle.limpar();
        assertThat(controle.permitir("ip-c", 1, Duration.ofMinutes(1))).isTrue();
    }
}
