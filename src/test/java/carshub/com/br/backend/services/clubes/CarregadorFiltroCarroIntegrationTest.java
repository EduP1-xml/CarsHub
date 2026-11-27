package carshub.com.br.backend.services.clubes;

import carshub.com.br.backend.models.entities.FiltroCarro;
import carshub.com.br.backend.models.enums.carros.*;
import carshub.com.br.backend.repositories.FiltroCarroRepository;
import carshub.com.br.backend.testsupport.TesteIntegracaoBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Carregamento EM LOTE dos filtros (16 consultas fixas): todas as 15 colecoes,
 * faixas e som de varios filtros de uma vez; ids inexistentes sao ignorados.
 * Banco MySQL real: os filtros criados PERMANECEM no banco.
 */
class CarregadorFiltroCarroIntegrationTest extends TesteIntegracaoBase {

    @Autowired
    private FiltroCarroRepository repositorioFiltro;

    @Autowired
    private CarregadorFiltroCarro carregador;

    private FiltroCarro filtroCompleto() {
        FiltroCarro filtro = new FiltroCarro();
        filtro.getMarcas().addAll(List.of("Volkswagen", "Ford"));
        filtro.getModelos().add("Fusca");
        filtro.getCores().add("Azul");
        filtro.getCarrocerias().add(TipoCarroceria.SEDA);
        filtro.getMotores().add(TipoMotor.BOXER);
        filtro.getInjecoes().add(TipoInjecao.CARBURADO);
        filtro.getAlimentacoes().add(TipoAlimentacao.ASPIRADO);
        filtro.getCombustiveis().add(TipoCombustivel.GASOLINA);
        filtro.getCambios().add(TipoCambio.MANUAL);
        filtro.getTracoes().add(TipoTracao.TRASEIRA);
        filtro.getSuspensoes().add(TipoSuspensao.FIXA);
        filtro.getFreios().add(TipoFreio.TAMBOR);
        filtro.getDirecoes().add(TipoDirecao.MECANICA);
        filtro.getRodas().add(TipoRoda.FERRO);
        filtro.getMateriaisRevestimentoInterno().add(TipoMaterialRevestimentoInterno.TECIDO);
        filtro.setAnoMin(1960);
        filtro.setAnoMax(1985);
        filtro.setQuantidadeCilindrosMin(4);
        filtro.setQuantidadeCilindrosMax(4);
        filtro.setLitragemMotorMin(new BigDecimal("1.200"));
        filtro.setLitragemMotorMax(new BigDecimal("1.600"));
        filtro.setAroMin(14);
        filtro.setAroMax(15);
        filtro.setNumeroPortasMin(2);
        filtro.setNumeroPortasMax(4);
        filtro.setNumeroAssentosMin(4);
        filtro.setNumeroAssentosMax(5);
        filtro.setSom(true);
        return filtro;
    }

    @Test
    @DisplayName("monta varios filtros de uma vez com todas as colecoes, faixas e som; ignora id inexistente")
    void carregaEmLote() {
        FiltroCarro completo = repositorioFiltro.saveAndFlush(filtroCompleto());

        FiltroCarro simples = new FiltroCarro();
        simples.getCambios().add(TipoCambio.AUTOMATICO);
        simples = repositorioFiltro.saveAndFlush(simples);

        UUID inexistente = UUID.randomUUID();
        Map<UUID, FiltroCarro> filtros = carregador.carregar(List.of(completo.getId(), simples.getId(), inexistente));

        assertThat(filtros).hasSize(2).doesNotContainKey(inexistente);

        FiltroCarro lido = filtros.get(completo.getId());
        assertThat(lido.getMarcas()).containsExactlyInAnyOrder("Volkswagen", "Ford");
        assertThat(lido.getModelos()).containsExactly("Fusca");
        assertThat(lido.getCores()).containsExactly("Azul");
        assertThat(lido.getCarrocerias()).containsExactly(TipoCarroceria.SEDA);
        assertThat(lido.getMotores()).containsExactly(TipoMotor.BOXER);
        assertThat(lido.getInjecoes()).containsExactly(TipoInjecao.CARBURADO);
        assertThat(lido.getAlimentacoes()).containsExactly(TipoAlimentacao.ASPIRADO);
        assertThat(lido.getCombustiveis()).containsExactly(TipoCombustivel.GASOLINA);
        assertThat(lido.getCambios()).containsExactly(TipoCambio.MANUAL);
        assertThat(lido.getTracoes()).containsExactly(TipoTracao.TRASEIRA);
        assertThat(lido.getSuspensoes()).containsExactly(TipoSuspensao.FIXA);
        assertThat(lido.getFreios()).containsExactly(TipoFreio.TAMBOR);
        assertThat(lido.getDirecoes()).containsExactly(TipoDirecao.MECANICA);
        assertThat(lido.getRodas()).containsExactly(TipoRoda.FERRO);
        assertThat(lido.getMateriaisRevestimentoInterno()).containsExactly(TipoMaterialRevestimentoInterno.TECIDO);
        assertThat(lido.getAnoMin()).isEqualTo(1960);
        assertThat(lido.getAnoMax()).isEqualTo(1985);
        assertThat(lido.getQuantidadeCilindrosMin()).isEqualTo(4);
        assertThat(lido.getLitragemMotorMin()).isEqualByComparingTo("1.2");
        assertThat(lido.getLitragemMotorMax()).isEqualByComparingTo("1.6");
        assertThat(lido.getAroMax()).isEqualTo(15);
        assertThat(lido.getNumeroPortasMin()).isEqualTo(2);
        assertThat(lido.getNumeroAssentosMax()).isEqualTo(5);
        assertThat(lido.getSom()).isTrue();

        FiltroCarro lidoSimples = filtros.get(simples.getId());
        assertThat(lidoSimples.getCambios()).containsExactly(TipoCambio.AUTOMATICO);
        assertThat(lidoSimples.getMarcas()).isEmpty();
        assertThat(lidoSimples.getAnoMin()).isNull();
        assertThat(lidoSimples.getSom()).isNull();
    }

    @Test
    @DisplayName("lista vazia ou nula devolve mapa vazio sem consultar o banco")
    void listaVazia() {
        assertThat(carregador.carregar(List.of())).isEmpty();
        assertThat(carregador.carregar((List<UUID>) null)).isEmpty();
    }
}
