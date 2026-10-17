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
 * sincrono e nao exige dependencia nova. Os timeouts seguem o padrao dos
 * timeouts de SMTP ja configurados no projeto.
 */
@Configuration
public class GeocodingConfigurations {

    @Value("${app.geocodificacao.urlBase}")
    private String urlBaseGeocodificacao;

    @Value("${app.geocodificacao.tempoLimiteConexaoMs:3000}")
    private int tempoLimiteConexaoMs;

    @Value("${app.geocodificacao.tempoLimiteLeituraDiretaMs:3000}")
    private int tempoLimiteLeituraDiretaMs;

    @Value("${app.geocodificacao.tempoLimiteLeituraReversaMs:800}")
    private int tempoLimiteLeituraReversaMs;

    @Bean
    public ClientesGeocodificacao clientesGeocodificacao() {
        return new ClientesGeocodificacao(
                construirCliente(tempoLimiteLeituraDiretaMs),
                construirCliente(tempoLimiteLeituraReversaMs));
    }

    private RestClient construirCliente(int tempoLimiteLeituraMs) {
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