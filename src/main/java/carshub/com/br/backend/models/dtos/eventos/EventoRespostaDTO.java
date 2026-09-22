package carshub.com.br.backend.models.dtos.eventos;

import carshub.com.br.backend.models.enums.eventos.StatusEvento;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record EventoRespostaDTO(
        UUID idEvento,
        String nome,
        BigDecimal preco,
        String descricao,
        Integer limiteParticipantes,
        String rua,
        String numero,
        String complemento,
        String bairro,
        StatusEvento status,
        String nomeOrganizador,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim
) {}