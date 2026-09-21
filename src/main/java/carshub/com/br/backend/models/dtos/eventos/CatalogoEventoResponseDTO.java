package carshub.com.br.backend.models.dtos.eventos;

import java.math.BigDecimal;
import java.util.UUID;

public record CatalogoEventoResponseDTO(
        UUID idEvento,
        String nome,
        String descricao,
        BigDecimal preco,
        String rua,
        String numero,
        String bairro,
        String nomeOrganizador,
        Integer vagasDisponiveis,
        Boolean inscrito
) {}