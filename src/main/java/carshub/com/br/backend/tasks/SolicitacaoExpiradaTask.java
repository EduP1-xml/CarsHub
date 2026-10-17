package carshub.com.br.backend.tasks;

import carshub.com.br.backend.models.entities.servicos.ServicoGuincho;
import carshub.com.br.backend.models.entities.servicos.ServicoMecanico;
import carshub.com.br.backend.models.enums.servicos.StatusServico;
import carshub.com.br.backend.repositories.servicos.ServicoGuinchoRepository;
import carshub.com.br.backend.repositories.servicos.ServicoMecanicoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Limpeza das solicitacoes abandonadas: chamados que ficaram
 * SOLICITADO por mais de 3 horas sao removidos, para a rede nao acumular
 * chamados fantasmas que ninguem vai atender.
 *
 * Roda a cada hora. A remocao e feita ENTIDADE POR ENTIDADE (e nao por DELETE em
 * massa) porque ServicoMecanico tem uma @ElementCollection de servicos: o DELETE
 * em massa do JPQL nao apaga as linhas da tabela auxiliar e deixaria lixo.
 *
 * NAO toca em FINALIZADO: esse e o historico de manutencoes do veiculo e a base
 * das avaliacoes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SolicitacaoExpiradaTask {

    private final ServicoMecanicoRepository repositorioServicoMecanico;
    private final ServicoGuinchoRepository repositorioServicoGuincho;

    /** Tempo maximo que um chamado pode ficar aberto na rede, em horas. */
    @Value("${app.servicos.horasExpiracaoSolicitacao:3}")
    private long horasExpiracaoSolicitacao;

    /** Roda no minuto 0 de cada hora. */
    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void removerSolicitacoesAbandonadas() {
        LocalDateTime limite = LocalDateTime.now().minusHours(horasExpiracaoSolicitacao);

        int mecanicosRemovidos = removerServicosMecanicos(limite);
        int guinchosRemovidos = removerServicosDeGuincho(limite);

        if (mecanicosRemovidos > 0 || guinchosRemovidos > 0) {
            log.info("Solicitações abandonadas removidas (SOLICITADO há mais de {} horas): {} mecânica(s), {} guincho(s).",
                    horasExpiracaoSolicitacao, mecanicosRemovidos, guinchosRemovidos);
        }
    }

    private int removerServicosMecanicos(LocalDateTime limite) {
        List<ServicoMecanico> abandonados = repositorioServicoMecanico
                .findAllByStatusAndDataHoraSolicitacaoBefore(StatusServico.SOLICITADO, limite);

        for (ServicoMecanico servico : abandonados) {
            repositorioServicoMecanico.delete(servico);
        }

        return abandonados.size();
    }

    private int removerServicosDeGuincho(LocalDateTime limite) {
        List<ServicoGuincho> abandonados = repositorioServicoGuincho
                .findAllByStatusAndDataHoraSolicitacaoBefore(StatusServico.SOLICITADO, limite);

        for (ServicoGuincho servico : abandonados) {
            repositorioServicoGuincho.delete(servico);
        }

        return abandonados.size();
    }
}