package carshub.com.br.backend.models.enums.servicos;

/**
 * Ciclo de vida compartilhado pelos dois tipos de servico (mecanico e guincho).
 *
 * SOLICITADO -> na rede, sem prestador vinculado.
 * PROPOSTA   -> prestador vinculado + valor enviado, aguardando a decisao.
 * PENDENTE   -> proposta aceita: prestador a caminho. dataHoraPendente marca o inicio da contagem das 2 horas que liberam o cancelamento.
 * CHEGOU     -> as DUAS partes confirmaram a chegada do prestador ao local.
 * FINALIZADO -> as duas partes enviaram a foto final, avaliaram e confirmaram.
 */
public enum StatusServico {
    SOLICITADO,
    PROPOSTA,
    PENDENTE,
    CHEGOU,
    FINALIZADO
}