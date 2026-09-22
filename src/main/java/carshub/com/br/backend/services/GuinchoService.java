package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.PlacaJaCadastradaException;
import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.models.dtos.perfis.patches.PerfilGuinchoPatchDTO;
import carshub.com.br.backend.models.dtos.perfis.puts.PerfilGuinchoPutDTO;
import carshub.com.br.backend.models.entities.Guincho;
import carshub.com.br.backend.repositories.GuinchoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GuinchoService {

    private final GuinchoRepository guinchoRepository;

    public GuinchoService(GuinchoRepository guinchoRepository) {
        this.guinchoRepository = guinchoRepository;
    }

    @Transactional
    public Guincho substituirPerfil(UUID id, PerfilGuinchoPutDTO dto) {
        Guincho guincho = buscarGuinchoOuLancarExcecao(id);

        validarDuplicidadePlaca(dto.placa(), guincho.getPlaca());

        guincho.setNome(dto.nome());
        guincho.setSenha(dto.senha());
        guincho.setTelefone(dto.telefone());
        guincho.setPlaca(dto.placa());

        return guinchoRepository.save(guincho);
    }

    @Transactional
    public Guincho atualizarPerfilCondicional(UUID id, PerfilGuinchoPatchDTO dto) {
        Guincho guincho = buscarGuinchoOuLancarExcecao(id);

        if (dto.nome() != null && !dto.nome().isBlank()) guincho.setNome(dto.nome());
        if (dto.senha() != null && !dto.senha().isBlank()) guincho.setSenha(dto.senha());
        if (dto.telefone() != null && !dto.telefone().isBlank()) guincho.setTelefone(dto.telefone());

        if (dto.placa() != null && !dto.placa().isBlank()) {
            validarDuplicidadePlaca(dto.placa(), guincho.getPlaca());
            guincho.setPlaca(dto.placa());
        }

        return guinchoRepository.save(guincho);
    }

    private Guincho buscarGuinchoOuLancarExcecao(UUID id) {
        return guinchoRepository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Sessão inválida: Guincho não encontrado."));
    }

    private void validarDuplicidadePlaca(String novaPlaca, String placaAtual) {
        if (!novaPlaca.equals(placaAtual) && guinchoRepository.existsByPlaca(novaPlaca)) {
            throw new PlacaJaCadastradaException("Esta placa já está registrada para outro guincho.");
        }
    }
}