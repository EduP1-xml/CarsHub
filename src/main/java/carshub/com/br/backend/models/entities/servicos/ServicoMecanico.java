package carshub.com.br.backend.models.entities.servicos;

import carshub.com.br.backend.models.entities.Carro;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.Mecanico;
import carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados;
import carshub.com.br.backend.models.enums.servicos.StatusServico;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Solicitacao de servico mecanico ("leilao inverso").
 *
 * CICLO: SOLICITADO -> PROPOSTA -> PENDENTE -> CHEGOU -> FINALIZADO.
 * Toda transicao critica e feita por UPDATE condicional no repositorio.
 *
 * ELEGIBILIDADE (compatibilidade): no momento da criacao o sistema resolve QUAIS
 * mecanicos ofertam todos os servicos pedidos e grava a lista aqui. O painel do
 * mecanico filtra por ela e a proposta apenas confere pertencimento — nao ha
 * revalidacao de compatibilidade a cada proposta.
 */
@Entity
@Table(name = "ServicoMecanico", indexes = {
        @Index(name = "idx_servicoMecanico_status_local",
                columnList = "status_ServicoMecanico, latitudeAtualEntusiasta_ServicoMecanico, longitudeAtualEntusiasta_ServicoMecanico"),
        @Index(name = "idx_servicoMecanico_mecanico_status",
                columnList = "id_Mecanico_ServicoMecanico, status_ServicoMecanico")
})
@Data
@NoArgsConstructor
public class ServicoMecanico {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.BINARY)
    @Column(name = "id_ServicoMecanico", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    /** Travamento otimista (decisao D1). */
    @Version
    @Column(name = "versao_ServicoMecanico")
    private Long versao;

    /**
     * Idempotencia (D5). AJUSTE 1.4 (decisao do time): a coluna e ANULAVEL
     * porque o UPDATE de FINALIZACAO zera a chave — nunca porque a solicitacao
     * possa nascer sem ela. Uma nova solicitacao identica (mesmo colecionador,
     * carro e servicos) so volta a ser aceita depois que a anterior for FINALIZADA.
     */
    @Column(name = "chaveIdempotencia_ServicoMecanico", length = 64, unique = true)
    private String chaveIdempotencia;

    @NotNull(message = "A solicitação precisa de um colecionador responsável")
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_Entusiasta_ServicoMecanico", referencedColumnName = "id_Entusiasta", nullable = false)
    private Entusiasta entusiasta;

    /** Nulo enquanto o chamado esta na rede (SOLICITADO). */
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

    /**
     * Mecanicos ativos que ofertam TODOS os servicos acima: a rede que enxerga
     * este chamado. Resolvida na criacao (decisao de compatibilidade) e usada
     * como filtro do painel.
     *
     * LAZY + @BatchSize: a lista e carregada em lote quando varios chamados sao
     * lidos juntos. AJUSTE 1.1c/1.2: lote de 10 = duas paginas de 5, e nas
     * LISTAGENS a contagem veio do GROUP BY contarElegiveisPorServico — a
     * hidratacao aqui fica so para os fluxos que usam o conteudo (proposta e
     * ranking). Excluida de toString/equals para nao disparar
     * LazyInitializationException em log.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "MecanicosElegiveis_ServicoMecanico",
            joinColumns = @JoinColumn(name = "id_ServicoMecanico"),
            inverseJoinColumns = @JoinColumn(name = "id_Mecanico"))
    @BatchSize(size = 10)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Mecanico> mecanicosElegiveis = new ArrayList<>();

    // ==================================================================
    // LOCALIZACAO DA OCORRENCIA
    // ==================================================================

    @Column(name = "latitudeAtualEntusiasta_ServicoMecanico", nullable = false)
    private Double latitudeAtualEntusiasta;

    @Column(name = "longitudeAtualEntusiasta_ServicoMecanico", nullable = false)
    private Double longitudeAtualEntusiasta;

    @Column(name = "enderecoAtualEntusiasta_ServicoMecanico", length = 255)
    private String enderecoAtualEntusiasta;

    @Embedded
    private EnderecoOcorrencia enderecoEstruturado;

    @Column(name = "latitudeAtualMecanico_ServicoMecanico")
    private Double latitudeAtualMecanico;

    @Column(name = "longitudeAtualMecanico_ServicoMecanico")
    private Double longitudeAtualMecanico;

    // ==================================================================
    // NEGOCIACAO
    // ==================================================================

    @DecimalMin(value = "0.01", message = "O valor proposto deve ser maior que zero")
    @Column(name = "valorProposto_ServicoMecanico", precision = 10, scale = 2)
    private BigDecimal valorProposto;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status_ServicoMecanico", nullable = false)
    private StatusServico status = StatusServico.SOLICITADO;

    @CreationTimestamp
    @Column(name = "dataHoraSolicitacao_ServicoMecanico", nullable = false, updatable = false, columnDefinition = "DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6)")
    private LocalDateTime dataHoraSolicitacao;

    @Column(name = "dataHoraProposta_ServicoMecanico")
    private LocalDateTime dataHoraProposta;

    @Column(name = "dataHoraPendente_ServicoMecanico")
    private LocalDateTime dataHoraPendente;

    /**
     * MATCH (regra N14): quantas propostas este chamado ja recebeu ao longo da
     * vida — teto de 10. Nunca decrementa: rejeitar/expirada so devolve o chamado
     * para a rede, nao apaga a proposta que existiu.
     */
    @Column(name = "quantidadePropostas_ServicoMecanico", nullable = false)
    private Integer quantidadePropostas = 0;

    // ==================================================================
    // EVIDENCIAS: FOTOS
    // ==================================================================

    @Column(name = "urlFotoInicial_ServicoMecanico", length = 255)
    private String urlFotoInicial;

    @Column(name = "urlFotoFinalEntusiasta_ServicoMecanico", length = 255)
    private String urlFotoFinalEntusiasta;

    @Column(name = "urlFotoFinalMecanico_ServicoMecanico", length = 255)
    private String urlFotoFinalMecanico;

    // ==================================================================
    // CHEGADA (confirmacao dupla)
    // ==================================================================

    @NotNull
    @Column(name = "confirmacaoChegadaEntusiasta_ServicoMecanico", nullable = false)
    private Boolean confirmacaoChegadaEntusiasta = false;

    @NotNull
    @Column(name = "confirmacaoChegadaMecanico_ServicoMecanico", nullable = false)
    private Boolean confirmacaoChegadaMecanico = false;

    @Column(name = "dataHoraChegou_ServicoMecanico")
    private LocalDateTime dataHoraChegou;

    // ==================================================================
    // AVALIACAO MUTUA
    // ==================================================================

    @Min(1) @Max(5)
    @Column(name = "notaAvaliacaoEntusiasta_ServicoMecanico")
    private Integer notaAvaliacaoEntusiasta;

    @Size(max = 500)
    @Column(name = "comentarioAvaliacaoEntusiasta_ServicoMecanico", length = 500)
    private String comentarioAvaliacaoEntusiasta;

    @Column(name = "dataHoraAvaliacaoEntusiasta_ServicoMecanico")
    private LocalDateTime dataHoraAvaliacaoEntusiasta;

    @Min(1) @Max(5)
    @Column(name = "notaAvaliacaoMecanico_ServicoMecanico")
    private Integer notaAvaliacaoMecanico;

    @Size(max = 500)
    @Column(name = "comentarioAvaliacaoMecanico_ServicoMecanico", length = 500)
    private String comentarioAvaliacaoMecanico;

    @Column(name = "dataHoraAvaliacaoMecanico_ServicoMecanico")
    private LocalDateTime dataHoraAvaliacaoMecanico;

    // ==================================================================
    // CONCLUSAO (handshake duplo)
    // ==================================================================

    @NotNull
    @Column(name = "confirmacaoConclusaoEntusiasta_ServicoMecanico", nullable = false)
    private Boolean confirmacaoConclusaoEntusiasta = false;

    @NotNull
    @Column(name = "confirmacaoConclusaoMecanico_ServicoMecanico", nullable = false)
    private Boolean confirmacaoConclusaoMecanico = false;

    @Column(name = "dataHoraFinalizacao_ServicoMecanico")
    private LocalDateTime dataHoraFinalizacao;
}