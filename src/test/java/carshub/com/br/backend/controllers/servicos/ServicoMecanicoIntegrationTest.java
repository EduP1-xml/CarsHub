package carshub.com.br.backend.controllers.servicos;

import carshub.com.br.backend.models.entities.Carro;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.Mecanico;
import carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados;
import carshub.com.br.backend.models.enums.servicos.StatusServico;
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
import static carshub.com.br.backend.testsupport.FabricaDeDados.criarChamadoMecanico;
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
        MvcResult criado = mockMvc.perform(post(BASE_COLECIONADOR).with(csrfToken())
                        .header("Authorization", tokenDe(colecionador))
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
        mockMvc.perform(post(BASE_MECANICO + "/{id}/proposta", idServico).with(csrfToken())
                        .header("Authorization", tokenDe(mecanico))
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

        // Painel do prestador: chamado visivel, ordenado por distancia e COM notaMatch (ajuste 1.5)
        mockMvc.perform(get(BASE_MECANICO + "/disponiveis")
                        .header("Authorization", tokenDe(mecanico))
                        .param("latitude", String.valueOf(LATITUDE_PADRAO))
                        .param("longitude", String.valueOf(LONGITUDE_PADRAO)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[*].idServico")
                        .value(org.hamcrest.Matchers.hasItem(idServico.toString())))
                .andExpect(jsonPath("$.conteudo[0].notaMatch").isNumber())
                .andExpect(jsonPath("$.conteudo[0].nomeEntusiasta").isNotEmpty())
                .andExpect(jsonPath("$.conteudo[0].distanciaKm").isNumber())
                .andExpect(jsonPath("$.conteudo[0].carroDescricao").isNotEmpty());

                // Proposta do mecanico: SOLICITADO -> PROPOSTA
        String proposta = """
                {"valorProposto":350.00,"latitude":%s,"longitude":%s}
                """.formatted(LATITUDE_PADRAO, LONGITUDE_PADRAO);
        mockMvc.perform(post(BASE_MECANICO + "/{id}/proposta", idServico).with(csrfToken())
                        .header("Authorization", tokenDe(mecanico))
                        .contentType("application/json").content(proposta))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PROPOSTA"));

        // Aceite: PROPOSTA -> PENDENTE (telefones liberados)
        mockMvc.perform(patch(BASE_COLECIONADOR + "/{id}/aceitar-proposta", idServico).with(csrfToken())
                        .header("Authorization", tokenDe(colecionador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.telefonePrestador").isNotEmpty());

        // Foto inicial (colecionador) e chegada dos dois lados
        mockMvc.perform(multipart(BASE_COLECIONADOR + "/{id}/foto-inicial", idServico).with(csrfToken())
                        .file(new MockMultipartFile("arquivo", "foto-inicial.png", "image/png", new byte[]{1, 2, 3, 4}))
                        .header("Authorization", tokenDe(colecionador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.urlFotoInicial").isNotEmpty());

        mockMvc.perform(patch(BASE_COLECIONADOR + "/{id}/confirmar-chegada", idServico).with(csrfToken())
                        .header("Authorization", tokenDe(colecionador)))
                .andExpect(status().isOk());

        mockMvc.perform(patch(BASE_MECANICO + "/{id}/confirmar-chegada", idServico).with(csrfToken())
                        .header("Authorization", tokenDe(mecanico)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHEGOU"));

        // Fotos finais e avaliacoes dos dois lados
        mockMvc.perform(multipart(BASE_COLECIONADOR + "/{id}/foto-final", idServico).with(csrfToken())
                        .file(new MockMultipartFile("arquivo", "foto-final-e.png", "image/png", new byte[]{5, 6}))
                        .header("Authorization", tokenDe(colecionador)))
                .andExpect(status().isOk());

        mockMvc.perform(multipart(BASE_MECANICO + "/{id}/foto-final", idServico).with(csrfToken())
                        .file(new MockMultipartFile("arquivo", "foto-final-m.png", "image/png", new byte[]{7, 8}))
                        .header("Authorization", tokenDe(mecanico)))
                .andExpect(status().isOk());

        mockMvc.perform(post(BASE_COLECIONADOR + "/{id}/avaliacao", idServico).with(csrfToken())
                        .header("Authorization", tokenDe(colecionador))
                        .contentType("application/json")
                        .content("{\"nota\":5,\"comentario\":\"Excelente\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post(BASE_MECANICO + "/{id}/avaliacao", idServico).with(csrfToken())
                        .header("Authorization", tokenDe(mecanico))
                        .contentType("application/json")
                        .content("{\"nota\":4,\"comentario\":\"Otimo colecionador\"}"))
                .andExpect(status().isOk());

        // Conclusao dos dois lados -> FINALIZADO
        mockMvc.perform(patch(BASE_COLECIONADOR + "/{id}/confirmar-conclusao", idServico).with(csrfToken())
                        .header("Authorization", tokenDe(colecionador)))
                .andExpect(status().isOk());

        mockMvc.perform(patch(BASE_MECANICO + "/{id}/confirmar-conclusao", idServico).with(csrfToken())
                        .header("Authorization", tokenDe(mecanico)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADO"));

        // Reputacao M1: atualizada na conclusao; notaMediaPrestador = ajustada (bayesiana)
        mockMvc.perform(get(BASE_COLECIONADOR + "/{id}", idServico)
                        .header("Authorization", tokenDe(colecionador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FINALIZADO"))
                .andExpect(jsonPath("$.notaMediaPrestador").value(4.17))
                .andExpect(jsonPath("$.quantidadeAvaliacoes").value(1))
                .andExpect(jsonPath("$.notaMatch").isNumber())
                .andExpect(jsonPath("$.servicosAtivosDoPrestador").value(0));

        // Historico de manutencao do veiculo contem o servico finalizado
        mockMvc.perform(get("/api/carshub/colecionador/carros/{idCarro}/historico-manutencoes", carro.getId())
                        .header("Authorization", tokenDe(colecionador)))
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
        mockMvc.perform(post(BASE_COLECIONADOR).with(csrfToken())
                        .header("Authorization", tokenDe(colecionador))
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

        mockMvc.perform(post(BASE_MECANICO + "/{id}/proposta", idServico).with(csrfToken())
                        .header("Authorization", tokenDe(mecanico))
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
                        .with(csrfToken())
                        .header("Authorization", tokenDe(cenario.colecionador())))
                .andExpect(status().isOk());

        mockMvc.perform(delete(BASE_COLECIONADOR + "/{id}", cenario.idServico())
                        .with(csrfToken())
                        .header("Authorization", tokenDe(cenario.colecionador())))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("foto inicial antes do aceite retorna 422")
    void fotoInicialForaDoEstado422() throws Exception {
        String sufixo = sufixoUnico();
        Entusiasta colecionador = criarColecionador(repositorioEntusiasta, emailUnico("fot" + sufixo), cpfUnico());
        Carro carro = criarCarro(repositorioCarro, colecionador.getColecionador(), placaUnico());

        UUID idServico = abrirChamado(colecionador, carro);

        mockMvc.perform(multipart(BASE_COLECIONADOR + "/{id}/foto-inicial", idServico).with(csrfToken())
                        .file(new MockMultipartFile("arquivo", "foto.png", "image/png", new byte[]{1}))
                        .header("Authorization", tokenDe(colecionador)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("buscar chamado de outro usuario retorna 403 e inexistente retorna 404")
    void acessoNegadoENaoEncontrado() throws Exception {
        Cenario cenario = prepararChamadoEmProposta(sufixoUnico());

        Entusiasta outro = criarColecionador(repositorioEntusiasta, emailUnico("int" + sufixoUnico()), cpfUnico());
        criarCarro(repositorioCarro, outro.getColecionador(), placaUnico());

        mockMvc.perform(get(BASE_COLECIONADOR + "/{id}", cenario.idServico())
                        .header("Authorization", tokenDe(outro)))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(BASE_COLECIONADOR + "/{id}", UUID.randomUUID())
                        .header("Authorization", tokenDe(cenario.colecionador())))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("pedido sem token retorna 401 (seguranca fail-closed)")
    void headerUsuarioAusente401() throws Exception {
        mockMvc.perform(post(BASE_COLECIONADOR).with(csrfToken())
                        .contentType("application/json")
                        .content("{\"idCarro\":\"00000000-0000-0000-0000-000000000000\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("painel consolidado do colecionador devolve envelope paginado (AJUSTE 1.3)")
    void painelConsolidadoEnvelope() throws Exception {
        String sufixo = sufixoUnico();
        Entusiasta colecionador = criarColecionador(repositorioEntusiasta, emailUnico("pnc" + sufixo), cpfUnico());
        Carro carro = criarCarro(repositorioCarro, colecionador.getColecionador(), placaUnico());

        abrirChamado(colecionador, carro);

        mockMvc.perform(get("/api/carshub/colecionador/servicos")
                        .header("Authorization", tokenDe(colecionador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(5))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(1))
                .andExpect(jsonPath("$.ultima").value(true));
    }

    @Test
    @DisplayName("fila de trabalho do mecanico ordenada por prioridade de status (F37)")
    void filaOrdenadaPorPrioridadeDeStatus() throws Exception {
        String sufixo = sufixoUnico();
        Entusiasta dono = criarColecionador(repositorioEntusiasta, emailUnico("f37d" + sufixo), cpfUnico());
        Carro carro = criarCarro(repositorioCarro, dono.getColecionador(), placaUnico());
        Mecanico mecanico = criarMecanico(repositorioMecanico, emailUnico("f37m" + sufixo), cnpjUnico(),
                List.of(ServicosOfertados.TROCA_DE_OLEO), LATITUDE_PADRAO, LONGITUDE_PADRAO);

        // Semeia um chamado em cada status (a ordem de criacao e proposital: a mais
        // antiga e FINALIZADO, a mais nova e PROPOSTA — a prioridade DEVE vencer a data)
        criarChamadoMecanico(repositorioServicoMecanico, dono, carro, mecanico, StatusServico.FINALIZADO,
                LATITUDE_PADRAO, LONGITUDE_PADRAO);
        criarChamadoMecanico(repositorioServicoMecanico, dono, carro, mecanico, StatusServico.PROPOSTA,
                LATITUDE_PADRAO, LONGITUDE_PADRAO);
        criarChamadoMecanico(repositorioServicoMecanico, dono, carro, mecanico, StatusServico.CHEGOU,
                LATITUDE_PADRAO, LONGITUDE_PADRAO);
        criarChamadoMecanico(repositorioServicoMecanico, dono, carro, mecanico, StatusServico.PENDENTE,
                LATITUDE_PADRAO, LONGITUDE_PADRAO);

        mockMvc.perform(get(BASE_MECANICO)
                        .header("Authorization", tokenDe(mecanico))
                        .param("tamanho", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].status").value("PENDENTE"))
                .andExpect(jsonPath("$.conteudo[1].status").value("CHEGOU"))
                .andExpect(jsonPath("$.conteudo[2].status").value("PROPOSTA"))
                .andExpect(jsonPath("$.conteudo[3].status").value("FINALIZADO"));
    }
}