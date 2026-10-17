package carshub.com.br.backend.models.dtos;

import java.util.List;

/**
 * Envelope unico de paginacao da API (decisao D3).
 *
 * Usamos envelope proprio em vez do Page<T> do Spring para o contrato do JSON
 * ficar estavel (o Page carrega metadados internos que mudam entre versoes) e
 * para servir tanto as listas paginadas no banco quanto as que sao ordenadas em
 * memoria (painel por distancia).
 *
 * @param conteudo      itens da pagina atual
 * @param pagina        numero da pagina (base 0)
 * @param tamanho       itens por pagina
 * @param totalElementos total de itens de todas as paginas
 * @param totalPaginas  quantidade de paginas
 * @param ultima        true quando esta e a ultima pagina
 */
public record PaginaRespostaDTO<T>(
        List<T> conteudo,
        int pagina,
        int tamanho,
        long totalElementos,
        int totalPaginas,
        boolean ultima
) {

    public static <T> PaginaRespostaDTO<T> de(List<T> listaCompleta, int pagina, int tamanho) {
        int tamanhoSeguro = (tamanho <= 0) ? 20 : tamanho;
        int total = listaCompleta.size();
        int totalPaginas = (total == 0) ? 0 : (int) Math.ceil((double) total / tamanhoSeguro);
        int paginaSegura = Math.max(0, pagina);

        int inicio = Math.min(paginaSegura * tamanhoSeguro, total);
        int fim = Math.min(inicio + tamanhoSeguro, total);

        return new PaginaRespostaDTO<>(
                List.copyOf(listaCompleta.subList(inicio, fim)),
                paginaSegura,
                tamanhoSeguro,
                total,
                totalPaginas,
                paginaSegura >= totalPaginas - 1);
    }
}