package carshub.com.br.backend.models.dtos.servicos;

import carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados;
import carshub.com.br.backend.models.enums.servicos.TipoServico;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Chamado ainda na rede (SOLICITADO), visto pelo prestador no painel.
 *
 * Nao expoe valor, status interno nem telefone: o contato do colecionador so e
 * liberado depois que a proposta dele for aceita.
 */
public record ServicoDisponivelDTO(
        UUID idServico,
        TipoServico tipoServico,
        String nomeEntusiasta,
        String carroDescricao,
        List<ServicosOfertados> servicosSolicitados,
        String enderecoOcorrencia,
        Double latitudeEntusiasta,
        Double longitudeEntusiasta,
        Double distanciaKm,
        LocalDateTime dataHoraSolicitacao
) {}