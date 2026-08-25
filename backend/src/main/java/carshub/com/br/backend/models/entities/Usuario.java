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

import java.util.UUID;

@Entity
@Table(name = "Usuario")
@Data
@NoArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(java.sql.Types.BINARY)
    @Column(name = "id_Usuario", updatable = false, nullable = false)
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

    @NotNull(message = "O tipo de usuário é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_Usuario", nullable = false)
    private TipoUsuario tipoUsuario;
}
