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
                 "dataHoraInicio":"2026-12-01T09:00:00","dataHoraFim":"2026-12-01T18:00:00"}
                """.formatted(limiteParticipantes).replace("\n", "");

        MvcResult criado = mockMvc.perform(multipart(BASE_EVENTOS)
                        .file(new MockMultipartFile("dados", "", "application/json", corpo.getBytes()))
                        .header("user-id", organizador.getId()))
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
        for (com.fasterxml.jackson.databind.JsonNode e
                : objectMapper.readTree(resposta.getResponse().getContentAsString())) {
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

        MvcResult catalogo = mockMvc.perform(get("/api/carshub/eventos"))
                .andExpect(status().isOk())
                .andReturn();
        com.fasterxml.jackson.databind.JsonNode nossoEvento = encontrarEventoNo(catalogo, idEvento);
        org.assertj.core.api.Assertions.assertThat(nossoEvento).isNotNull();
        org.assertj.core.api.Assertions.assertThat(nossoEvento.get("inscrito").asBoolean()).isFalse();
        org.assertj.core.api.Assertions.assertThat(nossoEvento.get("vagasDisponiveis").asInt()).isEqualTo(2);

        Entusiasta entusiasta = criarEntusiasta("ins" + sufixo);
        mockMvc.perform(post(BASE_INSCRICOES)
                        .header("user-id", entusiasta.getId())
                        .contentType("application/json")
                        .content("{\"idEvento\":\"%s\"}".formatted(idEvento)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMADA"));

        // Duplicada: 422
        mockMvc.perform(post(BASE_INSCRICOES)
                        .header("user-id", entusiasta.getId())
                        .contentType("application/json")
                        .content("{\"idEvento\":\"%s\"}".formatted(idEvento)))
                .andExpect(status().isUnprocessableEntity());

        // Catalogo agora marca inscrito=true
        MvcResult catalogoInscrito = mockMvc.perform(get("/api/carshub/eventos").header("user-id", entusiasta.getId()))
                .andExpect(status().isOk())
                .andReturn();
        com.fasterxml.jackson.databind.JsonNode eventoInscrito = encontrarEventoNo(catalogoInscrito, idEvento);
        org.assertj.core.api.Assertions.assertThat(eventoInscrito).isNotNull();
        org.assertj.core.api.Assertions.assertThat(eventoInscrito.get("inscrito").asBoolean()).isTrue();

        // Dashboard do organizador
        mockMvc.perform(get("/api/carshub/organizadores/dashboard/metricas")
                        .header("user-id", organizador.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalParticipantesMobilizados").value(1));

        // Cancelamento devolve a vaga (String pura no corpo)
        MvcResult cancelamento = mockMvc.perform(patch(BASE_INSCRICOES + "/{id}/cancelar",
                        repositorioInscricao.findAll().stream()
                                .filter(i -> i.getEvento().getId().equals(idEvento))
                                .findFirst().orElseThrow().getId())
                        .header("user-id", entusiasta.getId()))
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
        mockMvc.perform(post(BASE_INSCRICOES)
                        .header("user-id", primeiro.getId())
                        .contentType("application/json")
                        .content("{\"idEvento\":\"%s\"}".formatted(idEvento)))
                .andExpect(status().isCreated());

        Entusiasta segundo = criarEntusiasta("lot2" + sufixo);
        mockMvc.perform(post(BASE_INSCRICOES)
                        .header("user-id", segundo.getId())
                        .contentType("application/json")
                        .content("{\"idEvento\":\"%s\"}".formatted(idEvento)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$").value(containsString("limite")));
    }

    @Test
    @DisplayName("evento com fim anterior ao inicio retorna 422")
    void datasInvalidas422() throws Exception {
        String corpo = """
                {"nome":"Evento Invalido","descricao":"Datas erradas","preco":0.00,"limiteParticipantes":5,
                 "rua":"Rua A","numero":"1","bairro":"Centro",
                 "dataHoraInicio":"2026-12-02T09:00:00","dataHoraFim":"2026-12-01T18:00:00"}
                """.replace("\n", "");

        mockMvc.perform(multipart(BASE_EVENTOS)
                        .file(new MockMultipartFile("dados", "", "application/json", corpo.getBytes()))
                        .header("user-id", organizador.getId()))
                .andExpect(status().isUnprocessableEntity());
    }
}