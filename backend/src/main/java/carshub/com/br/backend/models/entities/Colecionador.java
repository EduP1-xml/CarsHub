package carshub.com.br.backend.models.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.PrimaryKeyJoinColumn;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "Colecionador")
@PrimaryKeyJoinColumn(name = "idColecionador")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Colecionador extends Entusiasta {
    // A classe fica sem atributos no momento, atuando para habilitar lógicas e relacionamentos específicos no futuro, como os Ingressos VIP.
}
