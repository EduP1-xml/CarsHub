package carshub.com.br.backend.services.servicos;

import carshub.com.br.backend.exceptions.RegraDeNegocioException;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.Guincho;
import carshub.com.br.backend.models.entities.Mecanico;
import carshub.com.br.backend.repositories.EntusiastaRepository;
import carshub.com.br.backend.repositories.GuinchoRepository;
import carshub.com.br.backend.repositories.MecanicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;

/**
 * Rate limiting dos fluxos de servico,o horario fica no BANCO
 * (nao em memoria), entao sobrevive a restart e vale para qualquer instancia.
 *
 *   Colecionador -> 1 solicitacao a cada 5 minutos (mecanico e guincho juntos);
 *   Mecanico/Guincho -> 1 proposta a cada 1 minuto.
 */
@Service
@RequiredArgsConstructor
public class ControleCooldown {

    private static final long minutosEntreSolicitacoes = 5;
    private static final long minutosEntrePropostas = 1;
    private static final long segundosPorMinuto = 60;

    private final EntusiastaRepository repositorioEntusiasta;
    private final MecanicoRepository repositorioMecanico;
    private final GuinchoRepository repositorioGuincho;

    // ---------------- Colecionador ----------------

    public void validarSolicitacaoDoColecionador(Entusiasta entusiasta) {
        aplicarCooldown(entusiasta.getDataHoraUltimaSolicitacaoServico(),
                minutosEntreSolicitacoes,
                "Aguarde",
                "para solicitar um novo serviço.");
    }

    public void registrarSolicitacaoDoColecionador(Entusiasta entusiasta) {
        entusiasta.setDataHoraUltimaSolicitacaoServico(LocalDateTime.now());
        repositorioEntusiasta.save(entusiasta);
    }

    // ---------------- Mecanico ----------------

    public void validarPropostaDoMecanico(Mecanico mecanico) {
        aplicarCooldown(mecanico.getDataHoraUltimaProposta(),
                minutosEntrePropostas,
                "Aguarde",
                "para enviar uma nova proposta.");
    }

    public void registrarPropostaDoMecanico(Mecanico mecanico) {
        mecanico.setDataHoraUltimaProposta(LocalDateTime.now());
        repositorioMecanico.save(mecanico);
    }

    // ---------------- Guincho ----------------

    public void validarPropostaDoGuincho(Guincho guincho) {
        aplicarCooldown(guincho.getDataHoraUltimaProposta(),
                minutosEntrePropostas,
                "Aguarde",
                "para enviar uma nova proposta.");
    }

    public void registrarPropostaDoGuincho(Guincho guincho) {
        guincho.setDataHoraUltimaProposta(LocalDateTime.now());
        repositorioGuincho.save(guincho);
    }

    // ---------------- Regra comum ----------------

    private void aplicarCooldown(LocalDateTime ultimoEnvio, long minutos, String prefixo, String sufixo) {
        if (ultimoEnvio == null) {
            return;
        }

        LocalDateTime liberadoEm = ultimoEnvio.plusMinutes(minutos);

        if (LocalDateTime.now().isBefore(liberadoEm)) {
            long segundosRestantes = Duration.between(LocalDateTime.now(), liberadoEm).toSeconds() + 1;
            throw new RegraDeNegocioException(
                    prefixo + " " + formatarTempoDeEspera(segundosRestantes) + " " + sufixo);
        }
    }

    private String formatarTempoDeEspera(long segundos) {
        if (segundos >= segundosPorMinuto) {
            long minutos = (segundos + segundosPorMinuto - 1) / segundosPorMinuto;
            return minutos + (minutos == 1 ? " minuto" : " minutos");
        }

        return segundos + (segundos == 1 ? " segundo" : " segundos");
    }
}