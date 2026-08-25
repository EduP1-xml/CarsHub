package carshub.com.br.backend.models.entities;


import carshub.com.br.backend.models.enums.TipoCambio;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "Carro")
@Data
@NoArgsConstructor
public class Carro {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(java.sql.Types.BINARY)
    @Column(name = "idCarro", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "A marca é obrigatória")
    @Size(max = 30)
    @Column(name = "marca_carro", length = 30, nullable = false)
    private String marca;

    @NotBlank(message = "O modelo é obrigatório")
    @Size(max = 30)
    @Column(name = "modelo_carro", length = 30, nullable = false)
    private String modelo;

    @NotNull(message = "O número de portas é obrigatório")
    @Min(value = 1, message = "O número mínimo de portas é 1")
    @Max(value = 5, message = "O número máximo de portas é 5")
    @Column(name = "n_portas_carro", nullable = false)
    private Integer numeroPortas;

    @NotNull(message = "A litragem do motor é obrigatória")
    @DecimalMin(value = "0.049", message = "A litragem mínima é 0,049 L")
    @DecimalMax(value = "15.000", message = "A litragem máxima é 15,000 L")
    @Column(name = "litragem_motor_carro", precision = 5, scale = 3, nullable = false)
    private BigDecimal litragemMotor;

    @NotNull(message = "O tipo de câmbio é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "cambio_carro", nullable = false)
    private TipoCambio cambio;

    @NotBlank(message = "A placa é obrigatória")
    @Size(min = 7, max = 7, message = "A placa deve ter exatamente 7 caracteres")
    @Column(name = "placa_carro", length = 7, unique = true, nullable = false)
    private String placa;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "Entusiasta_idEntusiasta", referencedColumnName = "idEntusiasta", nullable = false)
    private Entusiasta entusiasta;
}
