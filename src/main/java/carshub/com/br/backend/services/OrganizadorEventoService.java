package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.models.dtos.perfis.patches.PerfilOrganizadorEventoPatchDTO;
import carshub.com.br.backend.models.dtos.perfis.puts.PerfilOrganizadorEventoPutDTO;
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
    public OrganizadorEvento substituirPerfil(UUID id, PerfilOrganizadorEventoPutDTO dto) {
        OrganizadorEvento organizador = buscarOrganizadorOuLancarExcecao(id);

        organizador.setNome(dto.nome());
        organizador.setSenha(dto.senha());
        organizador.setTelefone(dto.telefone());

        return organizadorRepository.save(organizador);
    }

    @Transactional
    public OrganizadorEvento atualizarPerfilCondicional(UUID id, PerfilOrganizadorEventoPatchDTO dto) {
        OrganizadorEvento organizador = buscarOrganizadorOuLancarExcecao(id);

        if (dto.nome() != null && !dto.nome().isBlank()) organizador.setNome(dto.nome());
        if (dto.senha() != null && !dto.senha().isBlank()) organizador.setSenha(dto.senha());
        if (dto.telefone() != null && !dto.telefone().isBlank()) organizador.setTelefone(dto.telefone());

        return organizadorRepository.save(organizador);
    }

    private OrganizadorEvento buscarOrganizadorOuLancarExcecao(UUID id) {
        return organizadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão inválida: Organizador não encontrado."));
    }
}