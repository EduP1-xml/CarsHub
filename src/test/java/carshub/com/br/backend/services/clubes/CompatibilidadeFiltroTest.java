package carshub.com.br.backend.services.clubes;

import carshub.com.br.backend.models.entities.Carro;
import carshub.com.br.backend.models.entities.FiltroCarro;
import carshub.com.br.backend.models.enums.carros.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regras puras de compatibilidade (sem Spring/banco). Exemplos do plano:
 * clube "Fuscas" = marca {Volkswagen}, modelo {Fusca}, carroceria {SEDA}, ano 1960-1985.
 */
class CompatibilidadeFiltroTest {

    private static FiltroCarro clubeFuscas() {
        FiltroCarro filtro = new FiltroCarro();
        filtro.getMarcas().add("Volkswagen");
        filtro.getModelos().add("Fusca");
        filtro.getCarrocerias().add(TipoCarroceria.SEDA);
        filtro.setAnoMin(1960);
        filtro.setAnoMax(1985);
        return filtro;
    }

    private static Carro carro(String marca, String modelo, TipoCarroceria carroceria, int ano) {
        Carro carro = new Carro();
        carro.setMarca(marca);
        carro.setModelo(modelo);
        carro.setCarroceria(carroceria);
        carro.setAno(ano);
        carro.setCor("Azul");
        carro.setQuantidadeCilindros(4);
        carro.setMotor(TipoMotor.BOXER);
        carro.setLitragemMotor(new BigDecimal("1.300"));
        carro.setInjecao(TipoInjecao.CARBURADO);
        carro.setAlimentacao(TipoAlimentacao.ASPIRADO);
        carro.setCombustivel(TipoCombustivel.GASOLINA);
        carro.setCambio(TipoCambio.MANUAL);
        carro.setTracao(TipoTracao.TRASEIRA);
        carro.setSuspensao(TipoSuspensao.FIXA);
        carro.setFreio(TipoFreio.TAMBOR);
        carro.setDirecao(TipoDirecao.MECANICA);
        carro.setRoda(TipoRoda.FERRO);
        carro.setAro(15);
        carro.setNumeroPortas(2);
        carro.setNumeroAssentos(4);
        carro.setMaterialRevestimentoInterno(TipoMaterialRevestimentoInterno.TECIDO);
        carro.setSom(false);
        return carro;
    }

    @Nested
    @DisplayName("Clube x Evento")
    class ClubeEvento {

        @Test
        @DisplayName("evento sem filtro (aberto a todos) e compativel com qualquer clube")
        void eventoSemFiltro() {
            assertThat(CompatibilidadeFiltro.compativel(clubeFuscas(), null)).isTrue();
        }

        @Test
        @DisplayName("caracteristica nao marcada em qualquer lado passa")
        void naoMarcadaPassa() {
            FiltroCarro evento = new FiltroCarro();
            evento.getCambios().add(TipoCambio.AUTOMATICO); // o clube nao marcou cambio
            assertThat(CompatibilidadeFiltro.compativel(clubeFuscas(), evento)).isTrue();
        }

        @Test
        @DisplayName("exemplo: marca {Volkswagen, Ford} + ano 1950-1990 aparece")
        void eventoMaisAmploAparece() {
            FiltroCarro evento = new FiltroCarro();
            evento.getMarcas().addAll(List.of("Volkswagen", "Ford"));
            evento.setAnoMin(1950);
            evento.setAnoMax(1990);
            assertThat(CompatibilidadeFiltro.compativel(clubeFuscas(), evento)).isTrue();
        }

        @Test
        @DisplayName("enum: conjunto do clube contido no do evento passa; o inverso falha")
        void enumContido() {
            FiltroCarro evento = new FiltroCarro();
            evento.getCarrocerias().addAll(List.of(TipoCarroceria.SEDA, TipoCarroceria.CUPE));
            assertThat(CompatibilidadeFiltro.compativel(clubeFuscas(), evento)).isTrue();

            FiltroCarro clubeAmplo = new FiltroCarro();
            clubeAmplo.getCarrocerias().addAll(List.of(TipoCarroceria.SEDA, TipoCarroceria.CUPE));
            FiltroCarro eventoRestrito = new FiltroCarro();
            eventoRestrito.getCarrocerias().add(TipoCarroceria.CUPE);
            assertThat(CompatibilidadeFiltro.compativel(clubeAmplo, eventoRestrito)).isFalse();
        }

