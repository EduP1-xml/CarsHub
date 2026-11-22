package carshub.com.br.backend.models.dtos;

import carshub.com.br.backend.models.enums.tiposUsuarios.TipoPrestadorServico;
import carshub.com.br.backend.models.enums.tiposUsuarios.TipoUsuario;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

/**
 * SPRINT 6 (11.1): resposta do login e da renovacao de tokens.
 * Substitui a antiga resposta em texto do login (que devolvia o ID do usuario
 * para o front reenviar no header "user-id"). A identidade agora viaja no
 * accessToken (Authorization: Bearer ...) — o header user-id foi REMOVIDO (11.4).
 */
public record LoginResponseDTO(
        String accessToken,
        String refreshToken,
        String tipoToken,
        long expiraEmSegundos,
        UUID idUsuario,
        String nomeUsuario,
        TipoUsuario tipoUsuario,
        @Nullable TipoPrestadorServico tipoPrestadorServico) {
}
