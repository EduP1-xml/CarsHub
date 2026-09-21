package carshub.com.br.backend.models.dtos.eventos.patches;

import java.math.BigDecimal;

public record EventoPatchDTO(
        String nome,
        String descricao,
        BigDecimal preco,
        Integer limiteParticipantes,
        String rua,
        String numero,
        String complemento,
        String bairro
) {}