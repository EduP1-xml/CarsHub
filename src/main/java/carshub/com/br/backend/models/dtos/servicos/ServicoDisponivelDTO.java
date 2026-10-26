package carshub.com.br.backend.models.dtos.servicos;

import carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados;

import java.util.List;
import java.util.UUID;

/**
 * Chamado ainda na rede (SOLICITADO), visto pelo prestador no painel.
 * Painel do Mecanico: idServico, nomeEntusiasta, distanciaKm, servicosSolicitados
 * e carroDescricao. Painel do Guincho: os mesmos, com servicosSolicitados vazio.
 * Nao expoe valor, status interno nem telefone: o contato do colecionador so e
 * liberado depois que a proposta dele for aceita.
 *
 * AJUSTE 1.5: notaMatch voltou ao painel como INFORMACAO (o prestador decide se
 * aceita). A ordenacao continua por distancia — o score nao reordena a lista.
 * A notaMatch e calculada NA LEITURA (dinamica), com as coordenadas do PERFIL do
 * prestador; null quando o prestador nao tem coordenadas (sem distancia nao ha match).
 */
public record ServicoDisponivelDTO(
        UUID idServico,
        String nomeEntusiasta,
        Double distanciaKm,
        List<ServicosOfertados> servicosSolicitados,
        String carroDescricao,
        Integer notaMatch
) {}
