package carshub.com.br.backend.services.servicos;

import carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados;
import carshub.com.br.backend.models.enums.servicos.TipoServico;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class GeradorChaveIdempotenciaTest {

    private final UUID idEntusiasta = UUID.randomUUID();
    private final UUID idCarro = UUID.randomUUID();

    @Test
    @DisplayName("mesma entrada produz a mesma chave SHA-256 (64 hex)")
    void chaveDeterministica() {
        String chave = GeradorChaveIdempotencia.gerar(
                idEntusiasta, idCarro, TipoServico.MECANICO, List.of(ServicosOfertados.TROCA_DE_OLEO));

        assertThat(chave).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(GeradorChaveIdempotencia.gerar(
                idEntusiasta, idCarro, TipoServico.MECANICO, List.of(ServicosOfertados.TROCA_DE_OLEO)))
                .isEqualTo(chave);
    }

    @Test
    @DisplayName("colecionador diferente muda a chave")
    void entusiastaDiferenteMudaChave() {
        String chaveA = GeradorChaveIdempotencia.gerar(idEntusiasta, idCarro, TipoServico.MECANICO, List.of(ServicosOfertados.FREIOS));
        String chaveB = GeradorChaveIdempotencia.gerar(UUID.randomUUID(), idCarro, TipoServico.MECANICO, List.of(ServicosOfertados.FREIOS));
        assertThat(chaveA).isNotEqualTo(chaveB);
    }

    @Test
    @DisplayName("carro diferente muda a chave")
    void carroDiferenteMudaChave() {
        String chaveA = GeradorChaveIdempotencia.gerar(idEntusiasta, idCarro, TipoServico.MECANICO, List.of(ServicosOfertados.FREIOS));
        String chaveB = GeradorChaveIdempotencia.gerar(idEntusiasta, UUID.randomUUID(), TipoServico.MECANICO, List.of(ServicosOfertados.FREIOS));
        assertThat(chaveA).isNotEqualTo(chaveB);
    }

    @Test
    @DisplayName("tipo de servico diferente muda a chave")
    void tipoDiferenteMudaChave() {
        String chaveA = GeradorChaveIdempotencia.gerar(idEntusiasta, idCarro, TipoServico.MECANICO, List.of(ServicosOfertados.FREIOS));
        String chaveB = GeradorChaveIdempotencia.gerar(idEntusiasta, idCarro, TipoServico.GUINCHO, List.of(ServicosOfertados.FREIOS));
        assertThat(chaveA).isNotEqualTo(chaveB);
    }

    @Test
    @DisplayName("ordem dos servicos nao importa (lista e sorteada)")
    void ordemDosServicosNaoImporta() {
        String chaveA = GeradorChaveIdempotencia.gerar(idEntusiasta, idCarro, TipoServico.MECANICO,
                List.of(ServicosOfertados.SUSPENSAO, ServicosOfertados.TROCA_DE_OLEO, ServicosOfertados.FREIOS));
        String chaveB = GeradorChaveIdempotencia.gerar(idEntusiasta, idCarro, TipoServico.MECANICO,
                List.of(ServicosOfertados.FREIOS, ServicosOfertados.SUSPENSAO, ServicosOfertados.TROCA_DE_OLEO));
        assertThat(chaveA).isEqualTo(chaveB);
    }

    @Test
    @DisplayName("lista de servicos diferente muda a chave")
    void listaDiferenteMudaChave() {
        String chaveA = GeradorChaveIdempotencia.gerar(idEntusiasta, idCarro, TipoServico.MECANICO, List.of(ServicosOfertados.FREIOS));
        String chaveB = GeradorChaveIdempotencia.gerar(idEntusiasta, idCarro, TipoServico.MECANICO, List.of(ServicosOfertados.SUSPENSAO));
        assertThat(chaveA).isNotEqualTo(chaveB);
    }

    @Test
    @DisplayName("lista nula nao quebra e continua devolvendo 64 hex")
    void listaNulaNaoQuebra() {
        String chave = GeradorChaveIdempotencia.gerar(idEntusiasta, idCarro, TipoServico.GUINCHO, null);
        assertThat(chave).hasSize(64).matches("[0-9a-f]{64}");
    }
}