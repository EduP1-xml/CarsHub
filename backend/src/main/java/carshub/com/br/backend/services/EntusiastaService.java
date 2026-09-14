package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.UsuarioNaoEncontradoException;
import carshub.com.br.backend.models.dtos.updates.EntusiastaUpdateDTO;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.repositories.EntusiastaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class EntusiastaService {

    private final EntusiastaRepository entusiastaRepository;

    public EntusiastaService(EntusiastaRepository entusiastaRepository) {
        this.entusiastaRepository = entusiastaRepository;
    }

    @Transactional
    public Entusiasta atualizarPerfil(UUID id, EntusiastaUpdateDTO dto) {
        Entusiasta entusiasta = entusiastaRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Sessão inválida: Entusiasta não encontrado."));

        if (dto.nome() != null && !dto.nome().isBlank()) entusiasta.setNome(dto.nome());
        if (dto.senha() != null && !dto.senha().isBlank()) entusiasta.setSenha(dto.senha());
        if (dto.telefone() != null && !dto.telefone().isBlank()) entusiasta.setTelefone(dto.telefone());

        return entusiastaRepository.save(entusiasta);
    }
}