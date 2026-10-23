package carshub.com.br.backend.controllers.servicos;

import carshub.com.br.backend.models.entities.Carro;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.Mecanico;
import carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados;
import carshub.com.br.backend.testsupport.TesteIntegracaoBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static carshub.com.br.backend.testsupport.FabricaDeDados.LATITUDE_PADRAO;
import static carshub.com.br.backend.testsupport.FabricaDeDados.LONGITUDE_PADRAO;
import static carshub.com.br.backend.testsupport.FabricaDeDados.cnpjUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.cpfUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.criarCarro;
import static carshub.com.br.backend.testsupport.FabricaDeDados.criarColecionador;
import static carshub.com.br.backend.testsupport.FabricaDeDados.criarMecanico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.emailUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.placaUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.sufixoUnico;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Teste de integracao ponta a ponta do fluxo de servicos de MECANICO.
 * Banco MySQL real: tudo o que e criado aqui PERMANECE gravado apos o teste.
 */
class ServicoMecanicoIntegrationTest extends TesteIntegracaoBase {

    private static final String BASE_COLECIONADOR = "/api/carshub/colecionador/servicos/mecanicos";
    private static final String BASE_MECANICO = "/api/carshub/mecanicos/servicos";

    private record Cenario(Entusiasta colecionador, Carro carro, Mecanico mecanico, UUID idServico) {
    }

    private UUID abrirChamado(Entusiasta colecionador, Carro carro) throws Exception {
        String corpo = """
                {"idCarro":"%s","servicosOfertados":["TROCA_DE_OLEO"],"latitude":%s,"longitude":%s}
                """.formatted(carro.getId(), LATITUDE_PADRAO, LONGITUDE_PADRAO);
        MvcResult criado = mockMvc.perform(post(BASE_COLECIONADOR)
                        .header("user-id", colecionador.getId())
                        .contentType("application/json").content(corpo))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SOLICITADO"))
                .andReturn();
        return UUID.fromString(objectMapper.readTree(criado.getResponse().getContentAsString())
                .get("idServico").asText());
    }

    private Cenario prepararChamadoEmProposta(String sufixo) throws Exception {
        Entusiasta colecionador = criarColecionador(repositorioEntusiasta, emailUnico("col" + sufixo), cpfUnico());
        Carro carro = criarCarro(repositorioCarro, colecionador.getColecionador(), placaUnico());
        Mecanico mecanico = criarMecanico(repositorioMecanico, emailUnico("mec" + sufixo), cnpjUnico(),
                List.of(ServicosOfertados.TROCA_DE_OLEO), LATITUDE_PADRAO, LONGITUDE_PADRAO);

        UUID idServico = abrirChamado(colecionador, carro);

        String proposta = """
                {"valorProposto":350.00,"latitude":%s,"longitude":%s}
                """.formatted(LATITUDE_PADRAO, LONGITUDE_PADRAO);
        mockMvc.perform(post(BASE_MECANICO + "/{id}/proposta", idServico)
                        .header("user-id", mecanico.getId())
                        .contentType("application/json").content(proposta))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROPOSTA"));

        return new Cenario(colecionador, carro, mecanico, idServico);
    }

