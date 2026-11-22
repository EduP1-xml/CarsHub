package carshub.com.br.backend.config;

import carshub.com.br.backend.services.seguranca.SanitizadorEntrada;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * SPRINT 6 (11.6): o desserializador (Jackson 3) sanitiza texto livre e POUPA
 * os campos identitarios (senha/token/codigo/login/e-mail/url/telefone/CEP/
 * placa/CPF/CNPJ).
 */
class SanitizadorJsonDeserializerTest {

    private final JsonMapper mapper = mapperComSanitizacao();

    private static JsonMapper mapperComSanitizacao() {
        SimpleModule modulo = new SimpleModule("sanitizacao-teste");
        modulo.addDeserializer(String.class, new SanitizadorJsonDeserializer(new SanitizadorEntrada()));
        return JsonMapper.builder().addModule(modulo).build();
    }

    @Test
    @DisplayName("texto livre e sanitizado na desserializacao")
    void textoLivreSanitizado() throws Exception {
        record Comentario(String comentario, String nome) {
        }

        Comentario dados = mapper.readValue(
                "{\"comentario\":\"<script>alert(1)</script>ok\",\"nome\":\"<b>Ana</b>\"}",
                Comentario.class);

        assertThat(dados.comentario()).isEqualTo("ok");
        assertThat(dados.nome()).isEqualTo("Ana");
    }

    @Test
    @DisplayName("campos identitarios NAO sao alterados (senha com &, url com query)")
    void camposIdentitariosPoupados() throws Exception {
        record Credenciais(String senha, String refreshToken, String codigo, String login,
                           String urlFotoPerfil, String telefone, String placa, String cpf) {
        }

        Credenciais dados = mapper.readValue(
                "{\"senha\":\"Senha@1&\",\"refreshToken\":\"a&b\",\"codigo\":\"123<456\","
                        + "\"login\":\"a@b.com\",\"urlFotoPerfil\":\"http://x/f.png?a=1&b=2\","
                        + "\"telefone\":\"+55 11 9<999-9999\",\"placa\":\"ABC1D23\",\"cpf\":\"123.456.789-00\"}",
                Credenciais.class);

        assertThat(dados.senha()).isEqualTo("Senha@1&");
        assertThat(dados.refreshToken()).isEqualTo("a&b");
        assertThat(dados.codigo()).isEqualTo("123<456");
        assertThat(dados.login()).isEqualTo("a@b.com");
        assertThat(dados.urlFotoPerfil()).isEqualTo("http://x/f.png?a=1&b=2");
        assertThat(dados.telefone()).isEqualTo("+55 11 9<999-9999");
        assertThat(dados.placa()).isEqualTo("ABC1D23");
        assertThat(dados.cpf()).isEqualTo("123.456.789-00");
    }
}
