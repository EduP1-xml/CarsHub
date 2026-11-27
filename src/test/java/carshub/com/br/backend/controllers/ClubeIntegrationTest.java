package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.geocodificacao.LocalizacaoGeocodificadaDTO;
import carshub.com.br.backend.models.dtos.servicos.EnderecoOcorrenciaDTO;
import carshub.com.br.backend.models.entities.Carro;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.OrganizadorEvento;
import carshub.com.br.backend.models.enums.carros.TipoCarroceria;
import carshub.com.br.backend.models.enums.tiposUsuarios.TipoUsuario;
import carshub.com.br.backend.testsupport.FabricaDeDados;
import carshub.com.br.backend.testsupport.TesteIntegracaoBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static carshub.com.br.backend.testsupport.FabricaDeDados.cnpjUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.cpfUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.criarCarro;
import static carshub.com.br.backend.testsupport.FabricaDeDados.criarColecionador;
import static carshub.com.br.backend.testsupport.FabricaDeDados.criarOrganizador;
import static carshub.com.br.backend.testsupport.FabricaDeDados.emailUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.placaUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.sufixoUnico;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Clubes ponta a ponta: criacao (fotos 0-5), membros, vinculo de carros,
 * desvinculo automatico, administracao e as abas Membros/Carros/Eventos.
 * Banco MySQL real: os dados PERMANECEM no banco (nomes/marcas com sufixo unico).
 *
 * Clube padrao ("Fuscas"): marca {Marca<sufixo>}, modelo {Fusca}, carroceria {SEDA},
 * ano 1960-1985. A marca unica isola o cenario dos dados de execucoes anteriores.
 */
class ClubeIntegrationTest extends TesteIntegracaoBase {

    private static final String BASE = "/api/carshub/clubes";
    private static final String BASE_CARROS = "/api/carshub/colecionador/carros";
    private static final String BASE_EVENTOS = "/api/carshub/organizadores/eventos";

    private String sufixo;
    private String marca;
    private Entusiasta admin;

    @BeforeEach
    void preparar() {
        sufixo = sufixoUnico();
        marca = "Marca" + sufixo;
        admin = criarColecionador(repositorioEntusiasta, emailUnico("cluadm"), cpfUnico());
        Mockito.when(servicoGeocodificacao.geocodificarEndereco(anyString()))
                .thenReturn(new LocalizacaoGeocodificadaDTO(-23.5505, -46.6333, "Rua A, 1 - Centro",
                        EnderecoOcorrenciaDTO.vazio()));
    }

    // ==================================================================
    // FIXTURES / HELPERS
    // ==================================================================

    private String filtroFuscas() {
        return """
                {"marcas":["%s"],"modelos":["Fusca"],"carrocerias":["SEDA"],"anoMin":1960,"anoMax":1985}
                """.formatted(marca).trim();
    }

    private String corpoClube(String nome, String filtro) {
        return """
                {"nome":"%s","descricao":"Clube de teste","filtro":%s}
                """.formatted(nome, filtro).trim();
    }

    private MockMultipartFile dados(String json) {
        return new MockMultipartFile("dados", "", "application/json", json.getBytes());
    }

    private MockMultipartFile foto(int indice) {
        return new MockMultipartFile("arquivos", "foto" + indice + ".png", "image/png", new byte[]{(byte) indice});
    }

