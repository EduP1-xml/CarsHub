package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.PlacaJaCadastradaException;
import carshub.com.br.backend.exceptions.UsuarioNaoEncontradoException;
import carshub.com.br.backend.models.dtos.updates.GuinchoUpdateDTO;
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
    public Guincho atualizarPerfil(UUID id, GuinchoUpdateDTO dto) {
        Guincho guincho = guinchoRepository.findById(id)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Sessão inválida: Guincho não encontrado."));

        if (dto.nome() != null && !dto.nome().isBlank()) guincho.setNome(dto.nome());
        if (dto.senha() != null && !dto.senha().isBlank()) guincho.setSenha(dto.senha());
        if (dto.telefone() != null && !dto.telefone().isBlank()) guincho.setTelefone(dto.telefone());

        // Regra específica: Se tentar alterar a placa, verifica duplicidade no banco
        if (dto.placa() != null && !dto.placa().isBlank() && !dto.placa().equals(guincho.getPlaca())) {
            if (guinchoRepository.existsByPlaca(dto.placa())) {
                throw new PlacaJaCadastradaException("Esta placa já está registrada para outro guincho.");
            }
            guincho.setPlaca(dto.placa());
        }

        return guinchoRepository.save(guincho);
    }
}