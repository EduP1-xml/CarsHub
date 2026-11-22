package carshub.com.br.backend.models.dtos;

import jakarta.validation.constraints.NotBlank;

/**
 * SPRINT 6 (11.1): corpo do POST /autenticacao/refresh — renovacao do par de
 * tokens a partir do refresh token emitido no login.
 */
public record RenovacaoTokenDTO(
        @NotBlank(message = "O refresh token é obrigatório.")
        String refreshToken) {
}
