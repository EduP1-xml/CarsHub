package carshub.com.br.backend.models.dtos.perfis.puts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PerfiGuinchoPutDTO(
        @NotBlank(message = "O nome é obrigatório") @Size(max = 100) String nome,
        @NotBlank(message = "A senha é obrigatória") @Size(min = 8) String senha,
        @NotBlank(message = "O telefone é obrigatório") @Size(max = 17) String telefone,
        @NotBlank(message = "A placa é obrigatória") @Pattern(regexp = "^[A-Za-z0-9]{7}$") String placa
) {
}
