package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.enums.tiposUsuarios.TipoUsuario;
import carshub.com.br.backend.testsupport.TesteIntegracaoBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;
import java.util.UUID;

import static carshub.com.br.backend.testsupport.FabricaDeDados.cpfUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.criarColecionador;
import static carshub.com.br.backend.testsupport.FabricaDeDados.criarCarro;
import static carshub.com.br.backend.testsupport.FabricaDeDados.emailUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.placaUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.sufixoUnico;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * CRUD de carros do colecionador. Banco MySQL real: os dados PERMANECEM no banco.
 */
class CarroIntegrationTest extends TesteIntegracaoBase {

    private static final String BASE = "/api/carshub/colecionador/carros";

    private String corpoCarro(String placa) {
        return """
                {"marca":"Volkswagen","modelo":"Gol","carroceria":"HATCH","ano":2015,"cor":"Prata",
                 "quantidadeCilindros":4,"motor":"LINHA","litragemMotor":1.6,"injecao":"ELETRONICA",
                 "alimentacao":"ASPIRADO","combustivel":"FLEX","cambio":"MANUAL","tracao":"DIANTEIRA",
                 "suspencao":"FIXA","freio":"DISCO","direcao":"HIDRAULICA","roda":"LIGA_LEVE","aro":15,
                 "numeroPortas":4,"numeroAssentos":5,"materialRevestimentoInterno":"TECIDO","placa":"%s",
                 "vidrosEletricos":true,"som":true,"arCondicionado":true,"tetoSolar":false,"urlFotoCarro":[]}
                """.formatted(placa).replace("\n", "");
    }

    private UUID criarViaApi(Entusiasta entusiasta, String placa) throws Exception {
        MvcResult criado = mockMvc.perform(multipart(BASE)
                        .file(new MockMultipartFile("dados", "", "application/json", corpoCarro(placa).getBytes()))
                        .header("user-id", entusiasta.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.placa").value(placa))
                .andReturn();
        return UUID.fromString(objectMapper.readTree(criado.getResponse().getContentAsString())
                .get("idCarro").asText());
    }

    private Entusiasta criarEntusiastaSemGaragem(String marcador) {
        Entusiasta entusiasta = new Entusiasta();
        entusiasta.setLogin(emailUnico(marcador));
        entusiasta.setSenha(carshub.com.br.backend.testsupport.FabricaDeDados.SENHA_PADRAO);
        entusiasta.setNome("Entusiasta " + marcador);
        entusiasta.setDataNascimento(LocalDate.of(1990, 1, 15));
        entusiasta.setTelefone("11999999999");
        entusiasta.setTipoUsuario(TipoUsuario.ENTUSIASTA);
        entusiasta.setAtivo(true);
        entusiasta.setPossuiCarros(false);
        return repositorioEntusiasta.saveAndFlush(entusiasta);
    }

    @Test
    @DisplayName("criar carro transforma o entusiasta em colecionador; listar, editar e excluir")
    void cicloDeVidaDoCarro() throws Exception {
        String sufixo = sufixoUnico();
        Entusiasta entusiasta = criarEntusiastaSemGaragem("car" + sufixo);

        String placa = placaUnico();
        UUID idCarro = criarViaApi(entusiasta, placa);

        Entusiasta atualizado = repositorioEntusiasta.findById(entusiasta.getId()).orElseThrow();
        org.assertj.core.api.Assertions.assertThat(atualizado.getPossuiCarros()).isTrue();

        mockMvc.perform(get(BASE).header("user-id", entusiasta.getId()))
                .andExpect(status().isOk())
                // AJUSTE 1.1: a garagem agora devolve o envelope PaginaRespostaDTO.
                .andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.totalElementos").value(1));

        mockMvc.perform(get(BASE + "/{id}", idCarro).header("user-id", entusiasta.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCarro").value(idCarro.toString()));

        mockMvc.perform(put(BASE + "/{id}", idCarro)
                        .header("user-id", entusiasta.getId())
                        .contentType("application/json")
                        .content(corpoCarro(placa).replace("\"modelo\":\"Gol\"", "\"modelo\":\"Golf\"")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelo").value("Golf"));

        mockMvc.perform(patch(BASE + "/{id}", idCarro)
                        .header("user-id", entusiasta.getId())
                        .contentType("application/json")
                        .content("{\"cor\":\"Azul\"}"))
                                .andExpect(status().isOk());

        // CarroRespostaDTO nao expoe a cor: confirma a alteracao direto no banco
        org.assertj.core.api.Assertions.assertThat(
                        repositorioCarro.findById(idCarro).orElseThrow().getCor())
                .isEqualTo("Azul");

        mockMvc.perform(delete(BASE + "/{id}", idCarro).header("user-id", entusiasta.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE + "/{id}", idCarro).header("user-id", entusiasta.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("placa duplicada retorna 409")
    void placaDuplicada409() throws Exception {
        String sufixo = sufixoUnico();
        Entusiasta entusiasta = criarColecionador(repositorioEntusiasta, emailUnico("pdup" + sufixo), cpfUnico());
        String placa = placaUnico();

        criarViaApi(entusiasta, placa);

        mockMvc.perform(multipart(BASE)
                        .file(new MockMultipartFile("dados", "", "application/json", corpoCarro(placa).getBytes()))
                        .header("user-id", entusiasta.getId()))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("carro de outro colecionador retorna 403")
    void carroDeOutroDono403() throws Exception {
        String sufixo = sufixoUnico();
        Entusiasta dono = criarColecionador(repositorioEntusiasta, emailUnico("don" + sufixo), cpfUnico());
        Entusiasta intruso = criarColecionador(repositorioEntusiasta, emailUnico("int" + sufixo), cpfUnico());

        carshub.com.br.backend.models.entities.Carro carro = criarCarro(repositorioCarro,
                dono.getColecionador(), placaUnico());

        mockMvc.perform(get(BASE + "/{id}", carro.getId()).header("user-id", intruso.getId()))
                .andExpect(status().isForbidden());
    }
}