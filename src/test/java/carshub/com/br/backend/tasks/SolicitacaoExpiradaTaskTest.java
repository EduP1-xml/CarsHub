package carshub.com.br.backend.tasks;

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
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SolicitacaoExpiradaTaskTest {

    @Mock
    private ServicoMecanicoRepository repositorioServicoMecanico;
    @Mock
    private ServicoGuinchoRepository repositorioServicoGuincho;

    @InjectMocks
    private SolicitacaoExpiradaTask task;

    @Test
    @DisplayName("remove SOLICITADO mais velho que o limite, um por um, sem tocar em FINALIZADO")
    void removeAbandonadosUmAUm() {
        ReflectionTestUtils.setField(task, "horasExpiracaoSolicitacao", 3L);

        ServicoMecanico abandonado1 = mock(ServicoMecanico.class);
        ServicoMecanico abandonado2 = mock(ServicoMecanico.class);
        when(repositorioServicoMecanico.findAllByStatusAndDataHoraSolicitacaoBefore(any(), any()))
                .thenReturn(List.of(abandonado1, abandonado2));
        when(repositorioServicoGuincho.findAllByStatusAndDataHoraSolicitacaoBefore(any(), any()))
                .thenReturn(List.of());

        task.removerSolicitacoesAbandonadas();

        verify(repositorioServicoMecanico).delete(abandonado1);
        verify(repositorioServicoMecanico).delete(abandonado2);
        verify(repositorioServicoGuincho, never()).delete(any(ServicoGuincho.class));
    }

    @Test
    @DisplayName("quando nao ha chamados abandonados nada e removido")
    void nadaParaRemover() {
        ReflectionTestUtils.setField(task, "horasExpiracaoSolicitacao", 3L);

        when(repositorioServicoMecanico.findAllByStatusAndDataHoraSolicitacaoBefore(any(), any()))
                .thenReturn(List.of());
        when(repositorioServicoGuincho.findAllByStatusAndDataHoraSolicitacaoBefore(any(), any()))
                .thenReturn(List.of());

        task.removerSolicitacoesAbandonadas();

        verify(repositorioServicoMecanico, never()).delete(any(ServicoMecanico.class));
        verify(repositorioServicoGuincho, never()).delete(any(ServicoGuincho.class));
    }

    @Test
    @DisplayName("busca usa o status SOLICITADO e o limite de horas configurado")
    void buscaComStatusSolicitado() {
        ReflectionTestUtils.setField(task, "horasExpiracaoSolicitacao", 3L);

        when(repositorioServicoMecanico.findAllByStatusAndDataHoraSolicitacaoBefore(any(), any()))
                .thenReturn(List.of());
        when(repositorioServicoGuincho.findAllByStatusAndDataHoraSolicitacaoBefore(any(), any()))
                .thenReturn(List.of());

        task.removerSolicitacoesAbandonadas();

        verify(repositorioServicoMecanico).findAllByStatusAndDataHoraSolicitacaoBefore(
                eq(carshub.com.br.backend.models.enums.servicos.StatusServico.SOLICITADO), any());
        verify(repositorioServicoGuincho).findAllByStatusAndDataHoraSolicitacaoBefore(
                eq(carshub.com.br.backend.models.enums.servicos.StatusServico.SOLICITADO), any());
    }
}