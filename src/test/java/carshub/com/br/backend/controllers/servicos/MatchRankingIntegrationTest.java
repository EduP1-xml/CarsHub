package carshub.com.br.backend.controllers.servicos;

import carshub.com.br.backend.models.entities.Carro;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.Mecanico;
import carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados;
import carshub.com.br.backend.models.enums.servicos.StatusServico;
import carshub.com.br.backend.testsupport.TesteIntegracaoBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static carshub.com.br.backend.testsupport.FabricaDeDados.LATITUDE_PADRAO;
import static carshub.com.br.backend.testsupport.FabricaDeDados.LONGITUDE_PADRAO;
import static carshub.com.br.backend.testsupport.FabricaDeDados.cnpjUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.cpfUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.criarCarro;
import static carshub.com.br.backend.testsupport.FabricaDeDados.criarChamadoMecanico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.criarColecionador;
import static carshub.com.br.backend.testsupport.FabricaDeDados.criarMecanico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.emailUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.placaUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.sufixoUnico;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Testes do bloco MATCH (M4/N14): ranking de prestadores compativeis, exclusao
 * de cota cheia, nota neutra 4.0 e limite de 10 propostas por chamado.
 * Banco MySQL real: os dados criados PERMANECEM gravados apos o teste.
 */
class MatchRankingIntegrationTest extends TesteIntegracaoBase {

    private static final String BASE_RANKING = "/api/carshub/colecionador/servicos";

