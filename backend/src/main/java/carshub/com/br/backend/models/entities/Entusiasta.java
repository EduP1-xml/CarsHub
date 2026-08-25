package carshub.com.br.backend.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import java.util.UUID;

@Entity
@Table(name = "Entusiasta")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@NoArgsConstructor
public class Entusiasta {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(java.sql.Types.BINARY)
    @Column(name = "idEntusiasta", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "O CPF é obrigatório")
    @Pattern(regexp = "\\d{11}", message = "O CPF deve conter exatamente 11 dígitos numéricos")
    @Column(name = "cpf_entusiasta", length = 11, unique = true)
    private String cpf;

    @NotNull(message = "É necessário informar se possui carros")
    @Column(name = "possui_carros", nullable = false)
    private Boolean possuiCarros;

    @OneToOne
    @JoinColumn(name = "Usuario_id_Usuario", referencedColumnName = "id_Usuario", nullable = false)
    private Usuario usuario;
}