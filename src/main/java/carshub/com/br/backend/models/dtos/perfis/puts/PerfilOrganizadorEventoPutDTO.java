package carshub.com.br.backend.models.dtos.perfis.puts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PerfilOrganizadorEventoPutDTO(
        @NotBlank(message = "O nome é obrigatório para a substituição completa.")
        @Size(max = 100) String nome,

        @NotBlank(message = "O telefone é obrigatório para a substituição completa.")
        @Size(max = 17) String telefone
) {
}