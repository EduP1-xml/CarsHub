package carshub.com.br.backend.models.entities.servicos;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Endereco estruturado da ocorrencia (decisao A6): em vez de guardar so a string
 * formatada, guardamos os componentes e o placeId do Google, que e a chave
 * estavel do local (permite refazer a consulta sem ambiguidade e trocar de
 * provedor de mapas sem perder dado).
 *
 * As colunas seguem o padrao atributo_Classe usando o nome do embeddable, que e
 * o mesmo conjunto nas duas tabelas de servico.
 */
@Embeddable
@Data
@NoArgsConstructor
public class EnderecoOcorrencia {

    @Column(name = "rua_EnderecoOcorrencia", length = 150)
    private String rua;

    @Column(name = "numero_EnderecoOcorrencia", length = 20)
    private String numero;

    @Column(name = "bairro_EnderecoOcorrencia", length = 100)
    private String bairro;

    @Column(name = "cidade_EnderecoOcorrencia", length = 100)
    private String cidade;

    @Column(name = "estado_EnderecoOcorrencia", length = 50)
    private String estado;

    @Column(name = "cep_EnderecoOcorrencia", length = 10)
    private String cep;

    @Column(name = "placeId_EnderecoOcorrencia", length = 255)
    private String placeId;
}