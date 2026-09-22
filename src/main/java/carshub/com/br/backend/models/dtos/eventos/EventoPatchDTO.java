package carshub.com.br.backend.models.dtos.eventos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record EventoPatchDTO(
        String nome,
        String descricao,
        BigDecimal preco,
        Integer limiteParticipantes,
        String rua,
        String numero,
        String complemento,
        String bairro,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        List<String> urlFotoEvento
) {}