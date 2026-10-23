package carshub.com.br.backend.controllers.servicos;

import carshub.com.br.backend.models.entities.Carro;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.Guincho;
import carshub.com.br.backend.models.enums.servicos.StatusServico;
import carshub.com.br.backend.testsupport.TesteIntegracaoBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static carshub.com.br.backend.testsupport.FabricaDeDados.LATITUDE_PADRAO;
import static carshub.com.br.backend.testsupport.FabricaDeDados.LONGITUDE_PADRAO;
import static carshub.com.br.backend.testsupport.FabricaDeDados.cnpjUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.cpfUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.criarCarro;
import static carshub.com.br.backend.testsupport.FabricaDeDados.criarChamadoGuincho;
import static carshub.com.br.backend.testsupport.FabricaDeDados.criarColecionador;
import static carshub.com.br.backend.testsupport.FabricaDeDados.criarGuincho;
import static carshub.com.br.backend.testsupport.FabricaDeDados.emailUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.placaUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.sufixoUnico;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integracao ponta a ponta do fluxo de servicos de GUINCHO.
 * Banco MySQL real: os dados criados PERMANECEM gravados apos o teste.
 */
class ServicoGuinchoIntegrationTest extends TesteIntegracaoBase {

    private static final String BASE_COLECIONADOR = "/api/carshub/colecionador/servicos/guinchos";
    private static final String BASE_GUINCHO = "/api/carshub/guinchos/servicos";

