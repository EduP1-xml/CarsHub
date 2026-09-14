package carshub.com.br.backend.models.dtos;

import carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados;
import carshub.com.br.backend.models.enums.tiposUsuarios.TipoPrestadorServico;
import carshub.com.br.backend.models.enums.tiposUsuarios.TipoUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;


public record CadastroDTO(
        @NotBlank(message = "O nome é obrigatório") String nome,
        @NotBlank(message = "O e-mail é obrigatório") @Email String login,
        @NotBlank(message = "A senha é obrigatória") String senha,
        @NotNull(message = "A data de nascimento é obrigatória") LocalDate dataNascimento,
        @NotNull(message = "O telefone é obrigatório") String telefone,
        @NotNull(message = "O tipo de usuário é obrigatório") TipoUsuario tipoUsuario,
        String cpf,
        String cnpj,
        String placa,
        TipoPrestadorServico tipoPrestadorServico,
        List<ServicosOfertados> servicosOfertados
) {}