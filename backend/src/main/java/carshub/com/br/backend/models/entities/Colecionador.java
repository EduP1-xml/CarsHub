package carshub.com.br.backend.models.entities;

import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "Colecionador")
@Data
@EqualsAndHashCode(exclude = {"entusiasta", "carros"}) // Evita loop infinito de verificação no Lombok
@NoArgsConstructor
public class Colecionador {

    @Id
    @Column(name = "id_Colecionador")
    private UUID id;

    // @MapsId diz ao JPA: "Use o UUID do Entusiasta como o meu próprio ID"
    @OneToOne
    @MapsId
    @JoinColumn(name = "id_Colecionador")
    private Entusiasta entusiasta;

    @OneToMany(mappedBy = "colecionador", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Carro> carros = new ArrayList<>();
}