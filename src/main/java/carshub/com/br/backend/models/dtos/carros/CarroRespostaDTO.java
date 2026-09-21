package carshub.com.br.backend.models.dtos.carros;

import java.util.UUID;

public record CarroRespostaDTO(
        UUID idCarro,
        String marca,
        String modelo,
        String placa,
        String nomeProprietario
) {
}