    private UUID criarClube(Entusiasta dono, String nome, String filtro) throws Exception {
        MvcResult criado = mockMvc.perform(multipart(BASE).with(csrfToken())
                        .file(dados(corpoClube(nome, filtro)))
                        .header("Authorization", tokenDe(dono)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json(criado).get("idClube").asText());
    }

    private UUID criarClubeFuscas() throws Exception {
        return criarClube(admin, "Fuscas " + sufixo, filtroFuscas());
    }

    private Carro criarCarroDe(Entusiasta dono, String modelo, TipoCarroceria carroceria, int ano) {
        Carro carro = criarCarro(repositorioCarro, dono.getColecionador(), placaUnico());
        carro.setMarca(marca);
        carro.setModelo(modelo);
        carro.setCarroceria(carroceria);
        carro.setAno(ano);
        return repositorioCarro.saveAndFlush(carro);
    }

    private Entusiasta criarEntusiastaSemGaragem(String marcador) {
        Entusiasta entusiasta = new Entusiasta();
        entusiasta.setLogin(emailUnico(marcador));
        entusiasta.setSenha(FabricaDeDados.SENHA_PADRAO);
        entusiasta.setNome("Entusiasta " + marcador);
        entusiasta.setDataNascimento(LocalDate.of(1990, 1, 15));
        entusiasta.setTelefone("11999999999");
        entusiasta.setTipoUsuario(TipoUsuario.ENTUSIASTA);
        entusiasta.setAtivo(true);
        entusiasta.setPossuiCarros(false);
        entusiasta.setCpf(cpfUnico());
        return repositorioEntusiasta.saveAndFlush(entusiasta);
    }

    private void entrar(Entusiasta quem, UUID idClube, String corpo, int statusEsperado) throws Exception {
        var requisicao = post(BASE + "/{id}/membros", idClube).with(csrfToken())
                .header("Authorization", tokenDe(quem));
        if (corpo != null) {
            requisicao = requisicao.contentType("application/json").content(corpo);
        }
        mockMvc.perform(requisicao).andExpect(status().is(statusEsperado));
    }

    private JsonNode json(MvcResult resultado) throws Exception {
        return objectMapper.readTree(resultado.getResponse().getContentAsString());
    }

    private JsonNode detalhar(UUID idClube, Entusiasta quem) throws Exception {
        return json(mockMvc.perform(get(BASE + "/{id}", idClube).header("Authorization", tokenDe(quem)))
                .andExpect(status().isOk()).andReturn());
    }

    private List<String> idsDaPagina(MvcResult resultado, String campoId) throws Exception {
        List<String> ids = new ArrayList<>();
        for (JsonNode item : json(resultado).get("conteudo")) {
            ids.add(item.get(campoId).asText());
        }
        return ids;
    }

    // ==================================================================
    // CRIACAO E FOTOS
    // ==================================================================

    @Test
    @DisplayName("criar clube sem fotos: o criador ja e membro e administrador, com o proprio id e data de entrada")
    void criarClubeSemFotos() throws Exception {
        UUID idClube = criarClubeFuscas();

        JsonNode clube = detalhar(idClube, admin);
        assertThat(clube.get("souAdministrador").asBoolean()).isTrue();
        assertThat(clube.get("souMembro").asBoolean()).isTrue();
        assertThat(clube.get("quantidadeMembros").asLong()).isEqualTo(1);
        assertThat(clube.get("fotosClube").size()).isZero();
        assertThat(clube.get("filtro").get("modelos").get(0).asText()).isEqualTo("Fusca");

        mockMvc.perform(get(BASE + "/{id}/membros", idClube).header("Authorization", tokenDe(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo[0].idEntusiasta").value(admin.getId().toString()))
                .andExpect(jsonPath("$.conteudo[0].administrador").value(true))
                .andExpect(jsonPath("$.conteudo[0].dataEntrada").isNotEmpty());
    }

    @Test
    @DisplayName("criar clube com 5 fotos funciona; 6 fotos -> 400")
    void limiteDeFotosNaCriacao() throws Exception {
        MockMultipartHttpServletRequestBuilder cincoFotos = multipart(BASE);
        cincoFotos.file(dados(corpoClube("Cinco " + sufixo, filtroFuscas())));
        for (int i = 1; i <= 5; i++) {
            cincoFotos.file(foto(i));
        }
        mockMvc.perform(cincoFotos.with(csrfToken()).header("Authorization", tokenDe(admin)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.fotosClube.length()").value(5));

        MockMultipartHttpServletRequestBuilder seisFotos = multipart(BASE);
        seisFotos.file(dados(corpoClube("Seis " + sufixo, filtroFuscas())));
        for (int i = 1; i <= 6; i++) {
            seisFotos.file(foto(i));
        }
        mockMvc.perform(seisFotos.with(csrfToken()).header("Authorization", tokenDe(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("PUT com 6 arquivos ou 6 URLs no JSON -> 400")
    void limiteDeFotosNaEdicao() throws Exception {
        UUID idClube = criarClubeFuscas();

        MockMultipartHttpServletRequestBuilder seisArquivos = multipart(HttpMethod.PUT, BASE + "/{id}", idClube);
        seisArquivos.file(dados(corpoClube("Fuscas " + sufixo, filtroFuscas())));
        for (int i = 1; i <= 6; i++) {
            seisArquivos.file(foto(i));
        }
        mockMvc.perform(seisArquivos.with(csrfToken()).header("Authorization", tokenDe(admin)))
                .andExpect(status().isBadRequest());

        String seisUrls = """
                {"nome":"Fuscas %s","descricao":"x","filtro":%s,"fotosClube":["a","b","c","d","e","f"]}
                """.formatted(sufixo, filtroFuscas()).trim();
        mockMvc.perform(multipart(HttpMethod.PUT, BASE + "/{id}", idClube).file(dados(seisUrls))
                        .with(csrfToken()).header("Authorization", tokenDe(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("nome repetido -> 409; filtro sem criterio -> 422; faixa invertida -> 422")
    void validacoesDaCriacao() throws Exception {
        criarClubeFuscas();

        mockMvc.perform(multipart(BASE).file(dados(corpoClube("fuscas " + sufixo, filtroFuscas())))
                        .with(csrfToken()).header("Authorization", tokenDe(admin)))
                .andExpect(status().isConflict());

        mockMvc.perform(multipart(BASE).file(dados(corpoClube("Vazio " + sufixo, "{}")))
                        .with(csrfToken()).header("Authorization", tokenDe(admin)))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(multipart(BASE).file(dados(corpoClube("Invertido " + sufixo, "{\"anoMin\":1990,\"anoMax\":1980}")))
                        .with(csrfToken()).header("Authorization", tokenDe(admin)))
                .andExpect(status().isUnprocessableEntity());
    }

    // ==================================================================
    // ENTRADA, VINCULOS E SAIDA
    // ==================================================================

    @Test
    @DisplayName("entrar: sem carros; ja vinculando compativel; incompativel -> 422 sem membro; carro alheio -> 403; de novo -> 409")
    void entradaNoClube() throws Exception {
        UUID idClube = criarClubeFuscas();

        Entusiasta semGaragem = criarEntusiastaSemGaragem("cluent" + sufixo);
        entrar(semGaragem, idClube, null, 201);
        entrar(semGaragem, idClube, null, 409);

        Entusiasta colecionador = criarColecionador(repositorioEntusiasta, emailUnico("clucol"), cpfUnico());
        Carro fusca = criarCarroDe(colecionador, "Fusca", TipoCarroceria.SEDA, 1972);
        Carro gol = criarCarroDe(colecionador, "Gol", TipoCarroceria.HATCH, 1980);

        // Seletor oferece so o Fusca.
        mockMvc.perform(get(BASE + "/{id}/meus-carros-compativeis", idClube)
                        .header("Authorization", tokenDe(colecionador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].idCarro").value(fusca.getId().toString()))
                .andExpect(jsonPath("$[0].vinculado").value(false));

        // Incompativel na entrada: 422 e NENHUM membro criado.
        entrar(colecionador, idClube, "{\"idsCarros\":[\"%s\",\"%s\"]}".formatted(fusca.getId(), gol.getId()), 422);
        mockMvc.perform(get(BASE + "/{id}/membros", idClube).header("Authorization", tokenDe(colecionador)))
                .andExpect(status().isForbidden());

        // Carro de outro usuario: 403.
        Entusiasta outro = criarColecionador(repositorioEntusiasta, emailUnico("cluout"), cpfUnico());
        entrar(outro, idClube, "{\"idsCarros\":[\"%s\"]}".formatted(fusca.getId()), 403);

        // Compativel: entra ja vinculando.
        entrar(colecionador, idClube, "{\"idsCarros\":[\"%s\"]}".formatted(fusca.getId()), 201);
        assertThat(detalhar(idClube, colecionador).get("quantidadeCarrosVinculados").asLong()).isEqualTo(1);

        MvcResult membros = mockMvc.perform(get(BASE + "/{id}/membros", idClube)
                        .header("Authorization", tokenDe(admin)))
                .andExpect(status().isOk()).andReturn();
        for (JsonNode membro : json(membros).get("conteudo")) {
            String id = membro.get("idEntusiasta").asText();
            if (id.equals(colecionador.getId().toString())) {
                assertThat(membro.get("carrosVinculados").size()).isEqualTo(1);
                assertThat(membro.get("carrosVinculados").get(0).get("modelo").asText()).isEqualTo("Fusca");
            }
            if (id.equals(semGaragem.getId().toString())) {
                assertThat(membro.get("carrosVinculados").size()).isZero();
            }
        }
    }

    @Test
    @DisplayName("vincular e desvincular depois da entrada; vincular de novo -> 409; aba Carros sem placa")
    void vinculosDepoisDaEntrada() throws Exception {
        UUID idClube = criarClubeFuscas();
        Entusiasta colecionador = criarColecionador(repositorioEntusiasta, emailUnico("cluvin"), cpfUnico());
        Carro fusca = criarCarroDe(colecionador, "Fusca", TipoCarroceria.SEDA, 1975);
        entrar(colecionador, idClube, null, 201);

        mockMvc.perform(post(BASE + "/{id}/carros/{idCarro}", idClube, fusca.getId()).with(csrfToken())
                        .header("Authorization", tokenDe(colecionador)))
                .andExpect(status().isNoContent());
        mockMvc.perform(post(BASE + "/{id}/carros/{idCarro}", idClube, fusca.getId()).with(csrfToken())
                        .header("Authorization", tokenDe(colecionador)))
                .andExpect(status().isConflict());

        mockMvc.perform(get(BASE + "/{id}/carros", idClube).header("Authorization", tokenDe(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conteudo.length()").value(1))
                .andExpect(jsonPath("$.conteudo[0].idCarro").value(fusca.getId().toString()))
                .andExpect(jsonPath("$.conteudo[0].placa").doesNotExist());

        mockMvc.perform(delete(BASE + "/{id}/carros/{idCarro}", idClube, fusca.getId()).with(csrfToken())
                        .header("Authorization", tokenDe(colecionador)))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(BASE + "/{id}/carros", idClube).header("Authorization", tokenDe(admin)))
                .andExpect(jsonPath("$.conteudo.length()").value(0));
    }

    @Test
    @DisplayName("admin muda o filtro: vinculos incompativeis somem e carrosDesvinculados vem correto")
    void desvinculoAoMudarFiltro() throws Exception {
        UUID idClube = criarClubeFuscas();
        Entusiasta colecionador = criarColecionador(repositorioEntusiasta, emailUnico("clufil"), cpfUnico());
        Carro fusca72 = criarCarroDe(colecionador, "Fusca", TipoCarroceria.SEDA, 1972);
        Carro fusca84 = criarCarroDe(colecionador, "Fusca", TipoCarroceria.SEDA, 1984);
        entrar(colecionador, idClube, "{\"idsCarros\":[\"%s\",\"%s\"]}".formatted(fusca72.getId(), fusca84.getId()), 201);

        String filtroAte1980 = """
                {"marcas":["%s"],"modelos":["Fusca"],"carrocerias":["SEDA"],"anoMin":1960,"anoMax":1980}
                """.formatted(marca).trim();
        mockMvc.perform(multipart(HttpMethod.PUT, BASE + "/{id}", idClube)
                        .file(dados(corpoClube("Fuscas " + sufixo, filtroAte1980)))
                        .with(csrfToken()).header("Authorization", tokenDe(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.carrosDesvinculados").value(1))
                .andExpect(jsonPath("$.quantidadeCarrosVinculados").value(1));
    }

    @Test
    @DisplayName("dono edita o carro e ele fica incompativel: o vinculo some; excluir carro vinculado funciona")
    void desvinculoAoEditarEExcluirCarro() throws Exception {
        UUID idClube = criarClubeFuscas();
        Entusiasta colecionador = criarColecionador(repositorioEntusiasta, emailUnico("cluedt"), cpfUnico());
        Carro fusca = criarCarroDe(colecionador, "Fusca", TipoCarroceria.SEDA, 1972);
        Carro outroFusca = criarCarroDe(colecionador, "Fusca", TipoCarroceria.SEDA, 1970);
        entrar(colecionador, idClube, "{\"idsCarros\":[\"%s\",\"%s\"]}".formatted(fusca.getId(), outroFusca.getId()), 201);

        mockMvc.perform(multipart(HttpMethod.PATCH, BASE_CARROS + "/{id}", fusca.getId())
                        .file(dados("{\"ano\":1990}"))
                        .with(csrfToken()).header("Authorization", tokenDe(colecionador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.clubesDesvinculados[0]").value("Fuscas " + sufixo));
        assertThat(detalhar(idClube, admin).get("quantidadeCarrosVinculados").asLong()).isEqualTo(1);

        mockMvc.perform(delete(BASE_CARROS + "/{id}", outroFusca.getId()).with(csrfToken())
                        .header("Authorization", tokenDe(colecionador)))
                .andExpect(status().isNoContent());
        assertThat(detalhar(idClube, admin).get("quantidadeCarrosVinculados").asLong()).isZero();
    }

    // ==================================================================
    // ADMINISTRACAO
    // ==================================================================

    @Test
    @DisplayName("admin nao sai (422); transferencia para membro; nao-membro -> 422; nao-admin -> 403; antigo admin sai")
    void administracao() throws Exception {
        UUID idClube = criarClubeFuscas();
        Entusiasta membro = criarEntusiastaSemGaragem("clutra" + sufixo);
        Entusiasta naoMembro = criarEntusiastaSemGaragem("clunao" + sufixo);
        entrar(membro, idClube, null, 201);

        mockMvc.perform(delete(BASE + "/{id}/membros/eu", idClube).with(csrfToken())
                        .header("Authorization", tokenDe(admin)))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(patch(BASE + "/{id}/administrador/{novo}", idClube, naoMembro.getId()).with(csrfToken())
                        .header("Authorization", tokenDe(admin)))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(patch(BASE + "/{id}/administrador/{novo}", idClube, membro.getId()).with(csrfToken())
                        .header("Authorization", tokenDe(membro)))
                .andExpect(status().isForbidden());

        mockMvc.perform(patch(BASE + "/{id}/administrador/{novo}", idClube, membro.getId()).with(csrfToken())
                        .header("Authorization", tokenDe(admin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.idAdministrador").value(membro.getId().toString()))
                .andExpect(jsonPath("$.souAdministrador").value(false))
                .andExpect(jsonPath("$.souMembro").value(true));

        // O antigo admin agora pode sair.
        mockMvc.perform(delete(BASE + "/{id}/membros/eu", idClube).with(csrfToken())
                        .header("Authorization", tokenDe(admin)))
                .andExpect(status().isNoContent());
        assertThat(detalhar(idClube, membro).get("quantidadeMembros").asLong()).isEqualTo(1);
    }

    @Test
    @DisplayName("admin remove membro e os vinculos dele somem; nao-admin editar -> 403; admin nao se remove")
    void removerMembro() throws Exception {
        UUID idClube = criarClubeFuscas();
        Entusiasta colecionador = criarColecionador(repositorioEntusiasta, emailUnico("clurem"), cpfUnico());
        Carro fusca = criarCarroDe(colecionador, "Fusca", TipoCarroceria.SEDA, 1972);
        entrar(colecionador, idClube, "{\"idsCarros\":[\"%s\"]}".formatted(fusca.getId()), 201);

        mockMvc.perform(multipart(HttpMethod.PUT, BASE + "/{id}", idClube)
                        .file(dados(corpoClube("Tomado " + sufixo, filtroFuscas())))
                        .with(csrfToken()).header("Authorization", tokenDe(colecionador)))
                .andExpect(status().isForbidden());

        mockMvc.perform(delete(BASE + "/{id}/membros/{idMembro}", idClube, admin.getId()).with(csrfToken())
                        .header("Authorization", tokenDe(admin)))
                .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(delete(BASE + "/{id}/membros/{idMembro}", idClube, colecionador.getId()).with(csrfToken())
                        .header("Authorization", tokenDe(admin)))
                .andExpect(status().isNoContent());

        JsonNode clube = detalhar(idClube, admin);
        assertThat(clube.get("quantidadeMembros").asLong()).isEqualTo(1);
        assertThat(clube.get("quantidadeCarrosVinculados").asLong()).isZero();
        mockMvc.perform(get(BASE + "/{id}/carros", idClube).header("Authorization", tokenDe(colecionador)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("excluir clube (so admin) leva membros e vinculos; o carro continua na garagem")
    void excluirClube() throws Exception {
        UUID idClube = criarClubeFuscas();
        Entusiasta colecionador = criarColecionador(repositorioEntusiasta, emailUnico("cluexc"), cpfUnico());
        Carro fusca = criarCarroDe(colecionador, "Fusca", TipoCarroceria.SEDA, 1972);
        entrar(colecionador, idClube, "{\"idsCarros\":[\"%s\"]}".formatted(fusca.getId()), 201);

        mockMvc.perform(delete(BASE + "/{id}", idClube).with(csrfToken()).header("Authorization", tokenDe(colecionador)))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(BASE + "/{id}", idClube).with(csrfToken()).header("Authorization", tokenDe(admin)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE + "/{id}", idClube).header("Authorization", tokenDe(admin)))
                .andExpect(status().isNotFound());
        assertThat(repositorioCarro.findById(fusca.getId())).isPresent();
    }

    // ==================================================================
    // ACESSO E ABA EVENTOS
    // ==================================================================

    @Test
    @DisplayName("nao-membro em /membros, /carros e /eventos -> 403")
    void abasSoParaMembros() throws Exception {
        UUID idClube = criarClubeFuscas();
        Entusiasta naoMembro = criarEntusiastaSemGaragem("clufor" + sufixo);

        for (String aba : List.of("/membros", "/carros", "/eventos")) {
            mockMvc.perform(get(BASE + "/{id}" + aba, idClube).header("Authorization", tokenDe(naoMembro)))
                    .andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("aba Eventos: so os compativeis com o gosto do clube (inclui eventos sem filtro)")
    void abaEventosSoCompativeis() throws Exception {
        UUID idClube = criarClubeFuscas();
        OrganizadorEvento organizador = criarOrganizador(repositorioOrganizadorEvento, emailUnico("cluorg"), cnpjUnico());

        UUID maisAmplo = criarEvento(organizador, "{\"marcas\":[\"%s\",\"Ford\"],\"anoMin\":1950,\"anoMax\":1990}".formatted(marca));
        UUID faixaEstreita = criarEvento(organizador, "{\"anoMin\":1970,\"anoMax\":1980}");
        UUID outroModelo = criarEvento(organizador, "{\"modelos\":[\"Gol\"]}");
        UUID semFiltro = criarEvento(organizador, null);
        UUID carroceriasAmplas = criarEvento(organizador, "{\"carrocerias\":[\"SEDA\",\"CUPE\"]}");

        MvcResult eventos = mockMvc.perform(get(BASE + "/{id}/eventos", idClube)
                        .param("tamanho", "1000").header("Authorization", tokenDe(admin)))
                .andExpect(status().isOk()).andReturn();
        List<String> ids = idsDaPagina(eventos, "idEvento");

        assertThat(ids).contains(maisAmplo.toString(), semFiltro.toString(), carroceriasAmplas.toString());
        assertThat(ids).doesNotContain(faixaEstreita.toString(), outroModelo.toString());
    }

    // ==================================================================
    // FILTRO DO EVENTO (POST / PUT / PATCH)
    // ==================================================================

    private String corpoEvento(String filtro) {
        String inicio = LocalDateTime.now().plusDays(30).withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String fim = LocalDateTime.now().plusDays(30).plusHours(8).withNano(0).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        return """
                {"nome":"Encontro %s","descricao":"Evento de teste","preco":10.00,"limiteParticipantes":10,
                 "rua":"Rua A","numero":"1","bairro":"Centro","cidade":"Sao Paulo","uf":"SP","cep":"01310100",
                 "dataHoraInicio":"%s","dataHoraFim":"%s"%s}
                """.formatted(sufixo, inicio, fim, filtro == null ? "" : ",\"filtroCarro\":" + filtro)
                .replace("\n", "");
    }

    private UUID criarEvento(OrganizadorEvento organizador, String filtro) throws Exception {
        MvcResult criado = mockMvc.perform(multipart(BASE_EVENTOS).file(dados(corpoEvento(filtro)))
                        .with(csrfToken()).header("Authorization", tokenDe(organizador)))
                .andExpect(status().isCreated())
                .andReturn();
        return UUID.fromString(json(criado).get("idEvento").asText());
    }

    @Test
    @DisplayName("evento: POST/PUT/PATCH persistem e devolvem filtroCarro; filtro vazio limpa; min > max -> 422")
    void filtroDoEvento() throws Exception {
        OrganizadorEvento organizador = criarOrganizador(repositorioOrganizadorEvento, emailUnico("cluevt"), cnpjUnico());

        UUID idEvento = criarEvento(organizador, "{\"marcas\":[\"Volkswagen\"],\"som\":true}");
        mockMvc.perform(get(BASE_EVENTOS + "/{id}", idEvento).header("Authorization", tokenDe(organizador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filtroCarro.marcas[0]").value("Volkswagen"))
                .andExpect(jsonPath("$.filtroCarro.som").value(true));

        // PATCH: troca o filtro.
        mockMvc.perform(multipart(HttpMethod.PATCH, BASE_EVENTOS + "/{id}", idEvento)
                        .file(dados("{\"filtroCarro\":{\"cambios\":[\"MANUAL\"]}}"))
                        .with(csrfToken()).header("Authorization", tokenDe(organizador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filtroCarro.cambios[0]").value("MANUAL"))
                .andExpect(jsonPath("$.filtroCarro.marcas.length()").value(0));

        // PATCH sem filtroCarro mantem o atual.
        mockMvc.perform(multipart(HttpMethod.PATCH, BASE_EVENTOS + "/{id}", idEvento)
                        .file(dados("{\"preco\":15.00}"))
                        .with(csrfToken()).header("Authorization", tokenDe(organizador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filtroCarro.cambios[0]").value("MANUAL"));

        // PUT com filtro novo.
        mockMvc.perform(multipart(HttpMethod.PUT, BASE_EVENTOS + "/{id}", idEvento)
                        .file(dados(corpoEvento("{\"aroMin\":14,\"aroMax\":17}")))
                        .with(csrfToken()).header("Authorization", tokenDe(organizador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filtroCarro.aroMin").value(14))
                .andExpect(jsonPath("$.filtroCarro.cambios.length()").value(0));

        // PATCH com filtro vazio -> evento aberto a todos.
        mockMvc.perform(multipart(HttpMethod.PATCH, BASE_EVENTOS + "/{id}", idEvento)
                        .file(dados("{\"filtroCarro\":{}}"))
                        .with(csrfToken()).header("Authorization", tokenDe(organizador)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.filtroCarro").doesNotExist());

        // Faixa invertida -> 422.
        mockMvc.perform(multipart(BASE_EVENTOS).file(dados(corpoEvento("{\"anoMin\":2000,\"anoMax\":1990}")))
                        .with(csrfToken()).header("Authorization", tokenDe(organizador)))
                .andExpect(status().isUnprocessableEntity());

        // Listagem do organizador devolve o filtro (carregado em lote).
        UUID comFiltro = criarEvento(organizador, "{\"cores\":[\"Azul\"]}");
        MvcResult meus = mockMvc.perform(get(BASE_EVENTOS).param("tamanho", "50")
                        .header("Authorization", tokenDe(organizador)))
                .andExpect(status().isOk()).andReturn();
        boolean achou = false;
        for (JsonNode evento : json(meus).get("conteudo")) {
            if (evento.get("idEvento").asText().equals(comFiltro.toString())) {
                assertThat(evento.get("filtroCarro").get("cores").get(0).asText()).isEqualTo("Azul");
                achou = true;
            }
        }
        assertThat(achou).isTrue();
    }
}
