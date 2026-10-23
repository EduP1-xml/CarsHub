package carshub.com.br.backend.models.dtos.servicos;

import carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados;
import carshub.com.br.backend.models.enums.servicos.StatusServico;
import carshub.com.br.backend.models.enums.servicos.TipoServico;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Visao completa de uma solicitacao, usada pelo colecionador e pelo prestador.
 *
 * PRIVACIDADE: os telefones so sao preenchidos a partir de PENDENTE.
 *
 * quantidadeMecanicosElegiveis: quantos mecanicos ativos ofertam TODOS os servicos
 * (resolvido na criacao). Zero significa "ninguem compativel agora" e o app pode
 * avisar o colecionador. No guincho vem null (nao ha catalogo de servicos).
 *
 * MATCH: os 5 campos finais so existem quando ha prestador vinculado.
 *  - notaMediaPrestador: NOTA AJUSTADA (bayesiana, neutro 4.0), nao a media crua;
 *  - servicosAtivosDoPrestador: carga ativa EXCLUINDO o proprio chamado (0 ou 1);
 *  - distanciaAteOcorrencia: do ponto declarado na proposta (ou do perfil) ate a ocorrencia.
 */
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
        Integer quantidadeMecanicosElegiveis,

        Double latitudeEntusiasta,
        Double longitudeEntusiasta,
        String enderecoOcorrencia,
        EnderecoOcorrenciaDTO enderecoEstruturado,

        Double latitudePrestador,
        Double longitudePrestador,

        String urlFotoInicial,

        Boolean confirmacaoChegadaEntusiasta,
        Boolean confirmacaoChegadaPrestador,
        LocalDateTime dataHoraChegou,

        String urlFotoFinalEntusiasta,
        String urlFotoFinalPrestador,

        Integer notaAvaliacaoEntusiasta,
        String comentarioAvaliacaoEntusiasta,
        LocalDateTime dataHoraAvaliacaoEntusiasta,

        Integer notaAvaliacaoPrestador,
        String comentarioAvaliacaoPrestador,
        LocalDateTime dataHoraAvaliacaoPrestador,

        Boolean confirmacaoConclusaoEntusiasta,
        Boolean confirmacaoConclusaoPrestador,

        LocalDateTime dataHoraSolicitacao,
        LocalDateTime dataHoraProposta,
        LocalDateTime dataHoraPendente,
        LocalDateTime dataHoraFinalizacao,

        Integer notaMatch,
        Double notaMediaPrestador,
        Long quantidadeAvaliacoes,
        Double distanciaAteOcorrencia,
        Long servicosAtivosDoPrestador
) {}