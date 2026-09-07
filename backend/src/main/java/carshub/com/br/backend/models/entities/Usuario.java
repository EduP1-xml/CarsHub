package carshub.com.br.backend.models.entities;


import carshub.com.br.backend.models.enums.TipoUsuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "Usuario")
@Inheritance(strategy = InheritanceType.JOINED) // Define a raiz da herança
@Data
@NoArgsConstructor
public abstract class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(java.sql.Types.BINARY)// troca a forma de armazenamento do UUID de varchar256 para binary16, poupando espaço no DB
    @Column(name = "id_Usuario", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @NotBlank(message = "O login/email não pode ser vazio")
    @Email(message = "Formato de e-mail inválido")
    @Size(min = 11,max = 45)
    @Column(name = "login_Usuario", length = 45, nullable = false, unique = true)
    private String login;

    @NotBlank(message = "A senha é obrigatória")
    @Size(min = 8, message = "A senha deve ter no mínimo 8 caracteres")
    @Column(name = "senha_Usuario", length = 64, nullable = false)
    private String senha;

    @NotBlank(message = "O nome é obrigatório")
    @Size(max = 100, message = "O nome não pode exceder 100 caracteres")
    @Column(name = "nome_Usuario", length = 100, nullable = false)
    private String nome;

    @NotNull(message = "A data de nascimento é obrigatória")
    @Column(name = "data_nascimento_Usuario", nullable = false)
    private LocalDate dataNascimento;

    @NotBlank(message = "O telefone é obrigatório")
    @Size(max = 17, message = "O telefone não pode exceder 17 caracteres")
    @Column(name = "telefone_Usuario", length = 17, nullable = false)
    private String telefone;

    @NotNull(message = "O tipo de usuário é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_Usuario", nullable = false)
    private TipoUsuario tipoUsuario;
}
