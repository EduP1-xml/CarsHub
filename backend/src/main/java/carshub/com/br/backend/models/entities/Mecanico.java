package carshub.com.br.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Mecanico")
@PrimaryKeyJoinColumn(name = "idMecanico")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Mecanico extends PrestadorServico {

    @NotBlank(message = "O CNPJ é obrigatório para mecânicos")
    @Pattern(regexp = "\\d{14}", message = "O CNPJ deve conter exatamente 14 dígitos numéricos")
    @Column(name = "cnpj_mecanico", length = 14, unique = true)
    private String cnpj;
}