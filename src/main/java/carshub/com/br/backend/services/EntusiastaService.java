package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.models.dtos.AlteracaoSenhaDTO;
import carshub.com.br.backend.models.dtos.LatitudeLongitudeDTO;
import carshub.com.br.backend.models.dtos.perfis.patches.PerfilEntusiastaPatchDTO;
import carshub.com.br.backend.models.dtos.perfis.puts.PerfilEntusiastaPutDTO;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.repositories.EntusiastaRepository;
import carshub.com.br.backend.services.credenciais.AlteracaoSenhaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EntusiastaService {

    private final EntusiastaRepository repositorioEntusiasta;
    private final AlteracaoSenhaService servicoAlteracaoSenha;

    @Transactional
    public Entusiasta substituirPerfil(UUID id, PerfilEntusiastaPutDTO dados) {
        Entusiasta entusiasta = buscarEntusiastaOuLancarExcecao(id);

        entusiasta.setNome(dados.nome());
        entusiasta.setTelefone(dados.telefone());

        return repositorioEntusiasta.save(entusiasta);
    }

    @Transactional
    public Entusiasta atualizarPerfilCondicional(UUID id, PerfilEntusiastaPatchDTO dados) {
        Entusiasta entusiasta = buscarEntusiastaOuLancarExcecao(id);

        if (dados.nome() != null && !dados.nome().isBlank()) entusiasta.setNome(dados.nome());
        if (dados.telefone() != null && !dados.telefone().isBlank()) entusiasta.setTelefone(dados.telefone());

        return repositorioEntusiasta.save(entusiasta);
    }

    @Transactional
    public Entusiasta atualizarLocalizacao(LatitudeLongitudeDTO dados, UUID id) {
        Entusiasta entusiasta = buscarEntusiastaOuLancarExcecao(id);
        atualizarCoordenadas(entusiasta, dados);
        return repositorioEntusiasta.save(entusiasta);
    }

    // --- Alteracao de senha (fluxo logado) ---

    @Transactional
    public void solicitarCodigoAlteracaoSenha(UUID id) {
        servicoAlteracaoSenha.solicitarCodigoAlteracao(buscarEntusiastaOuLancarExcecao(id));
    }

    @Transactional
    public void alterarSenha(UUID id, AlteracaoSenhaDTO dados) {
        servicoAlteracaoSenha.alterarSenha(buscarEntusiastaOuLancarExcecao(id), dados);
    }

    private Entusiasta buscarEntusiastaOuLancarExcecao(UUID id) {
        return repositorioEntusiasta.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão inválida: Entusiasta não encontrado."));
    }

    private void atualizarCoordenadas(Entusiasta entusiasta, LatitudeLongitudeDTO dados) {
        if (dados.latitude() != null) {
            entusiasta.setLatitude(dados.latitude());
        }
        if (dados.longitude() != null) {
            entusiasta.setLongitude(dados.longitude());
        }
    }
}