        @Test
        @DisplayName("string: lista do clube contida na do evento, sem diferenciar maiusculas")
        void stringContidaCaseInsensitive() {
            FiltroCarro evento = new FiltroCarro();
            evento.getMarcas().add("  VOLKSWAGEN ");
            assertThat(CompatibilidadeFiltro.compativel(clubeFuscas(), evento)).isTrue();

            FiltroCarro clubeDuasMarcas = new FiltroCarro();
            clubeDuasMarcas.getMarcas().addAll(List.of("Volkswagen", "Ford"));
            assertThat(CompatibilidadeFiltro.compativel(clubeDuasMarcas, evento)).isFalse();
        }

        @Test
        @DisplayName("exemplo: modelo {Gol} nao aparece")
        void modeloDiferente() {
            FiltroCarro evento = new FiltroCarro();
            evento.getModelos().add("Gol");
            assertThat(CompatibilidadeFiltro.compativel(clubeFuscas(), evento)).isFalse();
        }

        @Test
        @DisplayName("faixa: clube dentro do evento passa; parcialmente fora falha (exemplo 1970-1980)")
        void faixaDoClubeDentroDoEvento() {
            FiltroCarro eventoAmplo = new FiltroCarro();
            eventoAmplo.setAnoMin(1960);
            eventoAmplo.setAnoMax(1985); // limites iguais tambem cabem
            assertThat(CompatibilidadeFiltro.compativel(clubeFuscas(), eventoAmplo)).isTrue();

            FiltroCarro eventoEstreito = new FiltroCarro();
            eventoEstreito.setAnoMin(1970);
            eventoEstreito.setAnoMax(1980);
            assertThat(CompatibilidadeFiltro.compativel(clubeFuscas(), eventoEstreito)).isFalse();
        }

        @Test
        @DisplayName("faixa: pontas abertas")
        void pontasAbertas() {
            FiltroCarro eventoSoMinimo = new FiltroCarro();
            eventoSoMinimo.setAnoMin(1950); // ">= 1950"
            assertThat(CompatibilidadeFiltro.compativel(clubeFuscas(), eventoSoMinimo)).isTrue();

            // Clube com ponta aberta so cabe se o evento tambem tiver a mesma ponta aberta.
            FiltroCarro clubeAte1985 = new FiltroCarro();
            clubeAte1985.setAnoMax(1985);
            FiltroCarro evento1950a1990 = new FiltroCarro();
            evento1950a1990.setAnoMin(1950);
            evento1950a1990.setAnoMax(1990);
            assertThat(CompatibilidadeFiltro.compativel(clubeAte1985, evento1950a1990)).isFalse();

            FiltroCarro eventoAte1990 = new FiltroCarro();
            eventoAte1990.setAnoMax(1990);
            assertThat(CompatibilidadeFiltro.compativel(clubeAte1985, eventoAte1990)).isTrue();
        }

        @Test
        @DisplayName("faixa decimal (litragem) usa compareTo")
        void faixaDecimal() {
            FiltroCarro clube = new FiltroCarro();
            clube.setLitragemMotorMin(new BigDecimal("1.0"));
            clube.setLitragemMotorMax(new BigDecimal("2.000"));
            FiltroCarro evento = new FiltroCarro();
            evento.setLitragemMotorMin(new BigDecimal("1.000"));
            evento.setLitragemMotorMax(new BigDecimal("2.0"));
            assertThat(CompatibilidadeFiltro.compativel(clube, evento)).isTrue();
        }

        @Test
        @DisplayName("som marcado nos dois passa; marcado so no clube passa (evento nao marcou)")
        void som() {
            FiltroCarro clube = new FiltroCarro();
            clube.setSom(true);
            FiltroCarro evento = new FiltroCarro();
            evento.setSom(true);
            assertThat(CompatibilidadeFiltro.compativel(clube, evento)).isTrue();
            assertThat(CompatibilidadeFiltro.compativel(clube, new FiltroCarro())).isTrue();
        }
    }

    @Nested
    @DisplayName("Clube x Carro")
    class ClubeCarro {