    @Test
    @DisplayName("E2E: chamado -> painel -> proposta -> aceite -> fotos -> chegada -> avaliacao -> conclusao")
    void fluxoCompletoAteFinalizado() throws Exception {
                String sufixo = sufixoUnico();
        Entusiasta colecionador = criarColecionador(repositorioEntusiasta, emailUnico("col" + sufixo), cpfUnico());
        Carro carro = criarCarro(repositorioCarro, colecionador.getColecionador(), placaUnico());
        Mecanico mecanico = criarMecanico(repositorioMecanico, emailUnico("mec" + sufixo), cnpjUnico(),
                List.of(ServicosOfertados.TROCA_DE_OLEO), LATITUDE_PADRAO, LONGITUDE_PADRAO);

        UUID idServico = abrirChamado(colecionador, carro);

        // Painel do prestador: chamado visivel, ordenado por distancia e SEM notaMatch
        mockMvc.perform(get(BASE_MECANICO + "/disponiveis")
                        .header("user-id", mecanico.getId())
                        .param("latitude", String.valueOf(LATITUDE_PADRAO))
                        .param("longitude", String.valueOf(LONGITUDE_PADRAO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[*].idServico")
                        .value(org.hamcrest.Matchers.hasItem(idServico.toString())))
                .andExpect(jsonPath("$.conteudo[0].notaMatch").doesNotExist())
                .andExpect(jsonPath("$.conteudo[0].nomeEntusiasta").isNotEmpty())
                .andExpect(jsonPath("$.conteudo[0].distanciaKm").isNumber())
                .andExpect(jsonPath("$.conteudo[0].carroDescricao").isNotEmpty());

                // Proposta do mecanico: SOLICITADO -> PROPOSTA
        String proposta = """
                {"valorProposto":350.00,"latitude":%s,"longitude":%s}
                """.formatted(LATITUDE_PADRAO, LONGITUDE_PADRAO);
        mockMvc.perform(post(BASE_MECANICO + "/{id}/proposta", idServico)
                        .header("user-id", mecanico.getId())
                        .contentType("application/json").content(proposta))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROPOSTA"));

        // Aceite: PROPOSTA -> PENDENTE (telefones liberados)
        mockMvc.perform(patch(BASE_COLECIONADOR + "/{id}/aceitar-proposta", idServico)
                        .header("user-id", colecionador.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.telefonePrestador").isNotEmpty());

        // Foto inicial (colecionador) e chegada dos dois lados
        mockMvc.perform(multipart(BASE_COLECIONADOR + "/{id}/foto-inicial", idServico)
                        .file(new MockMultipartFile("arquivo", "foto-inicial.png", "image/png", new byte[]{1, 2, 3, 4}))
                        .header("user-id", colecionador.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urlFotoInicial").isNotEmpty());

        mockMvc.perform(patch(BASE_COLECIONADOR + "/{id}/confirmar-chegada", idServico)
                        .header("user-id", colecionador.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(patch(BASE_MECANICO + "/{id}/confirmar-chegada", idServico)
                        .header("user-id", mecanico.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHEGOU"));

        // Fotos finais e avaliacoes dos dois lados
        mockMvc.perform(multipart(BASE_COLECIONADOR + "/{id}/foto-final", idServico)
                        .file(new MockMultipartFile("arquivo", "foto-final-e.png", "image/png", new byte[]{5, 6}))
                        .header("user-id", colecionador.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(multipart(BASE_MECANICO + "/{id}/foto-final", idServico)
                        .file(new MockMultipartFile("arquivo", "foto-final-m.png", "image/png", new byte[]{7, 8}))
                        .header("user-id", mecanico.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(post(BASE_COLECIONADOR + "/{id}/avaliacao", idServico)
                        .header("user-id", colecionador.getId())
                        .contentType("application/json")
                        .content("{\"nota\":5,\"comentario\":\"Excelente\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post(BASE_MECANICO + "/{id}/avaliacao", idServico)
                        .header("user-id", mecanico.getId())
                        .contentType("application/json")
                        .content("{\"nota\":4,\"comentario\":\"Otimo colecionador\"}"))
                .andExpect(status().isOk());

        // Conclusao dos dois lados -> FINALIZADO
        mockMvc.perform(patch(BASE_COLECIONADOR + "/{id}/confirmar-conclusao", idServico)
                        .header("user-id", colecionador.getId()))
                .andExpect(status().isOk());

        mockMvc.perform(patch(BASE_MECANICO + "/{id}/confirmar-conclusao", idServico)
                        .header("user-id", mecanico.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADO"));

        // Reputacao M1: atualizada na conclusao; notaMediaPrestador = ajustada (bayesiana)
        mockMvc.perform(get(BASE_COLECIONADOR + "/{id}", idServico)
                        .header("user-id", colecionador.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADO"))
                .andExpect(jsonPath("$.notaMediaPrestador").value(4.17))
                .andExpect(jsonPath("$.quantidadeAvaliacoes").value(1))
                .andExpect(jsonPath("$.notaMatch").isNumber())
                .andExpect(jsonPath("$.servicosAtivosDoPrestador").value(0));

        // Historico de manutencao do veiculo contem o servico finalizado
        mockMvc.perform(get("/api/carshub/colecionador/carros/{idCarro}/historico-manutencoes", carro.getId())
                        .header("user-id", colecionador.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.conteudo[0].idServico").value(idServico.toString()))
                .andExpect(jsonPath("$.conteudo[0].tipoServico").value("MECANICO"));
    }

    @Test
    @DisplayName("solicitacao identica duplicada retorna 409")
    void solicitacaoDuplicada409() throws Exception {
        String sufixo = sufixoUnico();
        Entusiasta colecionador = criarColecionador(repositorioEntusiasta, emailUnico("dup" + sufixo), cpfUnico());
        Carro carro = criarCarro(repositorioCarro, colecionador.getColecionador(), placaUnico());

        abrirChamado(colecionador, carro);

        String corpo = """
                {"idCarro":"%s","servicosOfertados":["TROCA_DE_OLEO"],"latitude":%s,"longitude":%s}
                """.formatted(carro.getId(), LATITUDE_PADRAO, LONGITUDE_PADRAO);
        mockMvc.perform(post(BASE_COLECIONADOR)
                        .header("user-id", colecionador.getId())
                        .contentType("application/json").content(corpo))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("mecanico que nao oferta todos os servicos recebe 422 na proposta")
    void propostaDeNaoElegivel422() throws Exception {
        String sufixo = sufixoUnico();
        Entusiasta colecionador = criarColecionador(repositorioEntusiasta, emailUnico("ine" + sufixo), cpfUnico());
        Carro carro = criarCarro(repositorioCarro, colecionador.getColecionador(), placaUnico());
        Mecanico mecanico = criarMecanico(repositorioMecanico, emailUnico("ine" + sufixo), cnpjUnico(),
                List.of(ServicosOfertados.FREIOS), LATITUDE_PADRAO, LONGITUDE_PADRAO);

        UUID idServico = abrirChamado(colecionador, carro);

        mockMvc.perform(post(BASE_MECANICO + "/{id}/proposta", idServico)
                        .header("user-id", mecanico.getId())
                        .contentType("application/json")
                        .content("{\"valorProposto\":200.00,\"latitude\":%s,\"longitude\":%s}"
                                .formatted(LATITUDE_PADRAO, LONGITUDE_PADRAO)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("cancelar PENDENTE antes de 2 horas retorna 422")
    void cancelarPendenteAntesDeDuasHoras422() throws Exception {
        Cenario cenario = prepararChamadoEmProposta(sufixoUnico());

        mockMvc.perform(patch(BASE_COLECIONADOR + "/{id}/aceitar-proposta", cenario.idServico())
                        .header("user-id", cenario.colecionador().getId()))
                .andExpect(status().isOk());

        mockMvc.perform(delete(BASE_COLECIONADOR + "/{id}", cenario.idServico())
                        .header("user-id", cenario.colecionador().getId()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("foto inicial antes do aceite retorna 422")
    void fotoInicialForaDoEstado422() throws Exception {
        String sufixo = sufixoUnico();
        Entusiasta colecionador = criarColecionador(repositorioEntusiasta, emailUnico("fot" + sufixo), cpfUnico());
        Carro carro = criarCarro(repositorioCarro, colecionador.getColecionador(), placaUnico());

        UUID idServico = abrirChamado(colecionador, carro);

        mockMvc.perform(multipart(BASE_COLECIONADOR + "/{id}/foto-inicial", idServico)
                        .file(new MockMultipartFile("arquivo", "foto.png", "image/png", new byte[]{1}))
                        .header("user-id", colecionador.getId()))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("buscar chamado de outro usuario retorna 403 e inexistente retorna 404")
    void acessoNegadoENaoEncontrado() throws Exception {
        Cenario cenario = prepararChamadoEmProposta(sufixoUnico());

        Entusiasta outro = criarColecionador(repositorioEntusiasta, emailUnico("int" + sufixoUnico()), cpfUnico());
        criarCarro(repositorioCarro, outro.getColecionador(), placaUnico());

        mockMvc.perform(get(BASE_COLECIONADOR + "/{id}", cenario.idServico())
                        .header("user-id", outro.getId()))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_COLECIONADOR + "/{id}", UUID.randomUUID())
                        .header("user-id", cenario.colecionador().getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("pedido sem header user-id retorna 400")
    void headerUsuarioAusente400() throws Exception {
        mockMvc.perform(post(BASE_COLECIONADOR)
                        .contentType("application/json")
                        .content("{\"idCarro\":\"00000000-0000-0000-0000-000000000000\"}"))
                .andExpect(status().isBadRequest());
    }
}