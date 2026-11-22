package carshub.com.br.backend.services.servicos;

import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.models.dtos.PaginaRespostaDTO;
import carshub.com.br.backend.models.dtos.servicos.PrestadorCompativelDTO;
import carshub.com.br.backend.models.dtos.servicos.ServicoRespostaDTO;
import carshub.com.br.backend.models.entities.servicos.ServicoGuincho;
import carshub.com.br.backend.models.entities.servicos.ServicoMecanico;
import carshub.com.br.backend.repositories.servicos.ServicoGuinchoRepository;
import carshub.com.br.backend.repositories.servicos.ServicoMecanicoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PainelServicoServiceTest {

    @Mock
    private ServicoMecanicoService servicoMecanico;
    @Mock
    private ServicoGuinchoService servicoGuincho;
    @Mock
    private ServicoMecanicoRepository repositorioServicoMecanico;
    @Mock
    private ServicoGuinchoRepository repositorioServicoGuincho;
    @Mock
    private carshub.com.br.backend.repositories.CarroRepository repositorioCarro;

    @InjectMocks
    private PainelServicoService painelServico;

    private ServicoRespostaDTO item(LocalDateTime dataHoraSolicitacao) {
        ServicoRespostaDTO dto = mock(ServicoRespostaDTO.class);
        when(dto.dataHoraSolicitacao()).thenReturn(dataHoraSolicitacao);
        return dto;
    }

    /** Simula a versao paginada no SQL: devolve os primeiros "janela" itens com o total real. */
    private Page<ServicoRespostaDTO> pagina(List<ServicoRespostaDTO> itens, int janela) {
        List<ServicoRespostaDTO> conteudo = itens.subList(0, Math.min(janela, itens.size()));
        return new PageImpl<>(conteudo, PageRequest.of(0, janela), itens.size());
    }

    private void prepararFontes(List<ServicoRespostaDTO> mecanicos, List<ServicoRespostaDTO> guinchos) {
        when(servicoMecanico.listarMeusServicos(any(UUID.class), anyBoolean(), eq(0), anyInt()))
                .thenAnswer(inv -> pagina(mecanicos, inv.getArgument(3)));
        when(servicoGuincho.listarMeusServicos(any(UUID.class), anyBoolean(), eq(0), anyInt()))
                .thenAnswer(inv -> pagina(guinchos, inv.getArgument(3)));
    }

    @Test
    @DisplayName("roteia chamado de mecanico para o servico de mecanico")
    void roteiaParaMecanico() {
        UUID idServico = UUID.randomUUID();
        UUID idEntusiasta = UUID.randomUUID();
        ServicoMecanico entidade = mock(ServicoMecanico.class);
        List<PrestadorCompativelDTO> esperado = List.of();

        when(repositorioServicoMecanico.findById(idServico)).thenReturn(Optional.of(entidade));
        when(servicoMecanico.listarPrestadoresCompativeis(entidade, idEntusiasta)).thenReturn(esperado);

        assertThat(painelServico.listarPrestadoresCompativeis(idServico, idEntusiasta)).isSameAs(esperado);
        verify(servicoGuincho, never()).listarPrestadoresCompativeis(any(), any());
    }

    @Test
    @DisplayName("roteia chamado de guincho para o servico de guincho")
    void roteiaParaGuincho() {
        UUID idServico = UUID.randomUUID();
        UUID idEntusiasta = UUID.randomUUID();
        ServicoGuincho entidade = mock(ServicoGuincho.class);
        List<PrestadorCompativelDTO> esperado = List.of();

        when(repositorioServicoMecanico.findById(idServico)).thenReturn(Optional.empty());
        when(repositorioServicoGuincho.findById(idServico)).thenReturn(Optional.of(entidade));
        when(servicoGuincho.listarPrestadoresCompativeis(entidade, idEntusiasta)).thenReturn(esperado);

        assertThat(painelServico.listarPrestadoresCompativeis(idServico, idEntusiasta)).isSameAs(esperado);
    }

    @Test
    @DisplayName("id que nao e chamado de nenhum tipo retorna 404")
    void idInexistente404() {
        UUID idServico = UUID.randomUUID();
        UUID idEntusiasta = UUID.randomUUID();

        when(repositorioServicoMecanico.findById(idServico)).thenReturn(Optional.empty());
        when(repositorioServicoGuincho.findById(idServico)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> painelServico.listarPrestadoresCompativeis(idServico, idEntusiasta))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    // ==================================================================
    // AJUSTE 1.3 (pendencia): painel consolidado paginado com a tecnica da janela
    // ==================================================================

    @Test
    @DisplayName("painel consolidado: default tamanho=5 e borda na segunda pagina (mescla por data desc)")
    void painelConsolidadoPaginado() {
        UUID idEntusiasta = UUID.randomUUID();

        List<ServicoRespostaDTO> mecanicos = List.of(
                item(LocalDateTime.of(2026, 11, 1, 10, 0)),
                item(LocalDateTime.of(2026, 11, 1, 10, 5)),
                item(LocalDateTime.of(2026, 11, 1, 10, 10)));
        List<ServicoRespostaDTO> guinchos = List.of(
                item(LocalDateTime.of(2026, 11, 1, 10, 2)),
                item(LocalDateTime.of(2026, 11, 1, 10, 7)),
                item(LocalDateTime.of(2026, 11, 1, 10, 12)));
        prepararFontes(mecanicos, guinchos);

        // Pagina 0 (default tamanho=5): janela de 5 por fonte
        PaginaRespostaDTO<ServicoRespostaDTO> primeira = painelServico.listarSolicitacoesAtivas(idEntusiasta, 0, 5);
        assertThat(primeira.conteudo()).hasSize(5);
        assertThat(primeira.pagina()).isZero();
        assertThat(primeira.tamanho()).isEqualTo(5);
        assertThat(primeira.totalElementos()).isEqualTo(6);
        assertThat(primeira.totalPaginas()).isEqualTo(2);
        assertThat(primeira.ultima()).isFalse();

        assertThat(primeira.conteudo().stream().map(ServicoRespostaDTO::dataHoraSolicitacao).toList())
                .containsExactly(
                        LocalDateTime.of(2026, 11, 1, 10, 12),
                        LocalDateTime.of(2026, 11, 1, 10, 10),
                        LocalDateTime.of(2026, 11, 1, 10, 7),
                        LocalDateTime.of(2026, 11, 1, 10, 5),
                        LocalDateTime.of(2026, 11, 1, 10, 2));

        // Borda: pagina 1 traz o item restante e marca ultima=true (janela de 10)
        PaginaRespostaDTO<ServicoRespostaDTO> segunda = painelServico.listarSolicitacoesAtivas(idEntusiasta, 1, 5);
        assertThat(segunda.conteudo()).hasSize(1);
        assertThat(segunda.conteudo().get(0).dataHoraSolicitacao())
                .isEqualTo(LocalDateTime.of(2026, 11, 1, 10, 0));
        assertThat(segunda.ultima()).isTrue();
        assertThat(segunda.totalPaginas()).isEqualTo(2);

        verify(servicoMecanico).listarMeusServicos(eq(idEntusiasta), eq(true), eq(0), eq(5));
        verify(servicoGuincho).listarMeusServicos(eq(idEntusiasta), eq(true), eq(0), eq(5));
        verify(servicoMecanico).listarMeusServicos(eq(idEntusiasta), eq(true), eq(0), eq(10));
        verify(servicoGuincho).listarMeusServicos(eq(idEntusiasta), eq(true), eq(0), eq(10));
    }

    @Test
    @DisplayName("painel consolidado sem solicitacoes devolve pagina vazia")
    void painelConsolidadoVazio() {
        UUID idEntusiasta = UUID.randomUUID();
        prepararFontes(List.of(), List.of());

        PaginaRespostaDTO<ServicoRespostaDTO> pagina = painelServico.listarSolicitacoesAtivas(idEntusiasta, 0, 5);

        assertThat(pagina.conteudo()).isEmpty();
        assertThat(pagina.totalElementos()).isZero();
        assertThat(pagina.totalPaginas()).isZero();
        assertThat(pagina.ultima()).isTrue();
    }

    @Test
    @DisplayName("painel consolidado: tamanho <= 0 vira o default de 5 (regra do envelope)")
    void painelConsolidadoTamanhoInvalidoViraDefault() {
        UUID idEntusiasta = UUID.randomUUID();
        // Sem stub de dataHoraSolicitacao: com um unico item o sort nao compara.
        prepararFontes(List.of(mock(ServicoRespostaDTO.class)), List.of());

        PaginaRespostaDTO<ServicoRespostaDTO> pagina = painelServico.listarSolicitacoesAtivas(idEntusiasta, 0, 0);

        assertThat(pagina.tamanho()).isEqualTo(5);
        assertThat(pagina.conteudo()).hasSize(1);
    }
}
