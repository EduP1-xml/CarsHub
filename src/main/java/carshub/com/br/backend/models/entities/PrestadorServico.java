package carshub.com.br.backend.models.entities;

import carshub.com.br.backend.models.enums.tiposUsuarios.TipoPrestadorServico;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "PrestadorServico")
@Inheritance(strategy = InheritanceType.JOINED)// piora as consultas, mas o banco fica normalizado(mais importante);
@PrimaryKeyJoinColumn(name = "id_PrestadorServico")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public abstract class PrestadorServico extends Usuario{

    @Column(name = "cnpj_PrestadorServico", length = 14, unique = true)
    private String cnpj;

    @NotNull(message = "O tipo de prestador é obrigatório")
    @Enumerated(EnumType.STRING)// só definindo a forma de armazenamento desse Enum
    @Column(name = "tipoPrestadorServico_PrestadorServico", nullable = false)
    private TipoPrestadorServico tipoPrestadorServico;

    // REPUTACAO
    @Column(name = "notaMediaPrestador_PrestadorServico")
    private Double notaMediaPrestador = 0.0;

    /** Quantidade de avaliacoes que compoem a media acima. */
    @Column(name = "quantidadeAvaliacoes_PrestadorServico")
    private Long quantidadeAvaliacoes = 0L;
}