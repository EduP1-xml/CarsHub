package carshub.com.br.backend.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import java.math.BigDecimal;
import java.sql.Types;
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

    @NotBlank(message = "O nome do evento é um campo obrigatorio")
    @Size(max = 50, message = "O nome do evento não pode exceder 50 caracteres")
    @Column(name = "nome_Evento", length = 50, nullable = false)
    private String nome;

    @NotBlank(message = "O preço do evento é um campo obrigatório")
    @DecimalMin(value = "0.00", message = "O preço do evento deve ser maior ou igual a 0.00")
    @Column(name = "preco_Evento", precision = 7, scale = 2, nullable = false)
    private BigDecimal preco;

    @NotBlank(message = "A descrição é um campo obrigatório")
    @Size(max = 1000, message = "A descrição não pode exceder 1000 caracteres")
    @Column(name = "descricao_Evento", length = 1000, nullable = false)
    private String descricao;

    @NotBlank(message = "É necessário ter um organizador vinculado para criar um evento")
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_OrganizadorEvento_Evento", referencedColumnName = "id_OrganizadorEvento", nullable = false)
    private OrganizadorEvento organizadorEvento;
}
