package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.geocodificacao.LocalizacaoGeocodificadaDTO;
import carshub.com.br.backend.models.dtos.servicos.EnderecoOcorrenciaDTO;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.OrganizadorEvento;
import carshub.com.br.backend.testsupport.TesteIntegracaoBase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;

import static carshub.com.br.backend.testsupport.FabricaDeDados.cnpjUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.cpfUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.criarColecionador;
import static carshub.com.br.backend.testsupport.FabricaDeDados.criarOrganizador;
import static carshub.com.br.backend.testsupport.FabricaDeDados.emailUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.sufixoUnico;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Eventos (criacao multipart com geocodificacao mockada), catalogo, inscricoes e
 * dashboard do organizador. Banco MySQL real: os dados PERMANECEM no banco.
 */
class EventoInscricaoIntegrationTest extends TesteIntegracaoBase {

    private static final String BASE_EVENTOS = "/api/carshub/organizadores/eventos";
    private static final String BASE_INSCRICOES = "/api/carshub/entusiastas/inscricoes";

    private OrganizadorEvento organizador;

    @BeforeEach
    void preparar() {
        organizador = criarOrganizador(repositorioOrganizadorEvento, emailUnico("org"), cnpjUnico());
        Mockito.when(servicoGeocodificacao.geocodificarEndereco(anyString()))
                .thenReturn(new LocalizacaoGeocodificadaDTO(-23.5505, -46.6333, "Rua A, 1 - Centro",
                        EnderecoOcorrenciaDTO.vazio()));
    }