        @Test
        @DisplayName("exemplos: Fusca 1972 vincula; Fusca 1990 e Gol 1980 nao")
        void exemplosDoPlano() {
            assertThat(CompatibilidadeFiltro.carroAtende(clubeFuscas(),
                    carro("Volkswagen", "Fusca", TipoCarroceria.SEDA, 1972))).isTrue();
            assertThat(CompatibilidadeFiltro.carroAtende(clubeFuscas(),
                    carro("Volkswagen", "Fusca", TipoCarroceria.SEDA, 1990))).isFalse();
            assertThat(CompatibilidadeFiltro.carroAtende(clubeFuscas(),
                    carro("Volkswagen", "Gol", TipoCarroceria.SEDA, 1980))).isFalse();
        }

        @Test
        @DisplayName("enum: valor do carro no conjunto passa, fora falha")
        void enumNoConjunto() {
            assertThat(CompatibilidadeFiltro.carroAtende(clubeFuscas(),
                    carro("Volkswagen", "Fusca", TipoCarroceria.HATCH, 1972))).isFalse();
        }

        @Test
        @DisplayName("string: comparacao sem diferenciar maiusculas e espacos")
        void stringCaseInsensitive() {
            assertThat(CompatibilidadeFiltro.carroAtende(clubeFuscas(),
                    carro(" volkswagen", "FUSCA ", TipoCarroceria.SEDA, 1972))).isTrue();
        }

        @Test
        @DisplayName("faixa: limites inclusivos e ponta aberta")
        void faixaLimites() {
            assertThat(CompatibilidadeFiltro.carroAtende(clubeFuscas(),
                    carro("Volkswagen", "Fusca", TipoCarroceria.SEDA, 1960))).isTrue();
            assertThat(CompatibilidadeFiltro.carroAtende(clubeFuscas(),
                    carro("Volkswagen", "Fusca", TipoCarroceria.SEDA, 1985))).isTrue();
            assertThat(CompatibilidadeFiltro.carroAtende(clubeFuscas(),
                    carro("Volkswagen", "Fusca", TipoCarroceria.SEDA, 1959))).isFalse();

            FiltroCarro aPartirDe2000 = new FiltroCarro();
            aPartirDe2000.setAnoMin(2000);
            assertThat(CompatibilidadeFiltro.carroAtende(aPartirDe2000,
                    carro("Fiat", "Uno", TipoCarroceria.HATCH, 2020))).isTrue();
        }

        @Test
        @DisplayName("som exigido: carro sem som falha, com som passa")
        void somExigido() {
            FiltroCarro clubeDeSom = new FiltroCarro();
            clubeDeSom.setSom(true);
            Carro semSom = carro("Fiat", "Uno", TipoCarroceria.HATCH, 2000);
            assertThat(CompatibilidadeFiltro.carroAtende(clubeDeSom, semSom)).isFalse();

            semSom.setSom(true);
            assertThat(CompatibilidadeFiltro.carroAtende(clubeDeSom, semSom)).isTrue();
        }

        @Test
        @DisplayName("caracteristica nao marcada no clube passa")
        void naoMarcadaPassa() {
            FiltroCarro soCambio = new FiltroCarro();
            soCambio.getCambios().add(TipoCambio.MANUAL);
            assertThat(CompatibilidadeFiltro.carroAtende(soCambio,
                    carro("Qualquer", "Um", TipoCarroceria.PICAPE, 1999))).isTrue();
        }
    }

    @Test
    @DisplayName("possuiAlgumCriterio: vazio = false; qualquer criterio (inclusive som) = true")
    void possuiAlgumCriterio() {
        assertThat(CompatibilidadeFiltro.possuiAlgumCriterio(new FiltroCarro())).isFalse();
        assertThat(CompatibilidadeFiltro.possuiAlgumCriterio(null)).isFalse();

        FiltroCarro soSom = new FiltroCarro();
        soSom.setSom(true);
        assertThat(CompatibilidadeFiltro.possuiAlgumCriterio(soSom)).isTrue();

        FiltroCarro soAroMax = new FiltroCarro();
        soAroMax.setAroMax(17);
        assertThat(CompatibilidadeFiltro.possuiAlgumCriterio(soAroMax)).isTrue();
    }
}
