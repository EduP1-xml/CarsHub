package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
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

    private final EntusiastaRepository entusiastaRepository;

    //PUT
    @Transactional
    public Entusiasta substituirPerfil(UUID id, PerfilEntusiastaPutDTO dto) {
        Entusiasta entusiasta = buscarEntusiastaOuLancarExcecao(id);

        // Como o DTO é validado no Controller, temos certeza que os dados não são nulos.
        entusiasta.setNome(dto.nome());
        entusiasta.setSenha(dto.senha());
        entusiasta.setTelefone(dto.telefone());

        return entusiastaRepository.save(entusiasta);
    }

    //PATCH
    @Transactional
    public Entusiasta atualizarPerfilCondicional(UUID id, PerfilEntusiastaPatchDTO dto) {
        Entusiasta entusiasta = buscarEntusiastaOuLancarExcecao(id);

        if (dto.nome() != null && !dto.nome().isBlank()) entusiasta.setNome(dto.nome());
        if (dto.senha() != null && !dto.senha().isBlank()) entusiasta.setSenha(dto.senha());
        if (dto.telefone() != null && !dto.telefone().isBlank()) entusiasta.setTelefone(dto.telefone());

        return entusiastaRepository.save(entusiasta);
    }

    private Entusiasta buscarEntusiastaOuLancarExcecao(UUID id) {
        return entusiastaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão inválida: Entusiasta não encontrado."));
    }
}