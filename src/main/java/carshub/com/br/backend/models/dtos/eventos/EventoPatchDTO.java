package carshub.com.br.backend.models.dtos.eventos;

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