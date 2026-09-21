package carshub.com.br.backend.models.dtos.inscricoes;

import carshub.com.br.backend.models.enums.inscricoes.StatusInscricao;
import java.time.LocalDateTime;
import java.util.UUID;

public record InscricaoResponseDTO(
        UUID idInscricao,
        String nomeEvento,
        String nomeEntusiasta,
        String carroVinculado, // Retorna a marca/modelo ou placa se houver
        StatusInscricao status,
        LocalDateTime dataInscricao
) {}