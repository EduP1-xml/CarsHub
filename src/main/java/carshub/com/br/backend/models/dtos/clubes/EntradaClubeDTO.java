package carshub.com.br.backend.models.dtos.clubes;

import jakarta.validation.constraints.Size;

import java.util.Set;
import java.util.UUID;

/** Corpo OPCIONAL do "Entrar": carros da garagem a vincular ja na entrada. */
public record EntradaClubeDTO(
        @Size(max = 50, message = "Informe no máximo 50 carros")
        Set<UUID> idsCarros
) {}
