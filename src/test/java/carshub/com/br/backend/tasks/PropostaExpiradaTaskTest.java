package carshub.com.br.backend.tasks;

import carshub.com.br.backend.services.servicos.ServicoGuinchoService;
import carshub.com.br.backend.services.servicos.ServicoMecanicoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PropostaExpiradaTaskTest {

    @Mock
    private ServicoMecanicoService servicoMecanico;
    @Mock
    private ServicoGuinchoService servicoGuincho;

    @InjectMocks
    private PropostaExpiradaTask task;

    @Test
    @DisplayName("delega a liberacao de propostas vencidas aos dois services com o prazo configurado")
    void liberaNosDoisServices() {
        ReflectionTestUtils.setField(task, "minutosExpiracaoProposta", 30);

        task.liberarPropostasVencidas();

        verify(servicoMecanico).liberarPropostasExpiradas(30);
        verify(servicoGuincho).liberarPropostasExpiradas(30);
    }
}