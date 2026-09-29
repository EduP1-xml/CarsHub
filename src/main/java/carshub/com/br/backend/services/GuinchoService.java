package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.PlacaJaCadastradaException;
import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.models.dtos.AlteracaoSenhaDTO;
import carshub.com.br.backend.models.dtos.perfis.patches.PerfilGuinchoPatchDTO;
import carshub.com.br.backend.models.dtos.perfis.puts.PerfilGuinchoPutDTO;
import carshub.com.br.backend.models.entities.Guincho;
import carshub.com.br.backend.repositories.GuinchoRepository;
import carshub.com.br.backend.services.credenciais.AlteracaoSenhaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GuinchoService {

    private final GuinchoRepository repositorioGuincho;
    private final AlteracaoSenhaService servicoAlteracaoSenha;

    @Transactional
    public Guincho substituirPerfil(UUID id, PerfilGuinchoPutDTO dados) {
        Guincho guincho = buscarGuinchoOuLancarExcecao(id);

        validarDuplicidadePlaca(dados.placa(), guincho.getPlaca());

        guincho.setNome(dados.nome());
        guincho.setTelefone(dados.telefone());
        guincho.setPlaca(dados.placa());

        return repositorioGuincho.save(guincho);
    }

    @Transactional
    public Guincho atualizarPerfilCondicional(UUID id, PerfilGuinchoPatchDTO dados) {
        Guincho guincho = buscarGuinchoOuLancarExcecao(id);

        if (dados.nome() != null && !dados.nome().isBlank()) guincho.setNome(dados.nome());
        if (dados.telefone() != null && !dados.telefone().isBlank()) guincho.setTelefone(dados.telefone());

        if (dados.placa() != null && !dados.placa().isBlank()) {
            validarDuplicidadePlaca(dados.placa(), guincho.getPlaca());
            guincho.setPlaca(dados.placa());
        }

        return repositorioGuincho.save(guincho);
    }

    // --- Alteracao de senha (fluxo logado) ---

    @Transactional
    public void solicitarCodigoAlteracaoSenha(UUID id) {
        servicoAlteracaoSenha.solicitarCodigoAlteracao(buscarGuinchoOuLancarExcecao(id));
    }

    @Transactional
    public void alterarSenha(UUID id, AlteracaoSenhaDTO dados) {
        servicoAlteracaoSenha.alterarSenha(buscarGuinchoOuLancarExcecao(id), dados);
    }

    private Guincho buscarGuinchoOuLancarExcecao(UUID id) {
        return repositorioGuincho.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão inválida: Guincho não encontrado."));
    }

    private void validarDuplicidadePlaca(String novaPlaca, String placaAtual) {
        if (!novaPlaca.equals(placaAtual) && repositorioGuincho.existsByPlaca(novaPlaca)) {
            throw new PlacaJaCadastradaException("Esta placa já está registrada para outro guincho.");
        }
    }
}