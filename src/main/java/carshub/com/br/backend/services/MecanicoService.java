package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.PerfilIncompativelException;
import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.models.dtos.perfis.patches.PerfilMecanicoPatchDTO;
import carshub.com.br.backend.models.dtos.perfis.puts.PerfilMecanicoPutDTO;
import carshub.com.br.backend.models.entities.Mecanico;
import carshub.com.br.backend.models.entities.PrestadorServico;
import carshub.com.br.backend.repositories.PrestadorServicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MecanicoService {

    private final PrestadorServicoRepository prestadorRepository;

    public MecanicoService(PrestadorServicoRepository prestadorRepository) {
        this.prestadorRepository = prestadorRepository;
    }

    @Transactional
    public Mecanico substituirPerfil(UUID id, PerfilMecanicoPutDTO dto) {
        Mecanico mecanico = buscarEValidarMecanico(id);

        mecanico.setNome(dto.nome());
        mecanico.setSenha(dto.senha());
        mecanico.setTelefone(dto.telefone());
        mecanico.setServicos(dto.servicosOfertados());

        return prestadorRepository.save(mecanico);
    }

    @Transactional
    public Mecanico atualizarPerfilCondicional(UUID id, PerfilMecanicoPatchDTO dto) {
        Mecanico mecanico = buscarEValidarMecanico(id);

        if (dto.nome() != null && !dto.nome().isBlank()) mecanico.setNome(dto.nome());
        if (dto.senha() != null && !dto.senha().isBlank()) mecanico.setSenha(dto.senha());
        if (dto.telefone() != null && !dto.telefone().isBlank()) mecanico.setTelefone(dto.telefone());

        if (dto.servicosOfertados() != null && !dto.servicosOfertados().isEmpty()) {
            mecanico.setServicos(dto.servicosOfertados());
        }

        return prestadorRepository.save(mecanico);
    }

    private Mecanico buscarEValidarMecanico(UUID id) {
        PrestadorServico prestador = prestadorRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão inválida: Prestador não encontrado."));

        if (!(prestador instanceof Mecanico mecanico)) {
            throw new PerfilIncompativelException("Acesso Negado: O perfil selecionado não é de um Mecânico.");
        }
        return mecanico;
    }
}