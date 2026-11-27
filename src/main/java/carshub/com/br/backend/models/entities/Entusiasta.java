package carshub.com.br.backend.models.entities;

import carshub.com.br.backend.models.entities.servicos.EnderecoOcorrencia;
import carshub.com.br.backend.models.enums.tiposUsuarios.TipoUsuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "Entusiasta")
@PrimaryKeyJoinColumn(name = "id_Entusiasta")
@Data
@EqualsAndHashCode(callSuper = true, exclude = "colecionador") // Evita loop de memória
@ToString(exclude = "colecionador")
@NoArgsConstructor
public class Entusiasta extends Usuario {

    @Column(name = "cpf_Entusiasta", length = 11, unique = true)
    private String cpf;

    @NotNull(message = "É necessário informar se possui carros")
    @Column(name = "possuiCarros_Entusiasta", nullable = false)
    private Boolean possuiCarros = false;

    // Relacionamento de Composição
    @OneToOne(mappedBy = "entusiasta", cascade = CascadeType.ALL, orphanRemoval = true)
    private Colecionador colecionador;

    @Column(name = "latitude_Entusiasta")
    private Double latitude;

    @Column(name = "longitude_Entusiasta")
    private Double longitude;

    /*
     * Endereco das coordenadas acima (cache da geocodificacao). Quando o chamado
     * usa a localizacao do perfil, este endereco e reaproveitado e a Geocoding API
     * NAO e chamada. E apagado sempre que as coordenadas mudam sem endereco novo
     * (ex.: PUT /localizacao manual), para nunca ficar dessincronizado.
     */
    @Column(name = "enderecoFormatado_Entusiasta", length = 255)
    private String enderecoFormatado;

    @Embedded
    @AttributeOverrides({
            @AttributeOverride(name = "rua", column = @Column(name = "ruaEndereco_Entusiasta", length = 150)),
            @AttributeOverride(name = "numero", column = @Column(name = "numeroEndereco_Entusiasta", length = 20)),
            @AttributeOverride(name = "bairro", column = @Column(name = "bairroEndereco_Entusiasta", length = 100)),
            @AttributeOverride(name = "cidade", column = @Column(name = "cidadeEndereco_Entusiasta", length = 100)),
            @AttributeOverride(name = "estado", column = @Column(name = "estadoEndereco_Entusiasta", length = 50)),
            @AttributeOverride(name = "cep", column = @Column(name = "cepEndereco_Entusiasta", length = 10)),
            @AttributeOverride(name = "placeId", column = @Column(name = "placeIdEndereco_Entusiasta", length = 255))
    })
    private EnderecoOcorrencia enderecoEstruturado;

    /** Coordenadas mudaram sem endereco conhecido: descarta o cache do endereco. */
    public void limparEnderecoDaLocalizacao() {
        this.enderecoFormatado = null;
        this.enderecoEstruturado = null;
    }

    /** Ultima solicitacao de servico criada por este colecionador (cooldown de 5 minutos). */
    @Column(name = "dataHoraUltimaSolicitacaoServico_Entusiasta")
    private LocalDateTime dataHoraUltimaSolicitacaoServico;

    // ========================================================
    // REPUTACAO (item 2 - avaliacao do prestador sobre o colecionador)
    // ========================================================

    /** Media crua das notas dadas pelos prestadores a este entusiasta/colecionador. */
    @Column(name = "notaMediaEntusiasta_Entusiasta")
    private Double notaMediaEntusiasta = 0.0;

    /** Quantidade de avaliacoes que compoem a media acima. */
    @Column(name = "quantidadeAvaliacoes_Entusiasta")
    private Long quantidadeAvaliacoes = 0L;

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