package carshub.com.br.backend.services.geocodificacao;

import carshub.com.br.backend.exceptions.FalhaGeocodificacaoException;
import carshub.com.br.backend.models.entities.ContadorGeocodificacao;
import carshub.com.br.backend.repositories.ContadorGeocodificacaoRepository;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Contador mensal de chamadas ao Google Geocoding (decisao A5).
 *
 * DESENHO: a contagem vive em MEMORIA durante a operacao (custo zero por
 * requisicao, sem lock e sem ida ao banco no meio da transacao do usuario) e e
 * PERSISTIDA na tabela ContadorGeocodificacao a cada hora, ao virar o mes e no
 * encerramento da aplicacao. Na subida, o valor do mes corrente e retomado do
 * banco, entao um restart nao zera a contagem.
 *
 * LIMITE CONHECIDO: entre duas gravacoes (ate 1 hora) uma queda abrupta de energia
 * perde a diferenca contada; e, com mais de uma instancia no ar, cada uma mantem a
 * sua contagem. Para o cenario atual (uma instancia) o resultado pratico e exato.
 *
 * O plano gratuito da Geocoding API da ~40.000 requisicoes por mes.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContadorCotaGeocodificacao {

    private final ContadorGeocodificacaoRepository repositorioContador;
    private final TransactionTemplate templateTransacao;

    private final AtomicLong requisicoesNoMes = new AtomicLong();

    private volatile String mesCarregado = null;
    private volatile boolean avisoDe80PorCentoEmitido = false;
    private volatile boolean avisoDeLimiteEmitido = false;

    @Value("${app.geocodificacao.limiteMensalRequisicoes:40000}")
    private long limiteMensalRequisicoes;

    @Value("${app.geocodificacao.bloquearAoExcederLimite:false}")
    private boolean bloquearAoExcederLimite;

    // ==================================================================
    // USO DURANTE AS REQUISICOES (so memoria)
    // ==================================================================

    /** Chamado antes de cada requisicao ao Google. Nao toca no banco. */
    public void registrarRequisicao() {
        virarMesSeNecessario();

        long total = requisicoesNoMes.incrementAndGet();

        if (bloquearAoExcederLimite && total > limiteMensalRequisicoes) {
            throw new FalhaGeocodificacaoException(
                    "A cota mensal de geocodificação foi atingida (" + limiteMensalRequisicoes
                            + " requisições). Envie latitude e longitude do GPS enquanto a cota não renova.");
        }

        alertarUso(total);
    }

    public long obterRequisicoesNoMes() {
        return requisicoesNoMes.get();
    }

    public long obterLimiteMensal() {
        return limiteMensalRequisicoes;
    }

    // ==================================================================
    // PERSISTENCIA
    // ==================================================================

    /** Retoma a contagem do mes corrente quando a aplicacao sobe (schema ja criado). */
    @EventListener(ApplicationReadyEvent.class)
    public void carregarDoBanco() {
        String mes = mesCorrente();

        try {
            repositorioContador.findByMesReferencia(mes).ifPresentOrElse(
                    contador -> {
                        requisicoesNoMes.set(contador.getQuantidadeRequisicoes());
                        log.info("Cota de geocodificação retomada: {} de {} requisições já usadas em {}.",
                                contador.getQuantidadeRequisicoes(), limiteMensalRequisicoes, mes);
                    },
                    () -> log.info("Cota de geocodificação iniciada para o mês {} (limite de {} requisições).",
                            mes, limiteMensalRequisicoes));
        }
        catch (RuntimeException erro) {
            log.warn("Não foi possível retomar a cota de geocodificação do banco: {}", erro.getMessage());
        }

        mesCarregado = mes;
    }

    /** Grava a contagem a cada hora (minuto 0). */
    @Scheduled(cron = "0 0 * * * *")
    public void sincronizarComBanco() {
        persistirContagem(mesCorrente(), requisicoesNoMes.get(), "sincronização horária");
    }

    /** Ultima gravacao antes de encerrar (reduz a perda a um intervalo minimo). */
    @PreDestroy
    public void persistirAoEncerrar() {
        persistirContagem(mesCorrente(), requisicoesNoMes.get(), "encerramento da aplicação");
    }

    private void persistirContagem(String mesReferencia, long quantidade, String motivo) {
        try {
            templateTransacao.execute(status -> {
                ContadorGeocodificacao contador = repositorioContador.findByMesReferencia(mesReferencia)
                        .orElseGet(() -> novoContador(mesReferencia));

                contador.setQuantidadeRequisicoes(quantidade);
                contador.setDataHoraAtualizacao(LocalDateTime.now());
                repositorioContador.save(contador);
                return null;
            });

            log.debug("Cota de geocodificação persistida ({}): {} requisições em {}.",
                    motivo, quantidade, mesReferencia);
        }
        catch (RuntimeException erro) {
            // Nunca deixar a persistencia da cota derrubar a aplicacao.
            log.warn("Não foi possível persistir a cota de geocodificação ({}): {}", motivo, erro.getMessage());
        }
    }

    private ContadorGeocodificacao novoContador(String mesReferencia) {
        ContadorGeocodificacao contador = new ContadorGeocodificacao();
        contador.setMesReferencia(mesReferencia);
        contador.setQuantidadeRequisicoes(0L);
        return contador;
    }

    // ==================================================================
    // VIRADA DE MES E ALERTAS
    // ==================================================================

    private void virarMesSeNecessario() {
        String mes = mesCorrente();

        if (!mes.equals(mesCarregado)) {
            synchronized (this) {
                if (!mes.equals(mesCarregado)) {
                    // Fecha o mes anterior no banco antes de zerar a contagem.
                    if (mesCarregado != null) {
                        persistirContagem(mesCarregado, requisicoesNoMes.get(), "virada de mês");
                    }

                    mesCarregado = mes;
                    requisicoesNoMes.set(0);
                    avisoDe80PorCentoEmitido = false;
                    avisoDeLimiteEmitido = false;
                }
            }
        }
    }

    private void alertarUso(long total) {
        long limiteDeAviso = (long) (limiteMensalRequisicoes * 0.8);

        if (!avisoDe80PorCentoEmitido && total >= limiteDeAviso) {
            avisoDe80PorCentoEmitido = true;
            log.warn("Geocodificação: {} de {} requisições mensais já foram usadas.",
                    total, limiteMensalRequisicoes);
        }

        if (!avisoDeLimiteEmitido && total >= limiteMensalRequisicoes) {
            avisoDeLimiteEmitido = true;
            log.error("Geocodificação: cota mensal de {} requisições ESGOTADA. "
                    + "Novas chamadas tendem a falhar até a renovação.", limiteMensalRequisicoes);
        }
    }

    private String mesCorrente() {
        return YearMonth.now().toString();
    }
}