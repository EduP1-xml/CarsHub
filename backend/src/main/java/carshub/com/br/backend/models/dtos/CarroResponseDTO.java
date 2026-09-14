package carshub.com.br.backend.models.dtos;

import java.util.UUID;

public record CarroResponseDTO(
        UUID idCarro,
        String marca,
        String modelo,
        String placa,
        String nomeProprietario
) {
}
