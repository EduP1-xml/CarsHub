package carshub.com.br.backend.models.entities;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Participacao de um entusiasta/colecionador em um clube.
 *
 * SEM id proprio: a chave e o par (idClube, idEntusiasta), e as duas colunas
 * sao FKs para os registros ja existentes (@MapsId). Guarda a data de entrada
 * e os carros que o membro vinculou ao clube (tabela
 * CarrosVinculados_Membro_Clube, chave = clube + usuario + carro).
 *
 * Os carros vinculados devem ser removidos com removeIf (iterador), nunca com
 * remove(carro): o hashCode do Carro (@Data) muda quando o carro e editado.
 */
@Entity
@Table(name = "Membro_Clube")
@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class MembroClube {

    @EmbeddedId
    @EqualsAndHashCode.Include
    private MembroClubeId id;

    @MapsId("idClube")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "idClube_MembroClube", nullable = false)
    private Clube clube;

    @MapsId("idEntusiasta")
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "idEntusiasta_MembroClube", nullable = false)
    private Entusiasta entusiasta;

    @Column(name = "dataEntrada_MembroClube", nullable = false, updatable = false)
    private LocalDateTime dataEntrada;

    @ManyToMany
    @JoinTable(
            name = "CarrosVinculados_Membro_Clube",
            joinColumns = {
                    @JoinColumn(name = "idClube", referencedColumnName = "idClube_MembroClube"),
                    @JoinColumn(name = "idEntusiasta", referencedColumnName = "idEntusiasta_MembroClube")
            },
            inverseJoinColumns = @JoinColumn(name = "idCarro", referencedColumnName = "id_Carro"))
    private Set<Carro> carrosVinculados = new HashSet<>();

    public MembroClube(Clube clube, Entusiasta entusiasta, LocalDateTime dataEntrada) {
        this.id = new MembroClubeId(clube.getId(), entusiasta.getId());
        this.clube = clube;
        this.entusiasta = entusiasta;
        this.dataEntrada = dataEntrada;
    }
}
