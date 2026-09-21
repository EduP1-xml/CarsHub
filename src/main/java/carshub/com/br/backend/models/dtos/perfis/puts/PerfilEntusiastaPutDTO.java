package carshub.com.br.backend.models.dtos.perfis.puts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PerfilEntusiastaDTO(
        @NotBlank(message = "O nome é obrigatório para a substituição completa.")
        @Size(max = 100) String nome,

        @NotBlank(message = "A senha é obrigatória para a substituição completa.")
        @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres") String senha,

        @NotBlank(message = "O telefone é obrigatório para a substituição completa.")
        @Size(max = 17) String telefone
) {
}
