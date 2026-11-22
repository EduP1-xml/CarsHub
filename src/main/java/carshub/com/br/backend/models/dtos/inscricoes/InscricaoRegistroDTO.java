package carshub.com.br.backend.models.dtos.inscricoes;

import java.util.UUID;

/**
 * INVENTARIO DE ENDPOINTS (item 2): o id do evento deixou de vir no corpo e
 * passou para a propria rota POST /api/carshub/entusiastas/inscricoes/{idEvento}.
 */
public record InscricaoRegistroDTO(
        // Opcional: Se ele for um colecionador e quiser vincular um carro ao evento
        UUID idCarro
) {}