    @Test
    @DisplayName("ranking ordena por notaMatch, exclui cota cheia e prestador sem coordenadas")
    void rankingOrdenaEExclui() throws Exception {
        String sufixo = sufixoUnico();

        // M1: reputacao alta e perto (5,0 x 1 avaliacao, ~1 km)
        Mecanico bemAvaliado = criarMecanico(repositorioMecanico, emailUnico("rk1" + sufixo), cnpjUnico(),
                List.of(ServicosOfertados.TROCA_DE_OLEO), LATITUDE_PADRAO + 0.008, LONGITUDE_PADRAO + 0.008);
        bemAvaliado.setNotaMediaPrestador(5.0);
        bemAvaliado.setQuantidadeAvaliacoes(1L);
        repositorioMecanico.save(bemAvaliado);

        // M2: sem historico (neutro 4.0), mais longe (~4 km)
        Mecanico neutro = criarMecanico(repositorioMecanico, emailUnico("rk2" + sufixo), cnpjUnico(),
                List.of(ServicosOfertados.TROCA_DE_OLEO), LATITUDE_PADRAO + 0.035, LONGITUDE_PADRAO + 0.035);

        // M3: cota cheia (2 PENDENTE semeados) - nao deve aparecer
        Mecanico cotaCheia = criarMecanico(repositorioMecanico, emailUnico("rk3" + sufixo), cnpjUnico(),
                List.of(ServicosOfertados.TROCA_DE_OLEO), LATITUDE_PADRAO + 0.015, LONGITUDE_PADRAO + 0.015);
        Entusiasta donoM3 = criarColecionador(repositorioEntusiasta, emailUnico("rk3d" + sufixo), cpfUnico());
        Carro carroM3 = criarCarro(repositorioCarro, donoM3.getColecionador(), placaUnico());
        criarChamadoMecanico(repositorioServicoMecanico, donoM3, carroM3, cotaCheia, StatusServico.PENDENTE,
                LATITUDE_PADRAO, LONGITUDE_PADRAO);
        criarChamadoMecanico(repositorioServicoMecanico, donoM3, carroM3, cotaCheia, StatusServico.PENDENTE,
                LATITUDE_PADRAO, LONGITUDE_PADRAO);

        // M4: sem coordenadas - nao deve aparecer
        criarMecanico(repositorioMecanico, emailUnico("rk4" + sufixo), cnpjUnico(),
                List.of(ServicosOfertados.TROCA_DE_OLEO), null, null);

        // Chamado do colecionador (congela os elegiveis)
        Entusiasta colecionador = criarColecionador(repositorioEntusiasta, emailUnico("rk0" + sufixo), cpfUnico());
        Carro carro = criarCarro(repositorioCarro, colecionador.getColecionador(), placaUnico());
        MvcResult criado = mockMvc.perform(post("/api/carshub/colecionador/servicos/mecanicos")
                        .header("user-id", colecionador.getId())
                        .contentType("application/json")
                        .content("""
                                {"idCarro":"%s","servicosOfertados":["TROCA_DE_OLEO"],"latitude":%s,"longitude":%s}
                                """.formatted(carro.getId(), LATITUDE_PADRAO, LONGITUDE_PADRAO)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID idServico = UUID.fromString(objectMapper.readTree(criado.getResponse().getContentAsString())
                .get("idServico").asText());

        MvcResult ranking = mockMvc.perform(get(BASE_RANKING + "/{id}/prestadores-compativeis", idServico)
                        .header("user-id", colecionador.getId())
                        .param("tamanho", "1000"))
                .andExpect(status().isOk())
                .andReturn();

        com.fasterxml.jackson.databind.JsonNode conteudo = objectMapper
                .readTree(ranking.getResponse().getContentAsString()).get("conteudo");
        java.util.List<String> nomes = new java.util.ArrayList<>();
        conteudo.forEach(n -> nomes.add(n.get("nomePrestador").asText()));

        org.assertj.core.api.Assertions.assertThat(nomes)
                .contains(bemAvaliado.getNome(), neutro.getNome())
                .doesNotContain(cotaCheia.getNome());
        org.assertj.core.api.Assertions.assertThat(nomes.indexOf(bemAvaliado.getNome()))
                .isLessThan(nomes.indexOf(neutro.getNome()));

        com.fasterxml.jackson.databind.JsonNode melhor = null;
        com.fasterxml.jackson.databind.JsonNode semHistorico = null;
        for (com.fasterxml.jackson.databind.JsonNode n : conteudo) {
            if (n.get("nomePrestador").asText().equals(bemAvaliado.getNome())) { melhor = n; }
            if (n.get("nomePrestador").asText().equals(neutro.getNome())) { semHistorico = n; }
        }
        org.assertj.core.api.Assertions.assertThat(melhor).isNotNull();
        org.assertj.core.api.Assertions.assertThat(melhor.get("notaAjustada").asDouble()).isEqualTo(4.17);
        org.assertj.core.api.Assertions.assertThat(melhor.get("quantidadeAvaliacoes").asLong()).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(semHistorico).isNotNull();
        org.assertj.core.api.Assertions.assertThat(semHistorico.get("notaAjustada").asDouble()).isEqualTo(4.0);
        org.assertj.core.api.Assertions.assertThat(semHistorico.get("quantidadeAvaliacoes").asLong()).isEqualTo(0);

        // Dono errado: 403
        Entusiasta intruso = criarColecionador(repositorioEntusiasta, emailUnico("rki" + sufixo), cpfUnico());
        criarCarro(repositorioCarro, intruso.getColecionador(), placaUnico());
        mockMvc.perform(get(BASE_RANKING + "/{id}/prestadores-compativeis", idServico)
                        .header("user-id", intruso.getId()))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("11a proposta para o mesmo chamado retorna 422 (regra N14)")
    void decimaPrimeiraProposta422() throws Exception {
        String sufixo = sufixoUnico();
        Entusiasta colecionador = criarColecionador(repositorioEntusiasta, emailUnico("n14" + sufixo), cpfUnico());
        Carro carro = criarCarro(repositorioCarro, colecionador.getColecionador(), placaUnico());

        // 11 mecanicos elegiveis (um por proposta, para nao esbarrar no cooldown de 1 min)
        List<Mecanico> mecanicos = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            mecanicos.add(criarMecanico(repositorioMecanico, emailUnico("n14m" + sufixo + "-" + i), cnpjUnico(),
                    List.of(ServicosOfertados.TROCA_DE_OLEO), LATITUDE_PADRAO, LONGITUDE_PADRAO));
        }

        MvcResult criado = mockMvc.perform(post("/api/carshub/colecionador/servicos/mecanicos")
                        .header("user-id", colecionador.getId())
                        .contentType("application/json")
                        .content("""
                                {"idCarro":"%s","servicosOfertados":["TROCA_DE_OLEO"],"latitude":%s,"longitude":%s}
                                """.formatted(carro.getId(), LATITUDE_PADRAO, LONGITUDE_PADRAO)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID idServico = UUID.fromString(objectMapper.readTree(criado.getResponse().getContentAsString())
                .get("idServico").asText());

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(post("/api/carshub/mecanicos/servicos/{id}/proposta", idServico)
                            .header("user-id", mecanicos.get(i).getId())
                            .contentType("application/json")
                            .content("{\"valorProposto\":300.00,\"latitude\":%s,\"longitude\":%s}"
                                    .formatted(LATITUDE_PADRAO, LONGITUDE_PADRAO)))
                    .andExpect(status().isOk());

            // Rejeicao devolve o chamado para a rede; o contador N14 acumula sem decrementar
            mockMvc.perform(patch("/api/carshub/colecionador/servicos/mecanicos/{id}/rejeitar-proposta", idServico)
                            .header("user-id", colecionador.getId()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("SOLICITADO"));
        }

        // A 11a proposta (de outro mecanico) e recusada
        mockMvc.perform(post("/api/carshub/mecanicos/servicos/{id}/proposta", idServico)
                        .header("user-id", mecanicos.get(10).getId())
                        .contentType("application/json")
                        .content("{\"valorProposto\":300.00,\"latitude\":%s,\"longitude\":%s}"
                                .formatted(LATITUDE_PADRAO, LONGITUDE_PADRAO)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$").value(containsString("10 propostas")));
    }

    @Test
    @DisplayName("rejeitar proposta devolve o chamado para SOLICITADO")
    void rejeitarPropostaVoltaParaSolicitado() throws Exception {
        String sufixo = sufixoUnico();
        Entusiasta colecionador = criarColecionador(repositorioEntusiasta, emailUnico("rjp" + sufixo), cpfUnico());
        Carro carro = criarCarro(repositorioCarro, colecionador.getColecionador(), placaUnico());
        Mecanico mecanico = criarMecanico(repositorioMecanico, emailUnico("rjm" + sufixo), cnpjUnico(),
                List.of(ServicosOfertados.TROCA_DE_OLEO), LATITUDE_PADRAO, LONGITUDE_PADRAO);

        MvcResult criado = mockMvc.perform(post("/api/carshub/colecionador/servicos/mecanicos")
                        .header("user-id", colecionador.getId())
                        .contentType("application/json")
                        .content("""
                                {"idCarro":"%s","servicosOfertados":["TROCA_DE_OLEO"],"latitude":%s,"longitude":%s}
                                """.formatted(carro.getId(), LATITUDE_PADRAO, LONGITUDE_PADRAO)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID idServico = UUID.fromString(objectMapper.readTree(criado.getResponse().getContentAsString())
                .get("idServico").asText());

        mockMvc.perform(post("/api/carshub/mecanicos/servicos/{id}/proposta", idServico)
                        .header("user-id", mecanico.getId())
                        .contentType("application/json")
                        .content("{\"valorProposto\":300.00,\"latitude\":%s,\"longitude\":%s}"
                                .formatted(LATITUDE_PADRAO, LONGITUDE_PADRAO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROPOSTA"));

        mockMvc.perform(patch("/api/carshub/colecionador/servicos/mecanicos/{id}/rejeitar-proposta", idServico)
                        .header("user-id", colecionador.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOLICITADO"));
    }
}