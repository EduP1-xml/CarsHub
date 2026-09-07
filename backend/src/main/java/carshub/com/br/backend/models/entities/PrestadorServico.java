package carshub.com.br.backend.models.entities;


import carshub.com.br.backend.models.enums.TipoPrestadorServico;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "PrestadorServico")
@Inheritance(strategy = InheritanceType.JOINED)// piora as consultas, mas o banco fica normalizado(mais importante);
@PrimaryKeyJoinColumn(name = "idPrestadorServico")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public abstract class PrestadorServico extends Usuario{

    @NotBlank(message = "O CNPJ é obrigatório para prestadores de serviço")
    @Pattern(regexp = "\\d{14}", message = "O CNPJ deve conter exatamente 14 dígitos numéricos")
    @Column(name = "cnpj_prestador", length = 14, unique = true, nullable = false)
    private String cnpj;

    @NotNull(message = "O tipo de prestador é obrigatório")
    @Enumerated(EnumType.STRING)// só definindo a forma de armazenamento desse Enum
    @Column(name = "tipo_prestador_servico", nullable = false)
    private TipoPrestadorServico tipoPrestadorServico;
}