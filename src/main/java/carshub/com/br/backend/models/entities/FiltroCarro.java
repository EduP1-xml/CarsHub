package carshub.com.br.backend.models.entities;

import carshub.com.br.backend.models.enums.carros.*;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * "Gosto" de carro de um Clube ou publico-alvo de um Evento.
 *
 * Cada caracteristica da entidade Carro (menos id, placa, colecionador, fotos e
 * os booleanos que nao sao som) vira um criterio:
 *   - String  -> conjunto de valores aceitos (tabela filha propria);
 *   - Enum    -> conjunto de valores aceitos (tabela filha propria, tipada);
 *   - Numero  -> faixa minimo/maximo (pontas opcionais = abertas);
 *   - som     -> true = exige som; null = nao e criterio.
 *
 * Criterio NAO marcado (conjunto vazio, faixa sem pontas, som null) e
 * irrelevante: qualquer valor passa. As regras ficam em CompatibilidadeFiltro.
 *
 * Entidade propria (e nao @Embeddable) para Clube e Evento compartilharem as
 * mesmas collection tables sem conflito de nomes.
 */
@Entity
@Table(name = "FiltroCarro")
@Getter
@Setter
@NoArgsConstructor
public class FiltroCarro {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.BINARY)
    @Column(name = "id_FiltroCarro", updatable = false, nullable = false)
    private UUID id;

    // ========================================================
    // STRINGS (conjunto de valores aceitos)
    // ========================================================

    @ElementCollection
    @CollectionTable(name = "FiltroCarro_Marcas", joinColumns = @JoinColumn(name = "idFiltroCarro"))
    @Column(name = "marca", length = 30, nullable = false)
    private Set<String> marcas = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "FiltroCarro_Modelos", joinColumns = @JoinColumn(name = "idFiltroCarro"))
    @Column(name = "modelo", length = 30, nullable = false)
    private Set<String> modelos = new HashSet<>();

    @ElementCollection
    @CollectionTable(name = "FiltroCarro_Cores", joinColumns = @JoinColumn(name = "idFiltroCarro"))
    @Column(name = "cor", length = 20, nullable = false)
    private Set<String> cores = new HashSet<>();

    // ========================================================
    // ENUMS (conjunto de valores aceitos)
    // ========================================================

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "FiltroCarro_Carrocerias", joinColumns = @JoinColumn(name = "idFiltroCarro"))
    @Column(name = "carroceria", nullable = false)
    private Set<TipoCarroceria> carrocerias = new HashSet<>();

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "FiltroCarro_Motores", joinColumns = @JoinColumn(name = "idFiltroCarro"))
    @Column(name = "motor", nullable = false)
    private Set<TipoMotor> motores = new HashSet<>();

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "FiltroCarro_Injecoes", joinColumns = @JoinColumn(name = "idFiltroCarro"))
    @Column(name = "injecao", nullable = false)
    private Set<TipoInjecao> injecoes = new HashSet<>();

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "FiltroCarro_Alimentacoes", joinColumns = @JoinColumn(name = "idFiltroCarro"))
    @Column(name = "alimentacao", nullable = false)
    private Set<TipoAlimentacao> alimentacoes = new HashSet<>();

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "FiltroCarro_Combustiveis", joinColumns = @JoinColumn(name = "idFiltroCarro"))
    @Column(name = "combustivel", nullable = false)
    private Set<TipoCombustivel> combustiveis = new HashSet<>();

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "FiltroCarro_Cambios", joinColumns = @JoinColumn(name = "idFiltroCarro"))
    @Column(name = "cambio", nullable = false)
    private Set<TipoCambio> cambios = new HashSet<>();

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "FiltroCarro_Tracoes", joinColumns = @JoinColumn(name = "idFiltroCarro"))
    @Column(name = "tracao", nullable = false)
    private Set<TipoTracao> tracoes = new HashSet<>();

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "FiltroCarro_Suspensoes", joinColumns = @JoinColumn(name = "idFiltroCarro"))
    @Column(name = "suspensao", nullable = false)
    private Set<TipoSuspensao> suspensoes = new HashSet<>();

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "FiltroCarro_Freios", joinColumns = @JoinColumn(name = "idFiltroCarro"))
    @Column(name = "freio", nullable = false)
    private Set<TipoFreio> freios = new HashSet<>();

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "FiltroCarro_Direcoes", joinColumns = @JoinColumn(name = "idFiltroCarro"))
    @Column(name = "direcao", nullable = false)
    private Set<TipoDirecao> direcoes = new HashSet<>();

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "FiltroCarro_Rodas", joinColumns = @JoinColumn(name = "idFiltroCarro"))
    @Column(name = "roda", nullable = false)
    private Set<TipoRoda> rodas = new HashSet<>();

    @ElementCollection
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "FiltroCarro_MateriaisRevestimentoInterno", joinColumns = @JoinColumn(name = "idFiltroCarro"))
    @Column(name = "materialRevestimentoInterno", nullable = false)
    private Set<TipoMaterialRevestimentoInterno> materiaisRevestimentoInterno = new HashSet<>();

    // ========================================================
    // FAIXAS NUMERICAS (ponta nula = aberta)
    // ========================================================

    @Column(name = "anoMin_FiltroCarro")
    private Integer anoMin;

    @Column(name = "anoMax_FiltroCarro")
    private Integer anoMax;

    @Column(name = "quantidadeCilindrosMin_FiltroCarro")
    private Integer quantidadeCilindrosMin;

    @Column(name = "quantidadeCilindrosMax_FiltroCarro")
    private Integer quantidadeCilindrosMax;

    @Column(name = "litragemMotorMin_FiltroCarro", precision = 5, scale = 3)
    private BigDecimal litragemMotorMin;

    @Column(name = "litragemMotorMax_FiltroCarro", precision = 5, scale = 3)
    private BigDecimal litragemMotorMax;

    @Column(name = "aroMin_FiltroCarro")
    private Integer aroMin;

    @Column(name = "aroMax_FiltroCarro")
    private Integer aroMax;

    @Column(name = "numeroPortasMin_FiltroCarro")
    private Integer numeroPortasMin;

    @Column(name = "numeroPortasMax_FiltroCarro")
    private Integer numeroPortasMax;

    @Column(name = "numeroAssentosMin_FiltroCarro")
    private Integer numeroAssentosMin;

    @Column(name = "numeroAssentosMax_FiltroCarro")
    private Integer numeroAssentosMax;

    // ========================================================
    // BOOLEANO (unico que entra no filtro)
    // ========================================================

    /** true = exige som; null = nao e criterio. */
    @Column(name = "som_FiltroCarro")
    private Boolean som;
}
