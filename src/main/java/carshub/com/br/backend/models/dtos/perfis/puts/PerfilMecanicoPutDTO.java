package carshub.com.br.backend.models.dtos.perfis.puts;

import carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record PerfilMecanicoDTO(
        @NotBlank(message = "O nome é obrigatório") @Size(max = 100) String nome,
        @NotBlank(message = "A senha é obrigatória") @Size(min = 8) String senha,
        @NotBlank(message = "O telefone é obrigatório") @Size(max = 17) String telefone,
        @NotEmpty(message = "A lista de serviços ofertados não pode ser vazia") List<ServicosOfertados> servicosOfertados
) {
}
