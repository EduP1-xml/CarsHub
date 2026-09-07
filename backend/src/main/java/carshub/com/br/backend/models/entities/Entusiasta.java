package carshub.com.br.backend.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import java.util.UUID;

@Entity
@Table(name = "Entusiasta")
@PrimaryKeyJoinColumn(name = "idEntusiasta")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Entusiasta extends Usuario {

    @NotBlank(message = "O CPF é obrigatório")
    @Pattern(regexp = "\\d{11}", message = "O CPF deve conter exatamente 11 dígitos numéricos")// define uma padrão exatp(nesse caso tamanho 11) para ser validado em tempo de compilação
    @Column(name = "cpf_entusiasta", length = 11, unique = true)
    private String cpf;

    @NotNull(message = "É necessário informar se possui carros")
    @Column(name = "possui_carros", nullable = false)
    private Boolean possuiCarros = false;
}