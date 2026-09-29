package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.models.dtos.AlteracaoSenhaDTO;
import carshub.com.br.backend.models.dtos.perfis.patches.PerfilOrganizadorEventoPatchDTO;
import carshub.com.br.backend.models.dtos.perfis.puts.PerfilOrganizadorEventoPutDTO;
import carshub.com.br.backend.models.entities.OrganizadorEvento;
import carshub.com.br.backend.repositories.OrganizadorEventoRepository;
import carshub.com.br.backend.services.credenciais.AlteracaoSenhaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrganizadorEventoService {

    private final OrganizadorEventoRepository repositorioOrganizadorEvento;
    private final AlteracaoSenhaService servicoAlteracaoSenha;

    @Transactional
    public OrganizadorEvento substituirPerfil(UUID id, PerfilOrganizadorEventoPutDTO dados) {
        OrganizadorEvento organizador = buscarOrganizadorOuLancarExcecao(id);

        organizador.setNome(dados.nome());
        organizador.setTelefone(dados.telefone());

        return repositorioOrganizadorEvento.save(organizador);
    }

    @Transactional
    public OrganizadorEvento atualizarPerfilCondicional(UUID id, PerfilOrganizadorEventoPatchDTO dados) {
        OrganizadorEvento organizador = buscarOrganizadorOuLancarExcecao(id);

        if (dados.nome() != null && !dados.nome().isBlank()) organizador.setNome(dados.nome());
        if (dados.telefone() != null && !dados.telefone().isBlank()) organizador.setTelefone(dados.telefone());

        return repositorioOrganizadorEvento.save(organizador);
    }

    // --- Alteracao de senha (fluxo logado) ---

    @Transactional
    public void solicitarCodigoAlteracaoSenha(UUID id) {
        servicoAlteracaoSenha.solicitarCodigoAlteracao(buscarOrganizadorOuLancarExcecao(id));
    }

    @Transactional
    public void alterarSenha(UUID id, AlteracaoSenhaDTO dados) {
        servicoAlteracaoSenha.alterarSenha(buscarOrganizadorOuLancarExcecao(id), dados);
    }

    private OrganizadorEvento buscarOrganizadorOuLancarExcecao(UUID id) {
        return repositorioOrganizadorEvento.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão inválida: Organizador não encontrado."));
    }
}