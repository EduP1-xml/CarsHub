package carshub.com.br.backend.services.servicos;

import carshub.com.br.backend.exceptions.PerfilIncompativelException;
import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.models.dtos.servicos.HistoricoManutencaoResponseDTO;
import carshub.com.br.backend.models.dtos.servicos.ServicoRespostaDTO;
import carshub.com.br.backend.models.entities.Carro;
import carshub.com.br.backend.models.entities.servicos.ServicoGuincho;
import carshub.com.br.backend.models.entities.servicos.ServicoMecanico;
import carshub.com.br.backend.models.enums.servicos.StatusServico;
import carshub.com.br.backend.models.enums.servicos.TipoServico;
import carshub.com.br.backend.repositories.CarroRepository;
import carshub.com.br.backend.repositories.servicos.ServicoGuinchoRepository;
import carshub.com.br.backend.repositories.servicos.ServicoMecanicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Consultas que CRUZAM os dois tipos de servico: o painel padrao de solicitacoes
 * ativas do colecionador e o historico de manutencoes de um veiculo.
 */
@Service
@RequiredArgsConstructor
public class PainelServicoService {

    private final ServicoMecanicoService servicoMecanico;
    private final ServicoGuinchoService servicoGuincho;
    private final ServicoMecanicoRepository repositorioServicoMecanico;
    private final ServicoGuinchoRepository repositorioServicoGuincho;
    private final CarroRepository repositorioCarro;

    /**
     * Painel padrao (/api/carshub/colecionador/servicos): mecanicos e guinchos
     * ATIVOS na mesma lista, do mais recente para o mais antigo.
     */
    @Transactional(readOnly = true)
    public List<ServicoRespostaDTO> listarSolicitacoesAtivas(UUID idEntusiasta) {
        List<ServicoRespostaDTO> solicitacoes = new ArrayList<>();
        solicitacoes.addAll(servicoMecanico.listarMeusServicos(idEntusiasta, true));
        solicitacoes.addAll(servicoGuincho.listarMeusServicos(idEntusiasta, true));

        solicitacoes.sort(Comparator.comparing(ServicoRespostaDTO::dataHoraSolicitacao,
                Comparator.nullsLast(Comparator.reverseOrder())));

        return solicitacoes;
    }

    /**
     * Historico de manutencoes oficial do veiculo: todos os servicos mecanicos e
     * guinchos FINALIZADOS daquele carro, do mais recente para o mais antigo.
     */
    @Transactional(readOnly = true)
    public List<HistoricoManutencaoResponseDTO> obterHistoricoDoVeiculo(UUID idCarro, UUID idEntusiasta) {
        if (idCarro == null) {
            throw new IllegalArgumentException("O ID do carro é obrigatório para consultar o histórico.");
        }

        Carro carro = repositorioCarro.findById(idCarro)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carro não encontrado."));

        if (!carro.getColecionador().getId().equals(idEntusiasta)) {
            throw new PerfilIncompativelException("Acesso Negado: Este veículo não pertence à sua coleção.");
        }

        List<HistoricoManutencaoResponseDTO> historico = new ArrayList<>();

        List<ServicoMecanico> servicosMecanicos = repositorioServicoMecanico
                .findAllByCarro_IdAndStatusOrderByDataHoraFinalizacaoDesc(idCarro, StatusServico.FINALIZADO);

        for (ServicoMecanico servico : servicosMecanicos) {
            historico.add(new HistoricoManutencaoResponseDTO(
                    servico.getId(),
                    TipoServico.MECANICO,
                    servico.getMecanico() != null ? servico.getMecanico().getNome() : null,
                    new ArrayList<>(servico.getServicos()),
                    servico.getEnderecoAtualEntusiasta(),
                    servico.getValorProposto(),
                    servico.getDataHoraFinalizacao()));
        }

        List<ServicoGuincho> servicosGuincho = repositorioServicoGuincho
                .findAllByCarro_IdAndStatusOrderByDataHoraFinalizacaoDesc(idCarro, StatusServico.FINALIZADO);

        for (ServicoGuincho servico : servicosGuincho) {
            historico.add(new HistoricoManutencaoResponseDTO(
                    servico.getId(),
                    TipoServico.GUINCHO,
                    servico.getGuincho() != null ? servico.getGuincho().getNome() : null,
                    List.of(),
                    servico.getEnderecoAtualEntusiasta(),
                    servico.getValorProposto(),
                    servico.getDataHoraFinalizacao()));
        }

        historico.sort(Comparator.comparing(HistoricoManutencaoResponseDTO::dataHoraFinalizacao,
                Comparator.nullsLast(Comparator.reverseOrder())));

        return historico;
    }
}