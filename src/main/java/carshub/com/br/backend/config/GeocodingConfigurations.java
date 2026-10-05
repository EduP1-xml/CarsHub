package carshub.com.br.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

/**
 * Cliente HTTP do Google Geocoding API.
 *
 * RestClient (e nao RestTemplate/OpenFeign) porque ja vem no spring-webmvc, e
 * sincrono e nao exige dependencia nova. Os timeouts seguem o mesmo padrao dos
 * timeouts de SMTP ja configurados no projeto: sem eles, uma instabilidade do
 * Google seguraria a thread da requisicao indefinidamente.
 *
 * O bean se chama "clienteGeocodificacao". Se um dia existir outro RestClient na
 * aplicacao, injete por @Qualifier (com @RequiredArgsConstructor, configure
 * lombok.copyableAnnotations no pom ou use um @Bean do tipo GeocodificacaoService).
 */
@Configuration
public class GeocodingConfigurations {

    @Value("${app.geocodificacao.urlBase}")
    private String urlBaseGeocodificacao;

    @Value("${app.geocodificacao.tempoLimiteConexaoMs:3000}")
    private int tempoLimiteConexaoMs;

    @Value("${app.geocodificacao.tempoLimiteLeituraMs:3000}")
    private int tempoLimiteLeituraMs;

    @Bean
    public RestClient clienteGeocodificacao() {
        HttpClient clienteHttp = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(tempoLimiteConexaoMs))
                .build();

        JdkClientHttpRequestFactory fabricaRequisicoes = new JdkClientHttpRequestFactory(clienteHttp);
        fabricaRequisicoes.setReadTimeout(Duration.ofMillis(tempoLimiteLeituraMs));

        return RestClient.builder()
                .requestFactory(fabricaRequisicoes)
                .baseUrl(urlBaseGeocodificacao)
                .build();
    }
}