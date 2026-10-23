package carshub.com.br.backend.models.entities.servicos;

import carshub.com.br.backend.models.entities.Carro;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.Guincho;
import carshub.com.br.backend.models.enums.servicos.StatusServico;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Solicitacao de guincho ("leilao inverso"). Mesmo ciclo de vida do servico
 * mecanico: SOLICITADO -> PROPOSTA -> PENDENTE -> CHEGOU -> FINALIZADO, com
 * confirmacao dupla de chegada e conclusao, fotos e avaliacao mutua.
 */
@Entity
@Table(name = "ServicoGuincho", indexes = {
        @Index(name = "idx_servicoGuincho_status_local",
                columnList = "status_ServicoGuincho, latitudeAtualEntusiasta_ServicoGuincho, longitudeAtualEntusiasta_ServicoGuincho"),
        @Index(name = "idx_servicoGuincho_guincho_status",
                columnList = "id_Guincho_ServicoGuincho, status_ServicoGuincho")
})
@Data
@NoArgsConstructor
public class ServicoGuincho {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.BINARY)
    @Column(name = "id_ServicoGuincho", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    /** Travamento otimista (decisao D1). */
    @Version
    @Column(name = "versao_ServicoGuincho")
    private Long versao;

    /** Chave de idempotencia (decisao D5); zerada quando o servico e finalizado. */
    @Column(name = "chaveIdempotencia_ServicoGuincho", length = 64, unique = true)
    private String chaveIdempotencia;

    @NotNull(message = "A solicitação precisa de um colecionador responsável")
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_Entusiasta_ServicoGuincho", referencedColumnName = "id_Entusiasta", nullable = false)
    private Entusiasta entusiasta;

    /** Nulo enquanto o chamado esta na rede (SOLICITADO). */
    @ManyToOne
    @JoinColumn(name = "id_Guincho_ServicoGuincho", referencedColumnName = "id_Guincho")
    private Guincho guincho;

    @NotNull(message = "O veículo do serviço é obrigatório")
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_Carro_ServicoGuincho", referencedColumnName = "id_Carro", nullable = false)
    private Carro carro;

    // ==================================================================
    // LOCALIZACAO DA OCORRENCIA
    // ==================================================================

    @Column(name = "latitudeAtualEntusiasta_ServicoGuincho", nullable = false)
    private Double latitudeAtualEntusiasta;

    @Column(name = "longitudeAtualEntusiasta_ServicoGuincho", nullable = false)
    private Double longitudeAtualEntusiasta;

    @Column(name = "enderecoAtualEntusiasta_ServicoGuincho", length = 255)
    private String enderecoAtualEntusiasta;

    @Embedded
    private EnderecoOcorrencia enderecoEstruturado;

    @Column(name = "latitudeAtualGuincho_ServicoGuincho")
    private Double latitudeAtualGuincho;

    @Column(name = "longitudeAtualGuincho_ServicoGuincho")
    private Double longitudeAtualGuincho;

    // ==================================================================
    // NEGOCIACAO
    // ==================================================================

    @DecimalMin(value = "0.01", message = "O valor proposto deve ser maior que zero")
    @Column(name = "valorProposto_ServicoGuincho", precision = 10, scale = 2)
    private BigDecimal valorProposto;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status_ServicoGuincho", nullable = false)
    private StatusServico status = StatusServico.SOLICITADO;

    @CreationTimestamp
    @Column(name = "dataHoraSolicitacao_ServicoGuincho", nullable = false, updatable = false, columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private LocalDateTime dataHoraSolicitacao;

    @Column(name = "dataHoraProposta_ServicoGuincho")
    private LocalDateTime dataHoraProposta;

    @Column(name = "dataHoraPendente_ServicoGuincho")
    private LocalDateTime dataHoraPendente;

    /**
     * MATCH (regra N14): quantas propostas este chamado ja recebeu ao longo da
     * vida — teto de 10. Nunca decrementa: rejeitar/expirada so devolve o chamado
     * para a rede, nao apaga a proposta que existiu.
     */
    @Column(name = "quantidadePropostas_ServicoGuincho", nullable = false)
    private Integer quantidadePropostas = 0;

    // ==================================================================
    // EVIDENCIAS: FOTOS
    // ==================================================================

    @Column(name = "urlFotoInicial_ServicoGuincho", length = 255)
    private String urlFotoInicial;

    @Column(name = "urlFotoFinalEntusiasta_ServicoGuincho", length = 255)
    private String urlFotoFinalEntusiasta;

    @Column(name = "urlFotoFinalGuincho_ServicoGuincho", length = 255)
    private String urlFotoFinalGuincho;

    // ==================================================================
    // CHEGADA (confirmacao dupla)
    // ==================================================================

    @NotNull
    @Column(name = "confirmacaoChegadaEntusiasta_ServicoGuincho", nullable = false)
    private Boolean confirmacaoChegadaEntusiasta = false;

    @NotNull
    @Column(name = "confirmacaoChegadaGuincho_ServicoGuincho", nullable = false)
    private Boolean confirmacaoChegadaGuincho = false;

    @Column(name = "dataHoraChegou_ServicoGuincho")
    private LocalDateTime dataHoraChegou;

    // ==================================================================
    // AVALIACAO MUTUA
    // ==================================================================

    @Min(1) @Max(5)
    @Column(name = "notaAvaliacaoEntusiasta_ServicoGuincho")
    private Integer notaAvaliacaoEntusiasta;

    @Size(max = 500)
    @Column(name = "comentarioAvaliacaoEntusiasta_ServicoGuincho", length = 500)
    private String comentarioAvaliacaoEntusiasta;

    @Column(name = "dataHoraAvaliacaoEntusiasta_ServicoGuincho")
    private LocalDateTime dataHoraAvaliacaoEntusiasta;

    @Min(1) @Max(5)
    @Column(name = "notaAvaliacaoGuincho_ServicoGuincho")
    private Integer notaAvaliacaoGuincho;

    @Size(max = 500)
    @Column(name = "comentarioAvaliacaoGuincho_ServicoGuincho", length = 500)
    private String comentarioAvaliacaoGuincho;

    @Column(name = "dataHoraAvaliacaoGuincho_ServicoGuincho")
    private LocalDateTime dataHoraAvaliacaoGuincho;

    // ==================================================================
    // CONCLUSAO (handshake duplo)
    // ==================================================================

    @NotNull
    @Column(name = "confirmacaoConclusaoEntusiasta_ServicoGuincho", nullable = false)
    private Boolean confirmacaoConclusaoEntusiasta = false;

    @NotNull
    @Column(name = "confirmacaoConclusaoGuincho_ServicoGuincho", nullable = false)
    private Boolean confirmacaoConclusaoGuincho = false;

    @Column(name = "dataHoraFinalizacao_ServicoGuincho")
    private LocalDateTime dataHoraFinalizacao;
}