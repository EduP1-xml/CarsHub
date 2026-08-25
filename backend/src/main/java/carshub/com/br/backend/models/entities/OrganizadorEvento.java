package carshub.com.br.backend.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "OrganizadoEvento")
@Data
@NoArgsConstructor
public class OrganizadorEvento {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private Long id;

    @NotBlank(message = "O CNPJ é obrigatório para realização de atividades comerciais")
    @Pattern(rexexp = "\\d{14}", message = "O CNPJ deve conter 14 dígitos numéricos")
    @Column(name = "cnpj_organizadorEvento", length = 14, unique = true, nullable = false)
    private String cnpj;

    @NotNull
    @OneToOne(optional = false)
    @JoinColumn(name = "Usuario_id_Usuario", referencedColumnName = "id_Usuario", nullable = false)
    private Usuario usuario;
}
