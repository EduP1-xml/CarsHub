package carshub.com.br.backend.models.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Usado pelo botao "Reenviar codigo" do front, enquanto a conta ainda nao foi
 * confirmada. A identificacao e pelo e-mail porque o usuario ainda nao tem sessao.
 */
public record ReenvioCodigoDTO(
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Formato de e-mail inválido.")
        String email
) {}