package carshub.com.br.backend.models.entities;

import carshub.com.br.backend.models.enums.ServicosOfertados;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "Mecanico")
@PrimaryKeyJoinColumn(name = "idMecanico")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Mecanico extends PrestadorServico {

    // Cria a tabela auxiliar para armazenar a lista de Enums selecionados pelo mecânico
    @ElementCollection(targetClass = ServicosOfertados.class, fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "servicos_ofertados_mecanico", joinColumns = @JoinColumn(name = "idMecanico"))
    @Column(name = "servico")
    private List<ServicosOfertados> servicos;
}