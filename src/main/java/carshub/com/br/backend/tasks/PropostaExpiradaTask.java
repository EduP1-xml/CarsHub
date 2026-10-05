package carshub.com.br.backend.tasks;

import carshub.com.br.backend.services.servicos.ServicoGuinchoService;
import carshub.com.br.backend.services.servicos.ServicoMecanicoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Devolve para a rede os chamados que ficaram em PROPOSTA sem resposta do
 * colecionador. Sem isso, uma proposta esquecida bloquearia o chamado para
 * sempre: nenhum outro prestador conseguiria propor.
 *
 * O prazo e configuravel em app.servicos.minutosExpiracaoProposta (padrao 30).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PropostaExpiradaTask {

    private final ServicoMecanicoService servicoMecanico;
    private final ServicoGuinchoService servicoGuincho;

    /** Tempo maximo de vida de uma proposta sem resposta, em minutos. */
    @Value("${app.servicos.minutosExpiracaoProposta:30}")
    private int minutosExpiracaoProposta;

    /**
     * Roda a cada minuto (o prazo e de 30, entao o desvio maximo e de 1 minuto).
     * Formato do cron: segundo minuto hora dia-do-mes mes dia-da-semana
     */
    @Scheduled(cron = "0 * * * * *")
    public void liberarPropostasVencidas() {
        int propostasMecanicas = servicoMecanico.liberarPropostasExpiradas(minutosExpiracaoProposta);
        int propostasDeGuincho = servicoGuincho.liberarPropostasExpiradas(minutosExpiracaoProposta);

        if (propostasMecanicas > 0 || propostasDeGuincho > 0) {
            log.info("Propostas expiradas devolvidas para a rede (limite de {} minutos): {} mecânica(s), {} guincho(s).",
                    minutosExpiracaoProposta, propostasMecanicas, propostasDeGuincho);
        }
    }
}