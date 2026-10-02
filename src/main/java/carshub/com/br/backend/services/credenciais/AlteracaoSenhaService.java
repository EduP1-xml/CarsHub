package carshub.com.br.backend.services.credenciais;

import carshub.com.br.backend.exceptions.RegraDeNegocioException;
import carshub.com.br.backend.models.dtos.AlteracaoSenhaDTO;
import carshub.com.br.backend.models.dtos.RedefinicaoSenhaDTO;
import carshub.com.br.backend.models.entities.Usuario;
import carshub.com.br.backend.repositories.UsuarioRepository;
import carshub.com.br.backend.services.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class AlteracaoSenhaService {

    private final UsuarioRepository repositorioUsuario;
    private final CredencialService servicoCredencial;
    private final CodigoVerificacaoService servicoCodigoVerificacao;
    private final EmailService servicoEmail;
    private final TransactionTemplate templateTransacao;

    // ==================================================================
    // FLUXO LOGADO
    // ==================================================================

    public void solicitarCodigoAlteracao(Usuario usuario) {
        String codigo = templateTransacao.execute(
                status -> gerarCodigoRedefinicao(usuario.getId()));

        if (codigo == null) {
            throw new RegraDeNegocioException("Não foi possível gerar um novo código. Tente novamente.");
        }

        // Fora da transacao. Falha propaga como 503: o objetivo e entregar o codigo.
        servicoEmail.enviarCodigoRedefinicaoSenha(usuario.getLogin(), codigo);
    }

    @Transactional
    public void alterarSenha(Usuario usuario, AlteracaoSenhaDTO dados) {
        servicoCredencial.validarPolitica(dados.novaSenha());
        servicoCodigoVerificacao.validar(
                usuario.getCodigoRedefinicaoSenha(),
                usuario.getExpiracaoCodigoRedefinicaoSenha(),
                dados.codigo());

        aplicarNovaSenha(usuario, dados.novaSenha());
    }

    // ==================================================================
    // FLUXO DESLOGADO
    // ==================================================================
    public void solicitarCodigoRedefinicao(String email) {
        String codigo = templateTransacao.execute(status ->
                repositorioUsuario.findByLogin(email)
                        .map(usuario -> gerarCodigoRedefinicao(usuario.getId()))
                        .orElse(null));

        // E-mail inexistente: encerra silenciosamente, sem enviar nada.
        if (codigo == null) {
            return;
        }

        servicoEmail.enviarCodigoRedefinicaoSenha(email, codigo);
    }

    @Transactional
    public void redefinirSenha(RedefinicaoSenhaDTO dados) {
        Usuario usuario = repositorioUsuario.findByLogin(dados.email())
                .orElseThrow(() -> new RegraDeNegocioException("Código de verificação inválido."));

        servicoCredencial.validarPolitica(dados.novaSenha());
        servicoCodigoVerificacao.validar(
                usuario.getCodigoRedefinicaoSenha(),
                usuario.getExpiracaoCodigoRedefinicaoSenha(),
                dados.codigo());

        aplicarNovaSenha(usuario, dados.novaSenha());
    }
    // ==================================================================
    // METODOS AUXILIARES (executados dentro da transacao)
    // ==================================================================

    private String gerarCodigoRedefinicao(java.util.UUID idUsuario) {
        Usuario usuario = repositorioUsuario.findById(idUsuario)
                .orElseThrow(() -> new RegraDeNegocioException("Usuário não encontrado."));

        // Codigo novo substitui o anterior e reinicia o prazo de 10 minutos.
        String codigo = servicoCodigoVerificacao.gerarCodigo();
        usuario.setCodigoRedefinicaoSenha(codigo);
        usuario.setExpiracaoCodigoRedefinicaoSenha(servicoCodigoVerificacao.calcularExpiracao());
        repositorioUsuario.save(usuario);

        return codigo;
    }

    private void aplicarNovaSenha(Usuario usuario, String novaSenha) {
        servicoCredencial.atualizarSenha(usuario, novaSenha);

        // O codigo e consumido: nao pode ser reutilizado.
        usuario.setCodigoRedefinicaoSenha(null);
        usuario.setExpiracaoCodigoRedefinicaoSenha(null);

        repositorioUsuario.save(usuario);
    }
}