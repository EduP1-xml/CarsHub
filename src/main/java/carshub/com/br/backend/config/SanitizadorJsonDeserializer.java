package carshub.com.br.backend.config;

import carshub.com.br.backend.services.seguranca.SanitizadorEntrada;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;

import java.util.regex.Pattern;

/**
 * SPRINT 6 (11.6): desserializador de String (Jackson 3 — tools.jackson, que e
 * o que os conversores HTTP do Boot 4.1.1/Framework 7 usam) que sanitiza todo
 * texto livre recebido em JSON (inclusive a parte "dados" dos formularios
 * multipart) contra scripts maliciosos.
 *
 * O nome do campo chega via createContextual (BeanProperty): campos
 * identitarios/credenciais ficam ISENTOS (nao podem mudar de conteudo, sob
 * risco de quebrar senhas/tokens/URLs): senha*, accessToken, refreshToken,
 * token*, codigo*, chave*, login, email, url*, telefone, cep, placa, cpf, cnpj.
 */
public class SanitizadorJsonDeserializer extends ValueDeserializer<String> {

    private static final Pattern CAMPOS_ISENTOS = Pattern.compile(
            "(?i)^(senha|accesstoken|refreshtoken|token|codigo|chave|login|email|url|telefone|cep|placa|cpf|cnpj).*");

    private final SanitizadorEntrada sanitizador;
    private final String nomeCampo;

    public SanitizadorJsonDeserializer(SanitizadorEntrada sanitizador) {
        this(sanitizador, null);
    }

    private SanitizadorJsonDeserializer(SanitizadorEntrada sanitizador, String nomeCampo) {
        this.sanitizador = sanitizador;
        this.nomeCampo = nomeCampo;
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext context, BeanProperty property) {
        String nome = property == null ? null : property.getName();
        if (nome == null) {
            return this;
        }
        return new SanitizadorJsonDeserializer(sanitizador, nome);
    }

    @Override
    public String deserialize(JsonParser parser, DeserializationContext context) throws JacksonException {
        String valor = parser.getValueAsString();
        if (valor == null || valor.isEmpty()) {
            return valor;
        }

        if (nomeCampo != null && CAMPOS_ISENTOS.matcher(nomeCampo).matches()) {
            return valor;
        }

        return sanitizador.sanitizar(valor);
    }
}
