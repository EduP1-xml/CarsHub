package carshub.com.br.backend.models.entities;

import carshub.com.br.backend.models.enums.tiposUsuarios.TipoUsuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "Entusiasta")
@PrimaryKeyJoinColumn(name = "idEntusiasta")
@Data
@EqualsAndHashCode(callSuper = true, exclude = "colecionador") // Evita loop de memória
@ToString(exclude = "colecionador")
@NoArgsConstructor
public class Entusiasta extends Usuario {

    @NotBlank(message = "O CPF é obrigatório")
    @Pattern(regexp = "\\d{11}", message = "O CPF deve conter exatamente 11 dígitos numéricos")
    @Column(name = "cpf_entusiasta", length = 11, unique = true)
    private String cpf;

    @NotNull(message = "É necessário informar se possui carros")
    @Column(name = "possui_carros", nullable = false)
    private Boolean possuiCarros = false;

    // Relacionamento de Composição
    @OneToOne(mappedBy = "entusiasta", cascade = CascadeType.ALL, orphanRemoval = true)
    private Colecionador colecionador;

    // ========================================================
    // MÉTODOS DE REGRA DE NEGÓCIO (POO)
    // ========================================================

    public void criaGaragem() {
        if (this.colecionador == null) {
            this.colecionador = new Colecionador();
            this.colecionador.setEntusiasta(this);
            this.setTipoUsuario(TipoUsuario.COLECIONADOR);
            this.setPossuiCarros(true);
        }
    }

    public void verificaExcluiGaragem() {
        // Se a coleção existe, mas a lista de carros ficou vazia...
        if (this.colecionador != null && this.colecionador.getCarros().isEmpty()) {
            this.colecionador = null; // O orphanRemoval do JPA vai deletar a tabela automaticamente!
            this.setTipoUsuario(TipoUsuario.ENTUSIASTA);
            this.setPossuiCarros(false);
        }
    }
}