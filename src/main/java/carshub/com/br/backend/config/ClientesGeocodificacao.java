package carshub.com.br.backend.config;

import org.springframework.web.client.RestClient;

/**
 * Os DOIS clientes HTTP da geocodificacao (decisao A2).

 * Por que dois: o caminho DIRETO (endereco -> coordenadas) 3s, o REVERSO (coordenadas -> endereco) é best-effort e 0.8.

 * Vem num unico bean de tipo proprio para nao haver ambiguidade de injecao por
 * tipo (e para continuar funcionando com @RequiredArgsConstructor do Lombok,
 * que nao propaga @Qualifier).
 */
public record ClientesGeocodificacao(RestClient clienteDireto, RestClient clienteReverso) {
}