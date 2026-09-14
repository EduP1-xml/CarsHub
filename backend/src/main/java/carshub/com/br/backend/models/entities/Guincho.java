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
@PrimaryKeyJoinColumn(name = "id_Guincho")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Guincho extends PrestadorServico {

    @NotBlank(message = "A placa do guincho é obrigatória")
    @Pattern(regexp = "^[A-Za-z0-9]{7}$", message = "A placa deve conter exatamente 7 caracteres alfanuméricos")
    @Column(name = "placa_Guincho", length = 7, unique = true, nullable = false)
    private String placa;
}