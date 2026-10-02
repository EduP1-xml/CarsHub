package carshub.com.br.backend.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "OrganizadorEvento")
@PrimaryKeyJoinColumn(name = "id_OrganizadorEvento")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class OrganizadorEvento extends Usuario {

    @Column(name = "cnpj_OrganizadorEvento", length = 14, unique = true)
    private String cnpj;
}
