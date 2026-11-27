package carshub.com.br.backend.models.dtos.clubes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Clube visto pelo usuario logado (souMembro/souAdministrador dependem dele).
 * carrosDesvinculados: so e diferente de 0 na resposta da edicao, quando o
 * novo filtro tirou carros que ja nao atendiam ao gosto do clube.
 */
public record ClubeRespostaDTO(
        UUID idClube,
        String nome,
        String descricao,
        UUID idAdministrador,
        String nomeAdministrador,
        long quantidadeMembros,
        long quantidadeCarrosVinculados,
        boolean souMembro,
        boolean souAdministrador,
        FiltroCarroDTO filtro,
        List<String> fotosClube,
        LocalDateTime dataCriacao,
        int carrosDesvinculados
) {}
