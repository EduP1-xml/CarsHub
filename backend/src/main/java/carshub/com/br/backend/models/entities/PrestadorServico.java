package carshub.com.br.backend.models.entities;


import carshub.com.br.backend.models.enums.TipoPrestadorServico;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import java.util.UUID;

@Entity
@Table(name = "PrestadorServico")
@Inheritance(strategy = InheritanceType.JOINED)
@Data
@NoArgsConstructor
public class PrestadorServico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(java.sql.Types.BINARY)
    @Column(name = "idPrestadorServico", updatable = false, nullable = false)
    private UUID id;

    @NotNull(message = "O tipo de prestador é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_prestador_servico", nullable = false)
    private TipoPrestadorServico tipoPrestadorServico;

    @NotBlank(message = "O telefone é obrigatório")
    @Size(max = 17, message = "O telefone não pode exceder 17 caracteres")
    @Column(name = "telefone", length = 17, nullable = false)
    private String telefone;

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(name = "Usuario_id_Usuario", referencedColumnName = "id_Usuario", nullable = false)
    private Usuario usuario;
}