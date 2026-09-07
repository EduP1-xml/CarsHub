package carshub.com.br.backend.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "OrganizadoEvento")
@PrimaryKeyJoinColumn(name = "idOrganizadorEvento")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class OrganizadorEvento extends Usuario {

    @NotBlank(message = "O CNPJ é obrigatório para realização de atividades comerciais")
    @Pattern(regexp = "\\d{14}", message = "O CNPJ deve conter 14 dígitos numéricos")
    @Column(name = "cnpj_organizadorEvento", length = 14, unique = true, nullable = false)
    private String cnpj;
}
