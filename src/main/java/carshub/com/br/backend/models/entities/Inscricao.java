package carshub.com.br.backend.models.entities;

import carshub.com.br.backend.models.enums.inscricoes.StatusInscricao;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "Inscricao")
@Data
@NoArgsConstructor
public class Inscricao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.BINARY)
    @Column(name = "id_Inscricao", updatable = false, nullable = false)
    private UUID id;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_Entusiasta_Inscricao", referencedColumnName = "id_Entusiasta", nullable = false)
    private Entusiasta entusiasta;

    @NotNull
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_Evento_Inscricao", referencedColumnName = "id_Evento", nullable = false)
    private Evento evento;

    // Relacionamento Opcional: O Entusiasta pode ir a pé/carona ou levar um carro da sua garagem
    @ManyToOne
    @JoinColumn(name = "id_Carro_Inscricao", referencedColumnName = "id_Carro")
    private Carro carro;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status_Inscricao", nullable = false)
    private StatusInscricao status = StatusInscricao.CONFIRMADA;

    /** AJUSTE 1.3: travamento otimista. Conflito de escrita -> 409. */
    @Version
    @Column(name = "versao_Inscricao")
    private Long versao;

    @NotNull
    @Column(name = "data_Inscricao", nullable = false, updatable = false)
    private LocalDateTime data = LocalDateTime.now();
}