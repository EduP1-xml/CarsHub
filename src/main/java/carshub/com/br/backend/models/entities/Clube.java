package carshub.com.br.backend.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Clube criado por um entusiasta (que vira o administrador). O gosto do clube
 * e o FiltroCarro: define quais eventos aparecem na aba Eventos e quais carros
 * os membros podem vincular.
 *
 * O administrador e os membros NAO ganham id novo: apontam para o Entusiasta
 * (ou Colecionador, que compartilha o mesmo id) ja existente.
 */
@Entity
@Table(name = "Clube")
@Getter
@Setter
@NoArgsConstructor
public class Clube {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.BINARY)
    @Column(name = "id_Clube", updatable = false, nullable = false)
    private UUID id;

    @NotBlank(message = "O nome do clube é obrigatório")
    @Size(max = 50, message = "O nome do clube não pode exceder 50 caracteres")
    @Column(name = "nome_Clube", length = 50, nullable = false, unique = true)
    private String nome;

    @Size(max = 500, message = "A descrição do clube não pode exceder 500 caracteres")
    @Column(name = "descricao_Clube", length = 500)
    private String descricao;

    @NotNull
    @Column(name = "dataCriacao_Clube", nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    /** Travamento otimista, como no Evento: duas edicoes/transferencias simultaneas -> 409. */
    @Version
    @Column(name = "versao_Clube")
    private Long versao;

    /** Exatamente 1 administrador por vez; a transferencia so troca esta FK. */
    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "idAdministrador_Clube", nullable = false)
    private Entusiasta administrador;

    @OneToMany(mappedBy = "clube", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<MembroClube> membros = new HashSet<>();

    @NotNull
    @OneToOne(optional = false, fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "idFiltroCarro_Clube", nullable = false, unique = true)
    private FiltroCarro filtroCarro;

    /** 0 a 5 fotos; o limite e validado no ClubeService, como as fotos do Evento. */
    @ElementCollection
    @CollectionTable(name = "FotosClube_Clube", joinColumns = @JoinColumn(name = "idClube_Clube"))
    @Column(name = "urlFotoClube_Clube")
    private List<String> fotosClube = new ArrayList<>();
}
