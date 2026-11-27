package carshub.com.br.backend.models.entities;

import carshub.com.br.backend.models.enums.eventos.StatusEvento;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "Evento")
@Data
@NoArgsConstructor
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.BINARY)
    @Column(name = "id_Evento", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "O nome do evento é um campo obrigatório")
    @Size(max = 50, message = "O nome do evento não pode exceder 50 caracteres")
    @Column(name = "nome_Evento", length = 50, nullable = false)
    private String nome;

    @NotNull(message = "O preço do evento é um campo obrigatório")
    @DecimalMin(value = "0.00", message = "O preço do evento deve ser maior ou igual a 0.00")
    @Column(name = "preco_Evento", precision = 7, scale = 2, nullable = false)
    private BigDecimal preco;

    @NotBlank(message = "A descrição é um campo obrigatório")
    @Size(max = 1000, message = "A descrição não pode exceder 1000 caracteres")
    @Column(name = "descricao_Evento", length = 1000, nullable = false)
    private String descricao;

    @NotNull(message = "O limite de participantes é obrigatório")
    @Min(value = 1, message = "O evento deve ter espaço para pelo menos 1 participante")
    @Column(name = "limiteParticipantes_Evento", nullable = false)
    private Integer limiteParticipantes;

    @NotBlank(message = "A rua é obrigatória")
    @Size(max = 100, message = "A rua não pode exceder 100 caracteres")
    @Column(name = "rua_Evento", length = 100, nullable = false)
    private String rua;

    @NotBlank(message = "O número é obrigatório")
    @Size(max = 5, message = "O número não pode exceder 5 caracteres")
    @Column(name = "numero_Evento", length = 5, nullable = false)
    private String numero;

    @Size(max = 50, message = "O complemento não pode exceder 50 caracteres")
    @Column(name = "complemento_Evento", length = 50)
    private String complemento;

    @NotBlank(message = "O bairro é obrigatório")
    @Size(max = 50, message = "O bairro não pode exceder 50 caracteres")
    @Column(name = "bairro_Evento", length = 50, nullable = false)
    private String bairro;

    // F29: cidade, UF e CEP passam a compor o endereco do evento e sao usados
    // na geocodificacao e nos calculos de proximidade.

    @NotBlank(message = "A cidade é obrigatória")
    @Size(max = 50, message = "A cidade não pode exceder 50 caracteres")
    @Column(name = "cidade_Evento", length = 50, nullable = false)
    private String cidade;

    @NotBlank(message = "A UF é obrigatória")
    @Size(min = 2, max = 2, message = "A UF deve conter exatamente 2 caracteres")
    @Column(name = "uf_Evento", length = 2, nullable = false)
    private String uf;

    @NotBlank(message = "O CEP é obrigatório")
    @Pattern(regexp = "\\d{8}", message = "O CEP deve conter exatamente 8 dígitos")
    @Column(name = "cep_Evento", length = 8, nullable = false)
    private String cep;

    @NotNull(message = "A data e hora de início são obrigatórias")
    @Column(name = "dataHoraInicio_Evento", nullable = false)
    private LocalDateTime dataHoraInicio;

    @NotNull(message = "A data e hora de término são obrigatórias")
    @Column(name = "dataHoraFim_Evento", nullable = false)
    private LocalDateTime dataHoraFim;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status_Evento", nullable = false)
    private StatusEvento status = StatusEvento.ATIVO;

    /** AJUSTE 1.3: travamento otimista, como nos Servico*. Conflito de escrita -> 409. */
    @Version
    @Column(name = "versao_Evento")
    private Long versao;

    @NotNull(message = "É necessário ter um organizador vinculado para criar um evento")
    @ManyToOne(optional = false)
    @JoinColumn(name = "organizadorEvento_Evento", referencedColumnName = "id_Usuario", nullable = false)
    private OrganizadorEvento organizadorEvento;

    @ElementCollection
    @CollectionTable(name = "UrlFoto_Evento", joinColumns = @JoinColumn(name = "id_Evento"))
    @Column(name = "urlFotoEvento_Evento")
    private List<String> urlFotoEvento;

    @Column(name = "latitude_Evento")
    private Double latitude;

    @Column(name = "longitude_Evento")
    private Double longitude;

    /** Carros aceitos no evento; null = evento aberto a todos os carros. */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "idFiltroCarro_Evento", unique = true)
    private FiltroCarro filtroCarro;
}