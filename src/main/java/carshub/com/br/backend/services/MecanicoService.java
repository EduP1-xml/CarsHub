package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.PerfilIncompativelException;
import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.models.dtos.AlteracaoSenhaDTO;
import carshub.com.br.backend.models.dtos.LatitudeLongitudeDTO;
import carshub.com.br.backend.models.dtos.perfis.patches.PerfilMecanicoPatchDTO;
import carshub.com.br.backend.models.dtos.perfis.puts.PerfilMecanicoPutDTO;
import carshub.com.br.backend.models.entities.Mecanico;
import carshub.com.br.backend.models.entities.PrestadorServico;
import carshub.com.br.backend.repositories.PrestadorServicoRepository;
import carshub.com.br.backend.services.credenciais.AlteracaoSenhaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MecanicoService {

    private final PrestadorServicoRepository repositorioPrestadorServico;
    private final AlteracaoSenhaService servicoAlteracaoSenha;

    @Transactional
    public Mecanico substituirPerfil(UUID id, PerfilMecanicoPutDTO dados) {
        Mecanico mecanico = buscarEValidarMecanico(id);

        mecanico.setNome(dados.nome());
        mecanico.setTelefone(dados.telefone());
        mecanico.setServicos(dados.servicosOfertados());

        return repositorioPrestadorServico.save(mecanico);
    }

    @Transactional
    public Mecanico atualizarPerfilCondicional(UUID id, PerfilMecanicoPatchDTO dados) {
        Mecanico mecanico = buscarEValidarMecanico(id);

        if (dados.nome() != null && !dados.nome().isBlank()) mecanico.setNome(dados.nome());
        if (dados.telefone() != null && !dados.telefone().isBlank()) mecanico.setTelefone(dados.telefone());

        if (dados.servicosOfertados() != null && !dados.servicosOfertados().isEmpty()) {
            mecanico.setServicos(dados.servicosOfertados());
        }

        return repositorioPrestadorServico.save(mecanico);
    }

    @Transactional
    public Mecanico atualizarLocalizacao(UUID id, LatitudeLongitudeDTO dados) {
        Mecanico mecanico = buscarEValidarMecanico(id);

        if (dados.latitude() == null || dados.longitude() == null) {
            throw new IllegalArgumentException("Informe latitude e longitude para atualizar a localização.");
        }

        mecanico.setLatitude(dados.latitude());
        mecanico.setLongitude(dados.longitude());

        return repositorioPrestadorServico.save(mecanico);
    }

    // --- Alteracao de senha (fluxo logado) ---

    @Transactional
    public void solicitarCodigoAlteracaoSenha(UUID id) {
        servicoAlteracaoSenha.solicitarCodigoAlteracao(buscarEValidarMecanico(id));
    }

    @Transactional
    public void alterarSenha(UUID id, AlteracaoSenhaDTO dados) {
        servicoAlteracaoSenha.alterarSenha(buscarEValidarMecanico(id), dados);
    }

    private Mecanico buscarEValidarMecanico(UUID id) {
        PrestadorServico prestador = repositorioPrestadorServico.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão inválida: Prestador não encontrado."));

        if (!(prestador instanceof Mecanico mecanico)) {
            throw new PerfilIncompativelException("Acesso Negado: O perfil selecionado não é de um Mecânico.");
        }
        return mecanico;
    }
}