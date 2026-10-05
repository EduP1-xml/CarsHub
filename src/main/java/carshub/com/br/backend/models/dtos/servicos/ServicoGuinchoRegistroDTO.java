package carshub.com.br.backend.models.dtos.servicos;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Mesma prioridade de localizacao do servico mecanico:
 * latitude/longitude (GPS) > endereco digitado > localizacao do perfil.
 */
public record ServicoGuinchoRegistroDTO(
        @NotNull(message = "O ID do carro é obrigatório para solicitar o guincho.")
        UUID idCarro,

        Double latitude,
        Double longitude,

        String endereco
) {}