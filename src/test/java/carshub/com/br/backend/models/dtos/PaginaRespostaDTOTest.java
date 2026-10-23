package carshub.com.br.backend.models.dtos;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class PaginaRespostaDTOTest {

    private static List<Integer> listaDe(int quantidade) {
        return IntStream.rangeClosed(1, quantidade).boxed().toList();
    }

    @Test
    @DisplayName("tamanho <= 0 usa o default 5")
    void tamanhoInvalidoUsaDefaultCinco() {
        PaginaRespostaDTO<Integer> pagina = PaginaRespostaDTO.de(listaDe(12), 0, 0);
        assertThat(pagina.tamanho()).isEqualTo(5);
        assertThat(pagina.conteudo()).containsExactly(1, 2, 3, 4, 5);
        assertThat(pagina.totalElementos()).isEqualTo(12);
        assertThat(pagina.totalPaginas()).isEqualTo(3);
        assertThat(pagina.ultima()).isFalse();
    }

    @Test
    @DisplayName("primeira pagina de 12 itens com tamanho 5")
    void primeiraPagina() {
        PaginaRespostaDTO<Integer> pagina = PaginaRespostaDTO.de(listaDe(12), 0, 5);
        assertThat(pagina.pagina()).isZero();
        assertThat(pagina.conteudo()).hasSize(5).containsExactly(1, 2, 3, 4, 5);
        assertThat(pagina.totalPaginas()).isEqualTo(3);
        assertThat(pagina.ultima()).isFalse();
    }

    @Test
    @DisplayName("ultima pagina traz apenas o resto e marca ultima=true")
    void ultimaPagina() {
        PaginaRespostaDTO<Integer> pagina = PaginaRespostaDTO.de(listaDe(12), 2, 5);
        assertThat(pagina.conteudo()).containsExactly(11, 12);
        assertThat(pagina.ultima()).isTrue();
    }

    @Test
    @DisplayName("lista vazia: zero paginas e ultima=true")
    void listaVazia() {
        PaginaRespostaDTO<Integer> pagina = PaginaRespostaDTO.de(List.of(), 0, 5);
        assertThat(pagina.conteudo()).isEmpty();
        assertThat(pagina.totalElementos()).isZero();
        assertThat(pagina.totalPaginas()).isZero();
        assertThat(pagina.ultima()).isTrue();
    }

    @Test
    @DisplayName("pagina negativa e tratada como zero")
    void paginaNegativaViraZero() {
        PaginaRespostaDTO<Integer> pagina = PaginaRespostaDTO.de(listaDe(12), -3, 5);
        assertThat(pagina.pagina()).isZero();
        assertThat(pagina.conteudo()).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    @DisplayName("pagina alem da ultima devolve conteudo vazio")
    void paginaAlemDaUltima() {
        PaginaRespostaDTO<Integer> pagina = PaginaRespostaDTO.de(listaDe(12), 9, 5);
        assertThat(pagina.conteudo()).isEmpty();
        assertThat(pagina.ultima()).isTrue();
    }
}