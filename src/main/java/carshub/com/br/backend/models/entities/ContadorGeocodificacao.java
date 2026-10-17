package carshub.com.br.backend.models.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Contagem mensal de requisicoes feitas ao Google Geocoding.
 * Uma linha por mes no formato AAAA-MM. Mantem contagem em memória e grv no bnco  cada 1H.
 */
@Entity
@Table(name = "ContadorGeocodificacao")
@Data
@NoArgsConstructor
public class ContadorGeocodificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(Types.BINARY)
    @Column(name = "id_ContadorGeocodificacao", updatable = false, nullable = false, columnDefinition = "BINARY(16)")
    private UUID id;

    // Mes de referencia no formato AAAA-MM
    @NotBlank
    @Column(name = "mesReferencia_ContadorGeocodificacao", length = 7, nullable = false, unique = true)
    private String mesReferencia;

    @NotNull
    @Column(name = "quantidadeRequisicoes_ContadorGeocodificacao", nullable = false)
    private Long quantidadeRequisicoes = 0L;

    @Column(name = "dataHoraAtualizacao_ContadorGeocodificacao")
    private LocalDateTime dataHoraAtualizacao;
}