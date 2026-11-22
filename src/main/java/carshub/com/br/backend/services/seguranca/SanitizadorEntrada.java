package carshub.com.br.backend.services.seguranca;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

/**
 * SPRINT 6 (11.6): sanitiza texto livre contra scripts maliciosos (XSS
 * armazenado). Usa o jsoup com Safelist.none(): QUALQUER tag HTML e atributo
 * (script, img, iframe, on*=, javascript: ...) e REMOVIDO e o texto restante
 * tem as entidades residuais escapadas — nada do que o usuario digitou chega
 * "interpretavel" a quem renderizar o conteudo como HTML.
 *
 * Tradeoff documentado: caracteres literais &lt; &gt; &amp; no texto livre sao
 * armazenados escapados (ex.: "5 < 10" vira "5 &lt; 10" no JSON). Campos
 * identitarios (senha/token/codigo/login/e-mail/url/telefone/CEP/placa/CPF/
 * CNPJ) ficam FORA da sanitizacao (nao podem mudar de conteudo) — a lista de
 * isencao esta no SanitizadorJsonDeserializer.
 */
@Component
public class SanitizadorEntrada {

    private static final Document.OutputSettings SAIDA_SEM_PRETTY_PRINT =
            new Document.OutputSettings().prettyPrint(false);

    public String sanitizar(String entrada) {
        if (entrada == null || entrada.isBlank()) {
            return entrada;
        }
        return Jsoup.clean(entrada, "", Safelist.none(), SAIDA_SEM_PRETTY_PRINT);
    }
}
