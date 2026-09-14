package carshub.com.br.backend.models.entities;

import carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados;
import jakarta.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@Table(name = "Mecanico")
@PrimaryKeyJoinColumn(name = "id_Mecanico")
@Data
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
public class Mecanico extends PrestadorServico {

    // Cria a tabela auxiliar para armazenar a lista de Enums selecionados pelo mecânico
    @ElementCollection(targetClass = ServicosOfertados.class, fetch = FetchType.EAGER)
    @Enumerated(EnumType.STRING)
    @CollectionTable(name = "ServicosOfertados_Mecanico", joinColumns = @JoinColumn(name = "id_Mecanico"))
    @Column(name = "servicos_Mecanico")
    private List<ServicosOfertados> servicos;
}