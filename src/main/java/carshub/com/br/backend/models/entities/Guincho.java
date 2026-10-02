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

    @Column(name = "placa_Guincho", length = 7, unique = true)
    private String placa;

    @Column(name = "latitude_Guincho")
    private Double latitude;

    @Column(name = "longitude_Guincho")
    private Double longitude;
}