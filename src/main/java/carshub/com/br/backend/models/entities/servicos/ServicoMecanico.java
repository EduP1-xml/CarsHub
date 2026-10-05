package carshub.com.br.backend.models.entities.servicos;

import carshub.com.br.backend.models.entities.Carro;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.Mecanico;
import carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados;
import carshub.com.br.backend.models.enums.servicos.StatusServico;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ServicoMecanico")
@Data
@NoArgsConstructor
public class ServicoMecanico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.BINARY)
    @Column(name = "id_ServicoMecanico", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @NotNull(message = "A solicitação precisa de um colecionador responsável")
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_Entusiasta_ServicoMecanico", referencedColumnName = "id_Entusiasta", nullable = false)
    private Entusiasta entusiasta;

    @ManyToOne
    @JoinColumn(name = "id_Mecanico_ServicoMecanico", referencedColumnName = "id_Mecanico")
    private Mecanico mecanico;

    @NotNull(message = "O veículo do serviço é obrigatório")
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_Carro_ServicoMecanico", referencedColumnName = "id_Carro", nullable = false)
    private Carro carro;

    @NotEmpty(message = "Informe pelo menos um serviço a ser realizado")
    @ElementCollection(targetClass = ServicosOfertados.class)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "ServicosOfertados_ServicoMecanico", joinColumns = @JoinColumn(name = "id_ServicoMecanico"))
    @Column(name = "servicosOfertados_ServicoMecanico")
    private List<ServicosOfertados> servicos;

    @Column(name = "latitudeAtualEntusiasta_ServicoMecanico")
    private Double latitudeAtualEntusiasta;

    @Column(name = "longitudeAtualEntusiasta_ServicoMecanico")
    private Double longitudeAtualEntusiasta;

    @Column(name = "enderecoAtualEntusiasta_ServicoMecanico", length = 255)
    private String enderecoAtualEntusiasta;

    @Column(name = "latitudeAtualMecanico_ServicoMecanico")
    private Double latitudeAtualMecanico;

    @Column(name = "longitudeAtualMecanico_ServicoMecanico")
    private Double longitudeAtualMecanico;

    @DecimalMin(value = "0.01", message = "O valor proposto deve ser maior que zero")
    @Column(name = "valorProposto_ServicoMecanico", precision = 10, scale = 2)
    private BigDecimal valorProposto;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status_ServicoMecanico", nullable = false)
    private StatusServico status = StatusServico.SOLICITADO;

    @NotNull
    @Column(name = "confirmacaoConclusaoEntusiasta_ServicoMecanico", nullable = false)
    private Boolean confirmacaoConclusaoEntusiasta = false;

    @NotNull
    @Column(name = "confirmacaoConclusaoMecanico_ServicoMecanico", nullable = false)
    private Boolean confirmacaoConclusaoMecanico = false;

    @CreationTimestamp
    @Column(name = "dataHoraSolicitacao_ServicoMecanico", nullable = false, updatable = false, columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private LocalDateTime dataHoraSolicitacao;

    @Column(name = "dataHoraProposta_ServicoMecanico")
    private LocalDateTime dataHoraProposta;

    @Column(name = "dataHoraFinalizacao_ServicoMecanico")
    private LocalDateTime dataHoraFinalizacao;
}