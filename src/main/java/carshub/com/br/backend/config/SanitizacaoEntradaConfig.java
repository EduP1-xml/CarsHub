package carshub.com.br.backend.config;

import carshub.com.br.backend.services.seguranca.SanitizadorEntrada;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.module.SimpleModule;

/**
 * SPRINT 6 (11.6): registra a sanitizacao de entradas no JsonMapper
 * (Jackson 3 — tools.jackson) que os conversores HTTP do Boot 4.1.1 usam:
 * o JsonMapperBuilderCustomizer e aplicado pelo JacksonAutoConfiguration ao
 * JsonMapper bean, que por sua vez alimenta o JacksonJsonHttpMessageConverter
 * (corpo JSON e a parte "dados" dos formularios multipart).
 *
 * O ObjectMapper manual dos testes (so LE respostas) nao e afetado.
 */
@Configuration
public class SanitizacaoEntradaConfig {

    @Bean
    JsonMapperBuilderCustomizer customizadorSanitizacaoEntrada(SanitizadorEntrada sanitizador) {
        return builder -> {
            SimpleModule modulo = new SimpleModule("sanitizacao-entrada");
            modulo.addDeserializer(String.class, new SanitizadorJsonDeserializer(sanitizador));
            builder.addModule(modulo);
        };
    }
}