    @Test
    @DisplayName("E2E: chamado de guincho -> proposta -> aceite -> chegada -> avaliacao -> conclusao (reputacao)")
    void fluxoCompletoGuinchoAteFinalizado() throws Exception {
        String sufixo = sufixoUnico();
        Entusiasta colecionador = criarColecionador(repositorioEntusiasta, emailUnico("gcol" + sufixo), cpfUnico());
        Carro carro = criarCarro(repositorioCarro, colecionador.getColecionador(), placaUnico());
        Guincho guincho = criarGuincho(repositorioGuincho, emailUnico("ggui" + sufixo), cnpjUnico(), placaUnico(),
                LATITUDE_PADRAO, LONGITUDE_PADRAO);

        String corpo = """
                {"idCarro":"%s","latitude":%s,"longitude":%s}
                """.formatted(carro.getId(), LATITUDE_PADRAO, LONGITUDE_PADRAO);

        MvcResult criado = mockMvc.perform(post(BASE_COLECIONADOR)
                        .header("user-id", colecionador.getId())
                        .contentType("application/json").content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SOLICITADO"))
                .andExpect(jsonPath("$.servicosSolicitados").isEmpty())
                .andReturn();
        UUID idServico = UUID.fromString(objectMapper.readTree(criado.getResponse().getContentAsString())
                .get("idServico").asText());

        // Painel do guincho: sem notaMatch e sem lista de servicos
        mockMvc.perform(get(BASE_GUINCHO + "/disponiveis")
                        .header("user-id", guincho.getId())
                        .param("latitude", String.valueOf(LATITUDE_PADRAO))
                        .param("longitude", String.valueOf(LONGITUDE_PADRAO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[*].idServico")
                        .value(org.hamcrest.Matchers.hasItem(idServico.toString())))
                .andExpect(jsonPath("$.conteudo[0].notaMatch").doesNotExist())
                .andExpect(jsonPath("$.conteudo[0].servicosSolicitados").isEmpty());

        mockMvc.perform(post(BASE_GUINCHO + "/{id}/proposta", idServico)
                        .header("user-id", guincho.getId())
                        .contentType("application/json")
                        .content("{\"valorProposto\":500.00,\"latitude\":%s,\"longitude\":%s}"
                                .formatted(LATITUDE_PADRAO, LONGITUDE_PADRAO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROPOSTA"));

        mockMvc.perform(patch(BASE_COLECIONADOR + "/{id}/aceitar-proposta", idServico)
                        .header("user-id", colecionador.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"));

        mockMvc.perform(multipart(BASE_COLECIONADOR + "/{id}/foto-inicial", idServico)
                        .file(new MockMultipartFile("arquivo", "foto-inicial.png", "image/png", new byte[]{1, 2}))
                        .header("user-id", colecionador.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(patch(BASE_COLECIONADOR + "/{id}/confirmar-chegada", idServico)
                        .header("user-id", colecionador.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(patch(BASE_GUINCHO + "/{id}/confirmar-chegada", idServico)
                        .header("user-id", guincho.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHEGOU"));

        mockMvc.perform(multipart(BASE_COLECIONADOR + "/{id}/foto-final", idServico)
                        .file(new MockMultipartFile("arquivo", "ff-e.png", "image/png", new byte[]{3}))
                        .header("user-id", colecionador.getId()))
                .andExpect(status().isOk());
        mockMvc.perform(multipart(BASE_GUINCHO + "/{id}/foto-final", idServico)
                        .file(new MockMultipartFile("arquivo", "ff-g.png", "image/png", new byte[]{4}))
                        .header("user-id", guincho.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(post(BASE_COLECIONADOR + "/{id}/avaliacao", idServico)
                        .header("user-id", colecionador.getId())
                        .contentType("application/json").content("{\"nota\":5,\"comentario\":\"Rapido\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post(BASE_GUINCHO + "/{id}/avaliacao", idServico)
                        .header("user-id", guincho.getId())
                        .contentType("application/json").content("{\"nota\":3,\"comentario\":\"Ok\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch(BASE_COLECIONADOR + "/{id}/confirmar-conclusao", idServico)
                        .header("user-id", colecionador.getId()))
                .andExpect(status().isOk());
        mockMvc.perform(patch(BASE_GUINCHO + "/{id}/confirmar-conclusao", idServico)
                        .header("user-id", guincho.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADO"));

        mockMvc.perform(get(BASE_COLECIONADOR + "/{id}", idServico)
                        .header("user-id", colecionador.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.notaMediaPrestador").value(4.17))
                .andExpect(jsonPath("$.quantidadeAvaliacoes").value(1));

        // Historico do veiculo: guincho sem lista de servicos realizados
        mockMvc.perform(get("/api/carshub/colecionador/carros/{idCarro}/historico-manutencoes", carro.getId())
                        .header("user-id", colecionador.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].tipoServico").value("GUINCHO"))
                .andExpect(jsonPath("$.conteudo[0].servicosRealizados").isEmpty());
    }

    @Test
    @DisplayName("solicitacao de guincho identica duplicada retorna 409")
    void solicitacaoDuplicada409() throws Exception {
        String sufixo = sufixoUnico();
        Entusiasta colecionador = criarColecionador(repositorioEntusiasta, emailUnico("gdup" + sufixo), cpfUnico());
        Carro carro = criarCarro(repositorioCarro, colecionador.getColecionador(), placaUnico());

        String corpo = """
                {"idCarro":"%s","latitude":%s,"longitude":%s}
                """.formatted(carro.getId(), LATITUDE_PADRAO, LONGITUDE_PADRAO);

        mockMvc.perform(post(BASE_COLECIONADOR)
                        .header("user-id", colecionador.getId())
                        .contentType("application/json").content(corpo))
                .andExpect(status().isCreated());
        mockMvc.perform(post(BASE_COLECIONADOR)
                        .header("user-id", colecionador.getId())
                        .contentType("application/json").content(corpo))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("guincho com cota cheia (2 ativos) recebe 422 ao propor")
    void propostaComCotaCheia422() throws Exception {
        String sufixo = sufixoUnico();
        Entusiasta dono = criarColecionador(repositorioEntusiasta, emailUnico("gcot" + sufixo), cpfUnico());
        Carro carro = criarCarro(repositorioCarro, dono.getColecionador(), placaUnico());
        Guincho guincho = criarGuincho(repositorioGuincho, emailUnico("gcot" + sufixo), cnpjUnico(), placaUnico(),
                LATITUDE_PADRAO, LONGITUDE_PADRAO);

        // Ocupa a cota do guincho com 2 servicos PENDENTE (semeado direto, sem cooldown)
        criarChamadoGuincho(repositorioServicoGuincho, dono, carro, guincho, StatusServico.PENDENTE,
                LATITUDE_PADRAO, LONGITUDE_PADRAO);
        criarChamadoGuincho(repositorioServicoGuincho, dono, carro, guincho, StatusServico.PENDENTE,
                LATITUDE_PADRAO, LONGITUDE_PADRAO);

        // Novo chamado de outro colecionador
        Entusiasta solicitante = criarColecionador(repositorioEntusiasta, emailUnico("gsol" + sufixo), cpfUnico());
        Carro carroSolicitante = criarCarro(repositorioCarro, solicitante.getColecionador(), placaUnico());
        MvcResult criado = mockMvc.perform(post(BASE_COLECIONADOR)
                        .header("user-id", solicitante.getId())
                        .contentType("application/json")
                        .content("""
                                {"idCarro":"%s","latitude":%s,"longitude":%s}
                                """.formatted(carroSolicitante.getId(), LATITUDE_PADRAO, LONGITUDE_PADRAO)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID idServico = UUID.fromString(objectMapper.readTree(criado.getResponse().getContentAsString())
                .get("idServico").asText());

        mockMvc.perform(post(BASE_GUINCHO + "/{id}/proposta", idServico)
                        .header("user-id", guincho.getId())
                        .contentType("application/json")
                        .content("{\"valorProposto\":400.00,\"latitude\":%s,\"longitude\":%s}"
                                .formatted(LATITUDE_PADRAO, LONGITUDE_PADRAO)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("chamado inexistente retorna 404 na proposta")
    void naoEncontrado404() throws Exception {
        String sufixo = sufixoUnico();
        Entusiasta solicitante = criarColecionador(repositorioEntusiasta, emailUnico("g404" + sufixo), cpfUnico());
        criarCarro(repositorioCarro, solicitante.getColecionador(), placaUnico());

        mockMvc.perform(post(BASE_GUINCHO + "/{id}/proposta", UUID.randomUUID())
                        .header("user-id", solicitante.getId())
                        .contentType("application/json")
                        .content("{\"valorProposto\":400.00}"))
                .andExpect(status().isNotFound());
    }
}