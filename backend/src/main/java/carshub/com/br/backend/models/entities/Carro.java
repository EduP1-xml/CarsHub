package carshub.com.br.backend.models.entities;


import carshub.com.br.backend.models.enums.carros.*;
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
    @Column(name = "id_Carro", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "A marca é obrigatória")
    @Size(max = 30)
    @Column(name = "marca_Carro", length = 30, nullable = false)
    private String marca;

    @NotBlank(message = "O modelo é obrigatório")
    @Size(max = 30) //definir um valores para texto(tem atributo para validar mínimo e máximo) validado na compilação do código
    @Column(name = "modelo_Carro", length = 30, nullable = false)
    private String modelo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "carroceria_Carro", nullable = false)
    private TipoCarroceria carroceria;

    @NotNull(message = "O ano é obrigatório")
    @Min(value = 1886, message = "O ano mínimo que seu carro pode ser é 1886, quando o primeiro carro do mundo foi feito")
    @Column(name = "Ano", length = 4, nullable = false)
    private Integer ano;

    @NotBlank(message = "A cor é obrigatório")
    @Size(max = 20)
    @Column(name = "cor_Carro", length = 20, nullable = false)
    private String cor;

    @NotNull(message = "O número de cilindros é obrigatório")
    @Min(value = 1, message = "O número mínimo de portas é 1")
    @Max(value = 16, message = "O número máximo de portas é 16")
    @Column(name = "quantidadeCilindros_Carro")
    private Integer quantidadeCilindros;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "motor_Carro", nullable = false)
    private TipoMotor motor;

    @NotNull(message = "A litragem do motor é obrigatória")
    @DecimalMin(value = "0.049", message = "A litragem mínima é 0,049 L")// definir um valor mínimo para números validado na compilação do código
    @DecimalMax(value = "15.000", message = "A litragem máxima é 15,000 L")// definir um valor máximo para números validado na compilação do código
    @Column(name = "litragemMotor_Carro", precision = 5, scale = 3, nullable = false)
    private BigDecimal litragemMotor;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "injecao_Carro", nullable = false)
    private TipoInjecao injecao;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "alimentacao_Carro", nullable = false)
    private TipoAlimentacao alimentacao;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "combustivel_Carro", nullable = false)
    private TipoCombustivel combustivel;

    @NotNull(message = "O tipo de câmbio é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "cambio_Carro", nullable = false)
    private TipoCambio cambio;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "tracao_Carro", nullable = false)
    private TipoTracao tracao;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "suspencao_Carro", nullable = false)
    private TipoSuspencao suspencao;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "freio_Carro", nullable = false)
    private TipoFreio freio;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "direcao_Carro", nullable = false)
    private TipoDirecao direcao;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "roda_Carro", nullable = false)
    private TipoRoda roda;

    @NotNull(message = "O número do aro é obrigatório")
    @Min(value = 8, message = "O número mínimo do aro é 8")
    @Max(value = 32, message = "O número máximo do aro é 32")
    @Column(name = "aro_Carro", length = 2, nullable = false)
    private Integer aro;

    @NotNull(message = "O número de portas é obrigatório")
    @Min(value = 1, message = "O número mínimo de portas é 1")
    @Max(value = 5, message = "O número máximo de portas é 5")
    @Column(name = "numeroPortas_Carro", nullable = false)
    private Integer numeroPortas;

    @NotNull(message = "O número de assentos é obrigatório")
    @Min(value = 1, message = "O número mínimo de assentos é 1")
    @Max(value = 50, message = "O número máximo de assentos é 50")
    @Column(name = "numeroAssentos_Carro", nullable = false)
    private Integer numeroAssentos;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "materialRevestimentoInterno_Carro", nullable = false)
    private TipoMaterialRevestimentoInterno materialRevestimentoInterno;

    @NotBlank(message = "A placa é obrigatória")
    @Pattern(regexp = "^[A-Za-z0-9]{7}$", message = "A placa deve conter exatamente 7 caracteres alfanuméricos")
    @Column(name = "placa_Carro", length = 7, unique = true, nullable = false)
    private String placa;

    @NotNull(message = "É necessário informar se possui vidros elétricos")
    @Column(name = "vidrosEletricos_Carro", nullable = false)
    private Boolean vidrosEletricos = false;

    @NotNull(message = "É necessário informar se possui som")
    @Column(name = "som_Carro", nullable = false)
    private Boolean som = false;

    @NotNull(message = "É necessário informar se possui ar condicionado")
    @Column(name = "arCondicionado_Carro", nullable = false)
    private Boolean arCondicionado = false;

    @NotNull(message = "É necessário informar se possui teto solar")
    @Column(name = "tetoSolar_Carro", nullable = false)
    private Boolean tetoSolar = false;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "idColecionador_Carro", referencedColumnName = "id_Colecionador", nullable = false)
    private Colecionador colecionador;
}
