package carshub.com.br.backend.models.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;

import java.io.Serializable;
import java.sql.Types;
import java.util.UUID;

/**
 * Chave composta do membro: (id do clube, id do usuario ja existente).
 * Nenhum id novo e gerado para o membro.
 */
@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class MembroClubeId implements Serializable {

    @JdbcTypeCode(Types.BINARY)
    @Column(name = "idClube_MembroClube", nullable = false)
    private UUID idClube;

    @JdbcTypeCode(Types.BINARY)
    @Column(name = "idEntusiasta_MembroClube", nullable = false)
    private UUID idEntusiasta;
}
