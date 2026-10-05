package carshub.com.br.backend.models.dtos.servicos;

import carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados;
import carshub.com.br.backend.models.enums.servicos.TipoServico;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record HistoricoManutencaoResponseDTO(
        UUID idServico,
        TipoServico tipoServico,
        String nomePrestador,
        List<ServicosOfertados> servicosRealizados,
        String enderecoOcorrencia,
        BigDecimal valorProposto,
        LocalDateTime dataHoraFinalizacao
) {}