package carshub.com.br.backend.models.dtos;

public record ResetPasswordRequestDTO(String email, String codigo, String novaSenha) {
}
