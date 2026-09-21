package carshub.com.br.backend.models.entities;

import carshub.com.br.backend.models.enums.eventos.StatusEvento;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;
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
    private UUID idEvento;

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
    @Column(name = "limite_Participantes", nullable = false)
    private Integer limiteParticipantes;

    // --- ENDEREÇO ---
    @NotBlank(message = "A rua é obrigatória")
    @Size(max = 100, message = "A rua não pode exceder 100 caracteres")
    @Column(name = "rua_Evento", length = 100, nullable = false)
    private String rua;

    @NotBlank(message = "O número é obrigatório")
    @Size(max = 10, message = "O número não pode exceder 10 caracteres")
    @Column(name = "numero_Evento", length = 10, nullable = false)
    private String numero;

    @Size(max = 50, message = "O complemento não pode exceder 50 caracteres")
    @Column(name = "complemento_Evento", length = 50) // Campo puramente opcional
    private String complemento;

    @NotBlank(message = "O bairro é obrigatório")
    @Size(max = 50, message = "O bairro não pode exceder 50 caracteres")
    @Column(name = "bairro_Evento", length = 50, nullable = false)
    private String bairro;
    // ----------------

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

    @NotNull(message = "É necessário ter um organizador vinculado para criar um evento")
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_OrganizadorEvento_Evento", referencedColumnName = "id_OrganizadorEvento", nullable = false)
    private OrganizadorEvento organizadorEvento;
}