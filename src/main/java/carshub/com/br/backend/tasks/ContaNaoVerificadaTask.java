package carshub.com.br.backend.tasks;

import carshub.com.br.backend.models.entities.Usuario;
import carshub.com.br.backend.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Limpeza diaria de contas criadas e nunca confirmadas.
 *
 * Uma conta so vira ativo = true quando o usuario confirma o codigo enviado por
 * e-mail. Contas que nunca confirmam permanecem ativo = false indefinidamente,
 * o que polui a base e permite que alguem verifique quais e-mails estao
 * cadastrados (tentando se cadastrar e recebendo 409).
 *
 * A remocao e feita entidade por entidade, e nao por DELETE em massa, porque a
 * hierarquia de Usuario usa JOINED: o Hibernate precisa apagar as linhas das
 * tabelas filhas (Entusiasta, Colecionador, Carro, etc.) na ordem correta.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContaNaoVerificadaTask {

    private final UsuarioRepository repositorioUsuario;

    /** Tempo de retencao, em dias, de uma conta nao confirmada. */
    @Value("${app.cadastro.diasRetencaoContaNaoVerificada:7}")
    private long diasRetencao;

    /**
     * Executa todos os dias as 03:00 (horario do servidor).
     * Formato do cron: segundo minuto hora dia-do-mes mes dia-da-semana
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void removerContasNaoVerificadas() {
        LocalDateTime limite = LocalDateTime.now().minusDays(diasRetencao);

        List<Usuario> contasPendentes = repositorioUsuario.findAllByAtivoFalseAndDataHoraBefore(limite);

        if (contasPendentes.isEmpty()) {
            log.info("Limpeza de contas não verificadas: nenhuma conta a remover.");
            return;
        }

        int removidas = 0;
        int falhas = 0;

        for (Usuario conta : contasPendentes) {
            try {
                repositorioUsuario.delete(conta);
                repositorioUsuario.flush();
                removidas++;
                log.info("Conta não verificada removida: {} (criada em {})",
                        conta.getLogin(), conta.getDataHora());
            }
            catch (Exception e) {
                // Uma falha individual nao pode impedir a limpeza das demais.
                falhas++;
                log.warn("Não foi possível remover a conta não verificada {}: {}",
                        conta.getLogin(), e.getMessage());
            }
        }

        log.info("Limpeza de contas não verificadas concluída: {} removida(s), {} falha(s), limite de {} dia(s).",
                removidas, falhas, diasRetencao);
    }
}