package carshub.com.br.backend.models.entities.servicos;

import carshub.com.br.backend.models.entities.Carro;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.Guincho;
import carshub.com.br.backend.models.enums.servicos.StatusServico;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "ServicoGuincho")
@Data
@NoArgsConstructor
public class ServicoGuincho {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.BINARY)
    @Column(name = "id_ServicoGuincho", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    @NotNull(message = "A solicitação precisa de um colecionador responsável")
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_Entusiasta_ServicoGuincho", referencedColumnName = "id_Entusiasta", nullable = false)
    private Entusiasta entusiasta;

    @ManyToOne
    @JoinColumn(name = "id_Guincho_ServicoGuincho", referencedColumnName = "id_Guincho")
    private Guincho guincho;

    @NotNull(message = "O veículo do serviço é obrigatório")
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_Carro_ServicoGuincho", referencedColumnName = "id_Carro", nullable = false)
    private Carro carro;

    @Column(name = "latitudeAtualEntusiasta_ServicoGuincho")
    private Double latitudeAtualEntusiasta;

    @Column(name = "longitudeAtualEntusiasta_ServicoGuincho")
    private Double longitudeAtualEntusiasta;

    @Column(name = "enderecoAtualEntusiasta_ServicoGuincho", length = 255)
    private String enderecoAtualEntusiasta;

    @Column(name = "latitudeAtualGuincho_ServicoGuincho")
    private Double latitudeAtualGuincho;

    @Column(name = "longitudeAtualGuincho_ServicoGuincho")
    private Double longitudeAtualGuincho;

    @DecimalMin(value = "0.01", message = "O valor proposto deve ser maior que zero")
    @Column(name = "valorProposto_ServicoGuincho", precision = 10, scale = 2)
    private BigDecimal valorProposto;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status_ServicoGuincho", nullable = false)
    private StatusServico status = StatusServico.SOLICITADO;

    @NotNull
    @Column(name = "confirmacaoConclusaoEntusiasta_ServicoGuincho", nullable = false)
    private Boolean confirmacaoConclusaoEntusiasta = false;

    @NotNull
    @Column(name = "confirmacaoConclusaoGuincho_ServicoGuincho", nullable = false)
    private Boolean confirmacaoConclusaoGuincho = false;

    @CreationTimestamp
    @Column(name = "dataHoraSolicitacao_ServicoGuincho", nullable = false, updatable = false, columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private LocalDateTime dataHoraSolicitacao;

    @Column(name = "dataHoraProposta_ServicoGuincho")
    private LocalDateTime dataHoraProposta;

    @Column(name = "dataHoraFinalizacao_ServicoGuincho")
    private LocalDateTime dataHoraFinalizacao;
}