package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.UsuarioNaoEncontradoException;
import carshub.com.br.backend.models.dtos.updates.OrganizadorEventoUpdateDTO;
import carshub.com.br.backend.models.entities.OrganizadorEvento;
import carshub.com.br.backend.repositories.OrganizadorEventoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class OrganizadorEventoService {

    private final OrganizadorEventoRepository organizadorRepository;

    public OrganizadorEventoService(OrganizadorEventoRepository organizadorRepository) {
        this.organizadorRepository = organizadorRepository;
    }

    @Transactional
    public OrganizadorEvento atualizarPerfil(UUID id, OrganizadorEventoUpdateDTO dto) {
        OrganizadorEvento organizador = organizadorRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Sessão inválida: Organizador não encontrado."));

        if (dto.nome() != null && !dto.nome().isBlank()) organizador.setNome(dto.nome());
        if (dto.senha() != null && !dto.senha().isBlank()) organizador.setSenha(dto.senha());
        if (dto.telefone() != null && !dto.telefone().isBlank()) organizador.setTelefone(dto.telefone());

        return organizadorRepository.save(organizador);
    }
}