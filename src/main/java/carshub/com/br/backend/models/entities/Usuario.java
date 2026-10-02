package carshub.com.br.backend.models.entities;

import carshub.com.br.backend.models.enums.tiposUsuarios.TipoUsuario;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "Usuario")
@Inheritance(strategy = InheritanceType.JOINED) // Define a raiz da herança
@Data
@NoArgsConstructor
public class Usuario implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(java.sql.Types.BINARY)
    @Column(name = "id_Usuario", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @NotBlank(message = "O login/email não pode ser vazio")
    @Email(message = "Formato de e-mail inválido")
    @Size(min = 11, max = 45)
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
    @Column(name = "dataNascimento_Usuario", nullable = false)
    private LocalDate dataNascimento;

    @NotBlank(message = "O telefone é obrigatório")
    @Size(max = 17, message = "O telefone não pode exceder 17 caracteres")
    @Column(name = "telefone_Usuario", length = 17, nullable = false)
    private String telefone;

    @NotNull(message = "O tipo de usuário é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipoUsuario_Usuario", nullable = false)
    private TipoUsuario tipoUsuario;

    @CreationTimestamp
    @Column(name = "dataHora_Usuario", nullable = false, updatable = false, columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private LocalDateTime dataHora;

    @Column(name = "ativo_Usuario")
    private Boolean ativo;

    @Column(name = "urlFotoPerfil_Usuario")
    private String urlFotoPerfil;

    // --- Codigo de ativacao de conta ---
    @JsonIgnore
    @Column(name = "codigoVerificacao_Usuario")
    private String codigoVerificacao;

    @JsonIgnore
    @Column(name = "expiracaoCodigoVerificacao_Usuario")
    private LocalDateTime expiracaoCodigoVerificacao;

    @JsonIgnore
    @Column(name = "dataEnvioCodigoVerificacao_Usuario")
    private LocalDateTime dataEnvioCodigoVerificacao;

    @JsonIgnore
    @Column(name = "codigoRedefinicaoSenha_Usuario")
    private String codigoRedefinicaoSenha;

    @JsonIgnore
    @Column(name = "expiracaoCodigoRedefinicaoSenha_Usuario")
    private LocalDateTime expiracaoCodigoRedefinicaoSenha;

    @Override
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of();
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public @Nullable String getPassword() {
        return this.senha;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public String getUsername() {
        return this.login;
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public boolean isAccountNonExpired() {
        return UserDetails.super.isAccountNonExpired();
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public boolean isAccountNonLocked() {
        return UserDetails.super.isAccountNonLocked();
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public boolean isCredentialsNonExpired() {
        return UserDetails.super.isCredentialsNonExpired();
    }

    @Override
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    public boolean isEnabled() {
        return UserDetails.super.isEnabled();
    }
}