    private UUID criarEvento(int limiteParticipantes) throws Exception {
        String corpo = """
                {"nome":"Encontro de Classicos","descricao":"Evento de carros antigos","preco":20.00,
                 "limiteParticipantes":%d,"rua":"Rua A","numero":"1","bairro":"Centro",
                 "cidade":"Sao Paulo","uf":"SP","cep":"01310100",
                 "dataHoraInicio":"2026-12-01T09:00:00","dataHoraFim":"2026-12-01T18:00:00"}
                """.formatted(limiteParticipantes).replace("\n", "");

        MvcResult criado = mockMvc.perform(multipart(BASE_EVENTOS).with(csrfToken())
                        .file(new MockMultipartFile("dados", "", "application/json", corpo.getBytes()))
                        .header("Authorization", tokenDe(organizador)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ATIVO"))
                .andReturn();
        return UUID.fromString(objectMapper.readTree(criado.getResponse().getContentAsString())
                .get("idEvento").asText());
    }

    private Entusiasta criarEntusiasta(String marcador) {
        return criarColecionador(repositorioEntusiasta, emailUnico(marcador), cpfUnico());
    }

    private com.fasterxml.jackson.databind.JsonNode encontrarEventoNo(MvcResult resposta, UUID idEvento)
            throws Exception {
        // AJUSTE 1.1: o catalogo agora devolve o envelope PaginaRespostaDTO.
        for (com.fasterxml.jackson.databind.JsonNode e
                : objectMapper.readTree(resposta.getResponse().getContentAsString()).get("conteudo")) {
            if (e.get("idEvento").asText().equals(idEvento.toString())) {
                return e;
            }
        }
        return null;
    }

    @Test
    @DisplayName("evento criado aparece no catalogo e aceita inscricao, cancelamento e dashboard")
    void fluxoEventoInscricao() throws Exception {
        String sufixo = sufixoUnico();
        UUID idEvento = criarEvento(2);

        MvcResult catalogo = mockMvc.perform(get("/api/carshub/eventos").param("tamanho", "1000"))
                .andExpect(status().isOk())
                .andReturn();
        com.fasterxml.jackson.databind.JsonNode nossoEvento = encontrarEventoNo(catalogo, idEvento);
        org.assertj.core.api.Assertions.assertThat(nossoEvento).isNotNull();
        org.assertj.core.api.Assertions.assertThat(nossoEvento.get("inscrito").asBoolean()).isFalse();
        org.assertj.core.api.Assertions.assertThat(nossoEvento.get("vagasDisponiveis").asInt()).isEqualTo(2);

        Entusiasta entusiasta = criarEntusiasta("ins" + sufixo);
        // INVENTARIO DE ENDPOINTS (item 2): o idEvento agora vem na rota.
        mockMvc.perform(post(BASE_INSCRICOES + "/{idEvento}", idEvento).with(csrfToken())
                        .header("Authorization", tokenDe(entusiasta))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMADA"));

        // Duplicada: 422
        mockMvc.perform(post(BASE_INSCRICOES + "/{idEvento}", idEvento).with(csrfToken())
                        .header("Authorization", tokenDe(entusiasta))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity());

        // Catalogo agora marca inscrito=true
        MvcResult catalogoInscrito = mockMvc.perform(get("/api/carshub/eventos")
                        .header("Authorization", tokenDe(entusiasta)).param("tamanho", "1000"))
                .andExpect(status().isOk())
                .andReturn();
        com.fasterxml.jackson.databind.JsonNode eventoInscrito = encontrarEventoNo(catalogoInscrito, idEvento);
        org.assertj.core.api.Assertions.assertThat(eventoInscrito).isNotNull();
        org.assertj.core.api.Assertions.assertThat(eventoInscrito.get("inscrito").asBoolean()).isTrue();

        // Dashboard do organizador
        mockMvc.perform(get("/api/carshub/organizadores/dashboard/metricas")
                        .header("Authorization", tokenDe(organizador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalParticipantesMobilizados").value(1));

        // Cancelamento devolve a vaga (String pura no corpo)
        MvcResult cancelamento = mockMvc.perform(patch(BASE_INSCRICOES + "/{id}/cancelar",
                        repositorioInscricao.findAll().stream()
                                .filter(i -> i.getEvento().getId().equals(idEvento))
                                .findFirst().orElseThrow().getId())
                        .with(csrfToken())
                        .header("Authorization", tokenDe(entusiasta)))
                .andExpect(status().isOk())
                .andReturn();
        org.assertj.core.api.Assertions.assertThat(cancelamento.getResponse().getContentAsString())
                .contains("cancelada");
    }

    @Test
    @DisplayName("evento lotado rejeita nova inscricao com 422")
    void lotacaoEsgotada422() throws Exception {
        String sufixo = sufixoUnico();
        UUID idEvento = criarEvento(1);

        Entusiasta primeiro = criarEntusiasta("lot1" + sufixo);
        mockMvc.perform(post(BASE_INSCRICOES + "/{idEvento}", idEvento).with(csrfToken())
                        .header("Authorization", tokenDe(primeiro))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isCreated());

        Entusiasta segundo = criarEntusiasta("lot2" + sufixo);
        mockMvc.perform(post(BASE_INSCRICOES + "/{idEvento}", idEvento).with(csrfToken())
                        .header("Authorization", tokenDe(segundo))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$").value(containsString("limite")));
    }

    @Test
    @DisplayName("evento com fim anterior ao inicio retorna 422")
    void datasInvalidas422() throws Exception {
        String corpo = """
                {"nome":"Evento Invalido","descricao":"Datas erradas","preco":0.00,"limiteParticipantes":5,
                 "rua":"Rua A","numero":"1","bairro":"Centro","cidade":"Sao Paulo","uf":"SP","cep":"01310100",
                 "dataHoraInicio":"2026-12-02T09:00:00","dataHoraFim":"2026-12-01T18:00:00"}
                """.replace("\n", "");

        mockMvc.perform(multipart(BASE_EVENTOS).with(csrfToken())
                        .file(new MockMultipartFile("dados", "", "application/json", corpo.getBytes()))
                        .header("Authorization", tokenDe(organizador)))
                .andExpect(status().isUnprocessableEntity());
    }

    // ==================================================================
    // AJUSTE 1.3 (pendencia): minhas inscricoes paginadas no SQL
    // ==================================================================

    @Test
    @DisplayName("minhas inscricoes sem registros devolvem pagina vazia (default tamanho=5)")
    void inscricoesPaginaVazia() throws Exception {
        Entusiasta entusiasta = criarEntusiasta("pgv" + sufixoUnico());

        mockMvc.perform(get(BASE_INSCRICOES).header("Authorization", tokenDe(entusiasta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo.length()").value(0))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(5))
                .andExpect(jsonPath("$.totalElementos").value(0))
                .andExpect(jsonPath("$.totalPaginas").value(0))
                .andExpect(jsonPath("$.ultima").value(true));
    }

    @Test
    @DisplayName("minhas inscricoes paginam com default 5 e borda na segunda pagina")
    void inscricoesPaginadasDefaultEBorda() throws Exception {
        Entusiasta entusiasta = criarEntusiasta("pgb" + sufixoUnico());

        // 6 eventos + 6 inscricoes (a pagina 2 deve trazer apenas 1)
        for (int i = 0; i < 6; i++) {
            UUID idEvento = criarEvento(50);
            mockMvc.perform(post(BASE_INSCRICOES + "/{idEvento}", idEvento).with(csrfToken())
                            .header("Authorization", tokenDe(entusiasta))
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().isCreated());
        }

        // Default: pagina 0, tamanho 5
        mockMvc.perform(get(BASE_INSCRICOES).header("Authorization", tokenDe(entusiasta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo.length()").value(5))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(5))
                .andExpect(jsonPath("$.totalElementos").value(6))
                .andExpect(jsonPath("$.totalPaginas").value(2))
                .andExpect(jsonPath("$.ultima").value(false));

        // Borda: pagina 1 traz o item restante e marca ultima=true
        mockMvc.perform(get(BASE_INSCRICOES)
                        .header("Authorization", tokenDe(entusiasta))
                        .param("pagina", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.pagina").value(1))
                .andExpect(jsonPath("$.ultima").value(true));

        // Pagina alem do fim: conteudo vazio (pagina inexistente)
        mockMvc.perform(get(BASE_INSCRICOES)
                        .header("Authorization", tokenDe(entusiasta))
                        .param("pagina", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo.length()").value(0))
                .andExpect(jsonPath("$.pagina").value(9));
    }

    // ==================================================================
    // F31: PUT so regeocodifica quando o endereco muda; PATCH com BeanValidation
    // ==================================================================

    @Test
    @DisplayName("PUT com endereco igual NAO chama a geocodificacao (mantem as coordenadas)")
    void putComEnderecoIgualNaoRegeocodifica() throws Exception {
        UUID idEvento = criarEvento(10);
        Mockito.clearInvocations(servicoGeocodificacao);

        String corpo = """
                {"nome":"Encontro de Classicos","descricao":"Descricao atualizada","preco":25.00,
                 "limiteParticipantes":10,"rua":"Rua A","numero":"1","bairro":"Centro",
                 "cidade":"Sao Paulo","uf":"SP","cep":"01310100",
                 "dataHoraInicio":"2026-12-01T09:00:00","dataHoraFim":"2026-12-01T18:00:00"}
                """.replace("\n", "");

        mockMvc.perform(multipart(org.springframework.http.HttpMethod.PUT, BASE_EVENTOS + "/{idEvento}", idEvento).with(csrfToken())
                        .file(new MockMultipartFile("dados", "", "application/json", corpo.getBytes()))
                        .header("Authorization", tokenDe(organizador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descricao").value("Descricao atualizada"))
                .andExpect(jsonPath("$.latitude").value(-23.5505));

        // Endereco inalterado: nenhuma chamada nova ao Google
        Mockito.verify(servicoGeocodificacao, Mockito.never()).geocodificarEndereco(anyString());
    }

    @Test
    @DisplayName("PUT com endereco diferente regeocodifica")
    void putComEnderecoDiferenteRegeocodifica() throws Exception {
        UUID idEvento = criarEvento(10);
        Mockito.clearInvocations(servicoGeocodificacao);

        String corpo = """
                {"nome":"Encontro de Classicos","descricao":"Outro lugar","preco":20.00,
                 "limiteParticipantes":10,"rua":"Rua B","numero":"2","bairro":"Pinheiros",
                 "cidade":"Sao Paulo","uf":"SP","cep":"05422000",
                 "dataHoraInicio":"2026-12-01T09:00:00","dataHoraFim":"2026-12-01T18:00:00"}
                """.replace("\n", "");

        mockMvc.perform(multipart(org.springframework.http.HttpMethod.PUT, BASE_EVENTOS + "/{idEvento}", idEvento).with(csrfToken())
                        .file(new MockMultipartFile("dados", "", "application/json", corpo.getBytes()))
                        .header("Authorization", tokenDe(organizador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rua").value("Rua B"));

        Mockito.verify(servicoGeocodificacao).geocodificarEndereco(anyString());
    }

    @Test
    @DisplayName("PATCH valida os campos com BeanValidation (numero com mais de 5 caracteres -> 400)")
    void patchComNumeroLongo400() throws Exception {
        UUID idEvento = criarEvento(10);

        mockMvc.perform(multipart(org.springframework.http.HttpMethod.PATCH, BASE_EVENTOS + "/{idEvento}", idEvento).with(csrfToken())
                        .file(new MockMultipartFile("dados", "", "application/json",
                                "{\"numero\":\"123456\"}".getBytes()))
                        .header("Authorization", tokenDe(organizador)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PATCH valida UF (3 caracteres -> 400)")
    void patchComUfInvalida400() throws Exception {
        UUID idEvento = criarEvento(10);

        mockMvc.perform(multipart(org.springframework.http.HttpMethod.PATCH, BASE_EVENTOS + "/{idEvento}", idEvento).with(csrfToken())
                        .file(new MockMultipartFile("dados", "", "application/json",
                                "{\"uf\":\"SPP\"}".getBytes()))
                        .header("Authorization", tokenDe(organizador)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /eventos/{id} devolve 404 para evento nao-ATIVO (7.6)")
    void detalheDeEventoNaoAtivo404() throws Exception {
        UUID idEvento = criarEvento(10);

        mockMvc.perform(patch(BASE_EVENTOS + "/{idEvento}/status", idEvento).with(csrfToken())
                        .header("Authorization", tokenDe(organizador))
                        .param("novoStatus", "CANCELADO"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/carshub/eventos/{idEvento}", idEvento))
                .andExpect(status().isNotFound());
    }
}