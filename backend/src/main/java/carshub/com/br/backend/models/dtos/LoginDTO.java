package carshub.com.br.backend.models.dtos;

import carshub.com.br.backend.models.enums.tiposUsuarios.TipoUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record LoginDTO(
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "Formato de e-mail inválido.")
        String login,

        @NotBlank(message = "A senha é obrigatória.")
        String senha,

        @NotNull(message = "O tipo de usuário selecionado é obrigatório.")
        TipoUsuario tipoUsuarioSelecionado // Vem do dropdown da tela de login
) {
}
