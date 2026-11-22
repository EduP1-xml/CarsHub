package carshub.com.br.backend.services.seguranca;

import java.time.Clock;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * SPRINT 6 (11.8): rate limiting por CHAVE (IP do cliente) com JANELA
 * DESLIZANTE em memoria. Cada chave guarda os instantes das requisicoes
 * aceitas; a cada pedido os eventos mais velhos que a janela saem da fila e o
 * pedido so passa se ainda houver vaga abaixo do limite.
 *
 * E propositalmente em memoria (sem banco): e um controle de abuso, nao uma
 * garantia transacional — num cenario multi-instancia cada no mantem a sua
 * janela (documentado no handoff; escalar exige um limite distribuido, ex.
 * Redis). O Clock injetado permite testar o tempo sem dormir.
 */
public class ControleRateLimit {

    private final Clock relogio;
    private final ConcurrentMap<String, Deque<Long>> janelasPorChave = new ConcurrentHashMap<>();

    public ControleRateLimit(Clock relogio) {
        this.relogio = relogio;
    }

    /**
     * @return true quando a requisicao foi aceita (e registrada) ou false
     *         quando o limite da janela ja foi atingido.
     */
    public boolean permitir(String chave, int limite, Duration janela) {
        Deque<Long> eventos = janelasPorChave.computeIfAbsent(chave, k -> new ArrayDeque<>());

        synchronized (eventos) {
            long agora = relogio.millis();
            long limiteInferior = agora - janela.toMillis();

            while (!eventos.isEmpty() && eventos.peekFirst() <= limiteInferior) {
                eventos.pollFirst();
            }

            if (eventos.size() >= limite) {
                return false;
            }

            eventos.addLast(agora);
            return true;
        }
    }

    /** Zera todas as janelas (uso em testes). */
    public void limpar() {
        janelasPorChave.clear();
    }
}
