package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.models.dtos.LatitudeLongitudeDTO;
import carshub.com.br.backend.models.dtos.perfis.patches.PerfilEntusiastaPatchDTO;
import carshub.com.br.backend.models.dtos.perfis.puts.PerfilEntusiastaPutDTO;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.repositories.EntusiastaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EntusiastaService {

    private final EntusiastaRepository repositorioEntusiasta;

    @Transactional
    public Entusiasta substituirPerfil(UUID id, PerfilEntusiastaPutDTO dados) {
        Entusiasta entusiasta = buscarEntusiastaOuLancarExcecao(id);

        entusiasta.setNome(dados.nome());
        entusiasta.setSenha(dados.senha());
        entusiasta.setTelefone(dados.telefone());

        return repositorioEntusiasta.save(entusiasta);
    }

    @Transactional
    public Entusiasta atualizarPerfilCondicional(UUID id, PerfilEntusiastaPatchDTO dados) {
        Entusiasta entusiasta = buscarEntusiastaOuLancarExcecao(id);

        if (dados.nome() != null && !dados.nome().isBlank()) entusiasta.setNome(dados.nome());
        if (dados.senha() != null && !dados.senha().isBlank()) entusiasta.setSenha(dados.senha());
        if (dados.telefone() != null && !dados.telefone().isBlank()) entusiasta.setTelefone(dados.telefone());

        return repositorioEntusiasta.save(entusiasta);
    }

    @Transactional
    public Entusiasta atualizarLocalizacao(LatitudeLongitudeDTO dados, UUID id) {
        Entusiasta entidadeEntusiasta = buscarEntusiastaOuLancarExcecao(id);
        atualizarCoordenadas(entidadeEntusiasta, dados);
        return repositorioEntusiasta.save(entidadeEntusiasta);
    }

    private Entusiasta buscarEntusiastaOuLancarExcecao(UUID id) {
        return repositorioEntusiasta.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão inválida: Entusiasta não encontrado."));
    }

    private void atualizarCoordenadas(Entusiasta entidade, LatitudeLongitudeDTO dados) {
        if (dados.latitude() != null) {
            entidade.setLatitude(dados.latitude());
        }
        if (dados.longitude() != null) {
            entidade.setLongitude(dados.longitude());
        }
    }
}