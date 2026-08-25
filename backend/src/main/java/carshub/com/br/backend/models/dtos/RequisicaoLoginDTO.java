package carshub.com.br.backend.models.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RequisicaoLoginDTO(
        @NotBlank(message = "É obrigatório informar o e-mail")
        @Email(message = "Formato inválido de e-mail")
        String login,

        @NotBlank(message = "A sernha é obrigatória")
        String senha
) {
}
