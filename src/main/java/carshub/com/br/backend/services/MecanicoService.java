package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.UsuarioNaoEncontradoException;
import carshub.com.br.backend.models.dtos.updates.MecanicoUpdateDTO;
import carshub.com.br.backend.models.entities.Mecanico;
import carshub.com.br.backend.models.entities.PrestadorServico;

import carshub.com.br.backend.repositories.PrestadorServicoRespository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class MecanicoService {

    private final PrestadorServicoRespository prestadorServicoRespository;

    public MecanicoService(PrestadorServicoRespository prestadorServicoRespository) {
        this.prestadorServicoRespository = prestadorServicoRespository;
    }

    @Transactional
    public Mecanico atualizarPerfil(UUID id, MecanicoUpdateDTO dto) {

        // Busca usando o repositório da superclasse
        PrestadorServico prestador = prestadorServicoRespository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Sessão inválida: Prestador não encontrado."));

        // Garante que o usuário logado tentando acessar o endpoint de mecânico é, de fato, um mecânico
        if (!(prestador instanceof Mecanico mecanico)) {
            throw new RuntimeException("Acesso Negado: O perfil selecionado não é de um Mecânico.");
        }

        if (dto.nome() != null && !dto.nome().isBlank()) mecanico.setNome(dto.nome());
        if (dto.senha() != null && !dto.senha().isBlank()) mecanico.setSenha(dto.senha());
        if (dto.telefone() != null && !dto.telefone().isBlank()) mecanico.setTelefone(dto.telefone());


        if (dto.servicosOfertados() != null && !dto.servicosOfertados().isEmpty()) {
            mecanico.setServicos(dto.servicosOfertados());
        }

        // O save() da superclasse sabe persistir a classe filha no banco corretamente
        return prestadorServicoRespository.save(mecanico);
    }
}