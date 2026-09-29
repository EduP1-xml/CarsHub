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

/**
 * Centraliza os dois fluxos de troca de senha:
 *
 *   LOGADO    - solicitado pelo perfil do usuario; a identidade vem da sessao.
 *   DESLOGADO - "esqueci minha senha"; a identidade vem do e-mail informado.
 *
 * Em ambos os casos e obrigatorio confirmar um codigo enviado por e-mail antes
 * de a nova senha ser aceita.
 */
@Service
@RequiredArgsConstructor
public class AlteracaoSenhaService {

    private final UsuarioRepository repositorioUsuario;
    private final CredencialService servicoCredencial;
    private final CodigoVerificacaoService servicoCodigoVerificacao;
    private final EmailService servicoEmail;

    // ==================================================================
    // FLUXO LOGADO
    // ==================================================================

    @Transactional
    public void solicitarCodigoAlteracao(Usuario usuario) {
        enviarCodigo(usuario);
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

    /**
     * A resposta e sempre a mesma, exista ou nao o e-mail, para nao permitir
     * que alguem descubra quais contas existem na plataforma.
     */
    @Transactional
    public void solicitarCodigoRedefinicao(String email) {
        repositorioUsuario.findByLogin(email).ifPresent(this::enviarCodigo);
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
    // METODOS AUXILIARES
    // ==================================================================

    private void enviarCodigo(Usuario usuario) {
        String codigo = servicoCodigoVerificacao.gerarCodigo();
        usuario.setCodigoRedefinicaoSenha(codigo);
        usuario.setExpiracaoCodigoRedefinicaoSenha(servicoCodigoVerificacao.calcularExpiracao());
        repositorioUsuario.save(usuario);

        try {
            servicoEmail.enviarCodigoRedefinicaoSenha(usuario.getLogin(), codigo);
        }
        catch (Exception e) {
            throw new RuntimeException("Erro ao enviar e-mail de recuperação.", e);
        }
    }

    private void aplicarNovaSenha(Usuario usuario, String novaSenha) {
        servicoCredencial.atualizarSenha(usuario, novaSenha);

        // O codigo e consumido: nao pode ser reutilizado.
        usuario.setCodigoRedefinicaoSenha(null);
        usuario.setExpiracaoCodigoRedefinicaoSenha(null);

        repositorioUsuario.save(usuario);
    }
}