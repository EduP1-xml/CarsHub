package carshub.com.br.backend.services.servicos;

import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.models.dtos.servicos.PrestadorCompativelDTO;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
        verify(servicoGuincho, never()).listarPrestadoresCompativeis(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
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
}