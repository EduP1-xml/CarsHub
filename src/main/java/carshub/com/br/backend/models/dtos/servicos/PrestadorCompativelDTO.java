package carshub.com.br.backend.models.dtos.servicos;

import carshub.com.br.backend.models.enums.tiposUsuarios.TipoPrestadorServico;

import java.util.UUID;

/**
 * Um candidato do ranking de prestadores para um chamado.
 * Ordem garantida pelo service: notaMatch desc, notaAjustada desc,
 * distanciaKm asc e, empatando tudo, nome do prestador em ordem alfabetica.
 * Quem esta com a cota cheia (2 ativos) NAO aparece nesta lista.
 */
public record PrestadorCompativelDTO(
        UUID idPrestador,
        TipoPrestadorServico tipoPrestador,
        String nomePrestador,
        Double notaAjustada,
        Long quantidadeAvaliacoes,
        Double distanciaKm,
        Integer notaMatch
) {}