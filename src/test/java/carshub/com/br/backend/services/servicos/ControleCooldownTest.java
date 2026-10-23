package carshub.com.br.backend.services.servicos;

import carshub.com.br.backend.exceptions.RegraDeNegocioException;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.Guincho;
import carshub.com.br.backend.models.entities.Mecanico;
import carshub.com.br.backend.repositories.EntusiastaRepository;
import carshub.com.br.backend.repositories.GuinchoRepository;
import carshub.com.br.backend.repositories.MecanicoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ControleCooldownTest {

    @Mock
    private EntusiastaRepository repositorioEntusiasta;
    @Mock
    private MecanicoRepository repositorioMecanico;
    @Mock
    private GuinchoRepository repositorioGuincho;

    @InjectMocks
    private ControleCooldown controleCooldown;

    @Test
    @DisplayName("colecionador sem ultima solicitacao registrada esta liberado")
    void solicitacaoSemHistoricoLibera() {
        Entusiasta entusiasta = mock(Entusiasta.class);
        when(entusiasta.getDataHoraUltimaSolicitacaoServico()).thenReturn(null);

        assertThatCode(() -> controleCooldown.validarSolicitacaoDoColecionador(entusiasta))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("colecionador dentro dos 5 minutos e bloqueado com mensagem em minutos")
    void solicitacaoDentroDaJanelaBloqueia() {
        Entusiasta entusiasta = mock(Entusiasta.class);
        when(entusiasta.getDataHoraUltimaSolicitacaoServico()).thenReturn(LocalDateTime.now().minusMinutes(1));

        assertThatThrownBy(() -> controleCooldown.validarSolicitacaoDoColecionador(entusiasta))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Aguarde")
                .hasMessageContaining("minutos");
    }

    @Test
    @DisplayName("colecionador fora dos 5 minutos esta liberado")
    void solicitacaoForaDaJanelaLibera() {
        Entusiasta entusiasta = mock(Entusiasta.class);
        when(entusiasta.getDataHoraUltimaSolicitacaoServico()).thenReturn(LocalDateTime.now().minusMinutes(6));

        assertThatCode(() -> controleCooldown.validarSolicitacaoDoColecionador(entusiasta))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("mecanico dentro de 1 minuto entre propostas e bloqueado em segundos")
    void propostaDentroDaJanelaBloqueia() {
        Mecanico mecanico = mock(Mecanico.class);
        when(mecanico.getDataHoraUltimaProposta()).thenReturn(LocalDateTime.now().minusSeconds(30));

        assertThatThrownBy(() -> controleCooldown.validarPropostaDoMecanico(mecanico))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Aguarde")
                .hasMessageContaining("segundos");
    }

    @Test
    @DisplayName("guincho dentro de 1 minuto entre propostas e bloqueado")
    void propostaGuinchoDentroDaJanelaBloqueia() {
        Guincho guincho = mock(Guincho.class);
        when(guincho.getDataHoraUltimaProposta()).thenReturn(LocalDateTime.now().minusSeconds(10));

        assertThatThrownBy(() -> controleCooldown.validarPropostaDoGuincho(guincho))
                .isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("Aguarde");
    }

    @Test
    @DisplayName("mecanico fora de 1 minuto esta liberado")
    void propostaForaDaJanelaLibera() {
        Mecanico mecanico = mock(Mecanico.class);
        when(mecanico.getDataHoraUltimaProposta()).thenReturn(LocalDateTime.now().minusMinutes(2));

        assertThatCode(() -> controleCooldown.validarPropostaDoMecanico(mecanico))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("registrar solicitacao grava o horario no entusiasta e salva no banco")
    void registrarSolicitacaoGravaNoBanco() {
        Entusiasta entusiasta = mock(Entusiasta.class);

        controleCooldown.registrarSolicitacaoDoColecionador(entusiasta);

        verify(entusiasta).setDataHoraUltimaSolicitacaoServico(any(LocalDateTime.class));
        verify(repositorioEntusiasta).save(entusiasta);
    }

    @Test
    @DisplayName("registrar proposta grava o horario no prestador e salva no banco")
    void registrarPropostaGravaNoBanco() {
        Mecanico mecanico = mock(Mecanico.class);
        Guincho guincho = mock(Guincho.class);

        controleCooldown.registrarPropostaDoMecanico(mecanico);
        controleCooldown.registrarPropostaDoGuincho(guincho);

        verify(mecanico).setDataHoraUltimaProposta(any(LocalDateTime.class));
        verify(repositorioMecanico).save(mecanico);
        verify(guincho).setDataHoraUltimaProposta(any(LocalDateTime.class));
        verify(repositorioGuincho).save(guincho);
    }
}