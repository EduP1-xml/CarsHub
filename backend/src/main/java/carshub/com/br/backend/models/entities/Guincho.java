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
@Table(name = "Guincho")
@PrimaryKeyJoinColumn(name = "idGuincho")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Guincho extends PrestadorServico {

    @NotBlank(message = "O CNPJ é obrigatório para motoristas de Guincho")
    @Pattern(regexp = "\\d{14}", message = "O CNPJ deve conter exatamente 14 dígitos numéricos")
    @Column(name = "cnpj_Guincho", length = 14, unique = true)
    private String cnpj;

    @NotBlank(message = "A placa do guincho é obrigatória")
    @Pattern(regexp = "\\d{7}", message = "A placa deve conter exatamente 7 dígitos")
    @Column(name = "placa_Guincho", length = 7, unique = true, nullable = false)
    private String placa;
}