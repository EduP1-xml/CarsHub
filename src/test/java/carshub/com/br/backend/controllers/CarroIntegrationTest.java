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
                 "suspensao":"FIXA","freio":"DISCO","direcao":"HIDRAULICA","roda":"LIGA_LEVE","aro":15,
                 "numeroPortas":4,"numeroAssentos":5,"materialRevestimentoInterno":"TECIDO","placa":"%s",
                 "vidrosEletricos":true,"som":true,"arCondicionado":true,"tetoSolar":false,"urlFotoCarro":[]}
                """.formatted(placa).replace("\n", "");
    }

    private UUID criarViaApi(Entusiasta entusiasta, String placa) throws Exception {
        MvcResult criado = mockMvc.perform(multipart(BASE).with(csrfToken())
                        .file(new MockMultipartFile("dados", "", "application/json", corpoCarro(placa).getBytes()))
                        .header("Authorization", tokenDe(entusiasta)))
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

        mockMvc.perform(get(BASE).header("Authorization", tokenDe(entusiasta)))
                .andExpect(status().isOk())
                // AJUSTE 1.1: a garagem agora devolve o envelope PaginaRespostaDTO.
                .andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.totalElementos").value(1));

        // F13: o detalhe devolve TODOS os campos do carro (inclusive fotos)
        mockMvc.perform(get(BASE + "/{id}", idCarro).header("Authorization", tokenDe(entusiasta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idCarro").value(idCarro.toString()))
                .andExpect(jsonPath("$.ano").value(2015))
                .andExpect(jsonPath("$.cor").value("Prata"))
                .andExpect(jsonPath("$.carroceria").value("HATCH"))
                .andExpect(jsonPath("$.nomeProprietario").value(atualizado.getNome()))
                .andExpect(jsonPath("$.fotosCarro.length()").value(0));

        // AVISO 7.9: PUT multipart como o POST
        mockMvc.perform(multipart(org.springframework.http.HttpMethod.PUT, BASE + "/{id}", idCarro).with(csrfToken())
                        .file(new MockMultipartFile("dados", "", "application/json",
                                corpoCarro(placa).replace("\"modelo\":\"Gol\"", "\"modelo\":\"Golf\"").getBytes()))
                        .header("Authorization", tokenDe(entusiasta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.modelo").value("Golf"));

        // AVISO 7.8: PATCH multipart — cor via dados + foto1 (slot a slot)
        mockMvc.perform(multipart(org.springframework.http.HttpMethod.PATCH, BASE + "/{id}", idCarro).with(csrfToken())
                        .file(new MockMultipartFile("dados", "", "application/json", "{\"cor\":\"Azul\"}".getBytes()))
                        .file(new MockMultipartFile("foto1", "foto1.png", "image/png", new byte[]{1, 2, 3}))
                        .header("Authorization", tokenDe(entusiasta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cor").value("Azul"))
                .andExpect(jsonPath("$.fotosCarro.length()").value(1));

        org.assertj.core.api.Assertions.assertThat(
                        repositorioCarro.findById(idCarro).orElseThrow().getCor())
                .isEqualTo("Azul");

        mockMvc.perform(delete(BASE + "/{id}", idCarro).with(csrfToken()).header("Authorization", tokenDe(entusiasta)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE + "/{id}", idCarro).header("Authorization", tokenDe(entusiasta)))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH troca as fotos slot a slot e valida o limite de 2 (7.8)")
    void patchTrocaFotosSlotASlot() throws Exception {
        String sufixo = sufixoUnico();
        Entusiasta entusiasta = criarEntusiastaSemGaragem("fot" + sufixo);
        UUID idCarro = criarViaApi(entusiasta, placaUnico());

        // foto1 e foto2 juntas: 2 fotos
        mockMvc.perform(multipart(org.springframework.http.HttpMethod.PATCH, BASE + "/{id}", idCarro).with(csrfToken())
                        .file(new MockMultipartFile("dados", "", "application/json", "{}".getBytes()))
                        .file(new MockMultipartFile("foto1", "a.png", "image/png", new byte[]{1}))
                        .file(new MockMultipartFile("foto2", "b.png", "image/png", new byte[]{2}))
                        .header("Authorization", tokenDe(entusiasta)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotosCarro.length()").value(2));

        // Trocando so a foto 1: continua com 2 (a foto 2 e preservada)
        MvcResult trocaFoto1 = mockMvc.perform(multipart(org.springframework.http.HttpMethod.PATCH, BASE + "/{id}", idCarro).with(csrfToken())
                        .file(new MockMultipartFile("dados", "", "application/json", "{}".getBytes()))
                        .file(new MockMultipartFile("foto1", "c.png", "image/png", new byte[]{3}))
                        .header("Authorization", tokenDe(entusiasta)))
                .andExpect(status().isOk())
                .andReturn();

        com.fasterxml.jackson.databind.JsonNode fotos = objectMapper
                .readTree(trocaFoto1.getResponse().getContentAsString()).get("fotosCarro");
        org.assertj.core.api.Assertions.assertThat(fotos.size()).isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(fotos.get(0).asText()).isNotEqualTo(fotos.get(1).asText());
    }

    @Test
    @DisplayName("carro com 3 arquivos no POST retorna 400 (limite de 2 fotos)")
    void limiteDeDuasFotos400() throws Exception {
        String sufixo = sufixoUnico();
        Entusiasta entusiasta = criarColecionador(repositorioEntusiasta, emailUnico("lim" + sufixo), cpfUnico());

        mockMvc.perform(multipart(BASE).with(csrfToken())
                        .file(new MockMultipartFile("dados", "", "application/json", corpoCarro(placaUnico()).getBytes()))
                        .file(new MockMultipartFile("arquivos", "a.png", "image/png", new byte[]{1}))
                        .file(new MockMultipartFile("arquivos", "b.png", "image/png", new byte[]{2}))
                        .file(new MockMultipartFile("arquivos", "c.png", "image/png", new byte[]{3}))
                        .header("Authorization", tokenDe(entusiasta)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("placa duplicada retorna 409")
    void placaDuplicada409() throws Exception {
        String sufixo = sufixoUnico();
        Entusiasta entusiasta = criarColecionador(repositorioEntusiasta, emailUnico("pdup" + sufixo), cpfUnico());
        String placa = placaUnico();

        criarViaApi(entusiasta, placa);

        mockMvc.perform(multipart(BASE).with(csrfToken())
                        .file(new MockMultipartFile("dados", "", "application/json", corpoCarro(placa).getBytes()))
                        .header("Authorization", tokenDe(entusiasta)))
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

        mockMvc.perform(get(BASE + "/{id}", carro.getId()).header("Authorization", tokenDe(intruso)))
                .andExpect(status().isForbidden());
    }
}
