package carshub.com.br.backend.models.dtos;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;

/**
 *O que tinha dados fixos para referencia foi paginado no SQL, o que precisava de regras de
 * negócio presentes no Java, paginaram em memória.

 * Envelope unico de paginacao da API.
 * As listagens ordenaveis no banco passaram a usar o Page<T>
 * NATIVO do Spring Data — a consulta ja nasce paginada no SQL e o envelope
 * continua sendo o contrato do JSON (o Page carrega metadados internos que
 * mudam entre versoes).

 * As listas ordenadas em Java (painel por distancia/score e o painel
 * consolidado do colecionador) continuam no corte em memoria do factory
 * de(List, pagina, tamanho);
 * As listas MESCLADAS de duas consultas paginadas
 * (historico do veiculo, dashboard do organizador) usam o factory
 * de(List, totalElementos, pagina, tamanho).
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
        int tamanhoSeguro = (tamanho <= 0) ? 5 : tamanho;
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

    /** Paginacao NATIVA: converte o Page<T> do Spring Data mantendo o contrato do envelope. */
    public static <T> PaginaRespostaDTO<T> de(Page<T> paginaSpring) {
        return new PaginaRespostaDTO<>(
                List.copyOf(paginaSpring.getContent()),
                paginaSpring.getNumber(),
                paginaSpring.getSize(),
                paginaSpring.getTotalElements(),
                paginaSpring.getTotalPages(),
                paginaSpring.isLast());
    }

    /**
     * Listas MESCLADAS: o service ja entrega o recorte da pagina
     * (varias consultas paginadas concatenadas e reordenadas em Java) e aqui so
     * montamos os metadados com o total real somado.
     */
    public static <T> PaginaRespostaDTO<T> de(List<T> conteudoDaPagina, long totalElementos, int pagina, int tamanho) {
        int tamanhoSeguro = (tamanho <= 0) ? 5 : tamanho;
        int totalPaginas = (totalElementos == 0) ? 0 : (int) Math.ceil((double) totalElementos / tamanhoSeguro);
        int paginaSegura = Math.max(0, pagina);

        return new PaginaRespostaDTO<>(
                List.copyOf(conteudoDaPagina),
                paginaSegura,
                tamanhoSeguro,
                totalElementos,
                totalPaginas,
                paginaSegura >= totalPaginas - 1);
    }

    /**
     * PageRequest com as MESMAS regras de seguranca do envelope: pagina nunca
     * negativa e tamanho <= 0 vira o padrao de 5 registros por pagina.
     */
    public static PageRequest pageRequest(int pagina, int tamanho) {
        int tamanhoSeguro = (tamanho <= 0) ? 5 : tamanho;
        return PageRequest.of(Math.max(0, pagina), tamanhoSeguro);
    }
}