package carshub.com.br.backend.models.dtos.servicos;

import carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados;
import carshub.com.br.backend.models.enums.servicos.StatusServico;
import carshub.com.br.backend.models.enums.servicos.TipoServico;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
//Telefone só é preenchido quando a proposta é aceita
public record ServicoRespostaDTO(
        UUID idServico,
        TipoServico tipoServico,
        StatusServico status,
        BigDecimal valorProposto,

        UUID idEntusiasta,
        String nomeEntusiasta,
        String telefoneEntusiasta,

        UUID idPrestador,
        String nomePrestador,
        String telefonePrestador,

        UUID idCarro,
        String carroDescricao,
        List<ServicosOfertados> servicosSolicitados,

        Double latitudeEntusiasta,
        Double longitudeEntusiasta,
        String enderecoOcorrencia,

        Double latitudePrestador,
        Double longitudePrestador,

        Boolean confirmacaoConclusaoEntusiasta,
        Boolean confirmacaoConclusaoPrestador,

        LocalDateTime dataHoraSolicitacao,
        LocalDateTime dataHoraProposta,
        LocalDateTime dataHoraFinalizacao
) {}