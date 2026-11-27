package carshub.com.br.backend.models.dtos.clubes;

import java.util.UUID;

/**
 * Carro da garagem do usuario logado que atende ao gosto do clube
 * (GET /clubes/{id}/meus-carros-compativeis). vinculado = ja esta no clube.
 */
public record CarroCompativelDTO(
        UUID idCarro,
        String marca,
        String modelo,
        Integer ano,
        String cor,
        String fotoCapa,
        boolean vinculado
) {}
