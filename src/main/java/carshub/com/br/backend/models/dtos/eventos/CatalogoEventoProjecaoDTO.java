package carshub.com.br.backend.models.dtos.eventos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * AJUSTE 1.1b: projecao de leitura do catalogo — os escalares que o Page&lt;T&gt;
 * traz direto do SQL, antes de o CatalogoEventoService completar vagasDisponiveis
 * e a flag inscrito (que dependem de agregados em outra tabela).
 * Nao e contrato de API: nunca sai como resposta.
 */
public record CatalogoEventoProjecaoDTO(
        UUID idEvento,
        String nome,
        String descricao,
        BigDecimal preco,
        String rua,
        String numero,
        String bairro,
        Double latitude,
        Double longitude,
        String nomeOrganizador,
        Integer limiteParticipantes,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim
) {}
