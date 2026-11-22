package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.CampoObrigatorioException;
import carshub.com.br.backend.exceptions.SenhaIncorretaException;
import carshub.com.br.backend.exceptions.TokenJwtInvalidoException;
import carshub.com.br.backend.models.dtos.LoginDTO;
import carshub.com.br.backend.models.dtos.LoginResponseDTO;
import carshub.com.br.backend.models.dtos.RedefinicaoSenhaDTO;
import carshub.com.br.backend.models.entities.PrestadorServico;
import carshub.com.br.backend.models.entities.Usuario;
import carshub.com.br.backend.models.enums.tiposUsuarios.TipoPrestadorServico;
import carshub.com.br.backend.models.enums.tiposUsuarios.TipoUsuario;
import carshub.com.br.backend.repositories.UsuarioRepository;
import carshub.com.br.backend.services.credenciais.AlteracaoSenhaService;
import carshub.com.br.backend.services.credenciais.CredencialService;
import carshub.com.br.backend.services.seguranca.ServicoAuditoria;
import carshub.com.br.backend.services.seguranca.ServicoJwt;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * SPRINT 6 (11.1): o login deixou de devolver mensagem em texto e passou a
 * emitir o PAR de tokens JWT (access + refresh). A identidade nao e mais o
 * header "user-id" (removido no 11.4): o access viaja em Authorization: Bearer
 * e o refresh renova o par em POST /autenticacao/refresh.
 *
 * As checagens de credencial e a mensagem unica para e-mail inexistente/senha
 * errada foram mantidas; cada desfecho vira registro na trilha de auditoria
 * (11.9 — sem senha/token nos detalhes).
 */
@Service
@RequiredArgsConstructor
public class LoginService {

    private final UsuarioRepository repositorioUsuario;
    private final CredencialService servicoCredencial;
    private final AlteracaoSenhaService servicoAlteracaoSenha;
    private final ServicoJwt servicoJwt;
    private final ServicoAuditoria servicoAuditoria;

    public LoginResponseDTO autenticar(LoginDTO dadosLogin) {
        Usuario usuario = repositorioUsuario.findByLogin(dadosLogin.login())
                .orElseThrow(() -> {
                    servicoAuditoria.registrar("LOGIN_FALHA", dadosLogin.login(),
                            "credenciais inválidas (e-mail inexistente ou senha incorreta)",
                            null, false);
                    return new SenhaIncorretaException("Credenciais inválidas.");
                });

        // Mensagem unica para usuario inexistente e senha errada: evita
        // que alguem descubra quais e-mails existem na plataforma.
        if (!servicoCredencial.verificar(dadosLogin.senha(), usuario.getSenha())) {
            servicoAuditoria.registrar("LOGIN_FALHA", usuario.getLogin(),
                    "senha incorreta", usuario.getId(), false);
            throw new SenhaIncorretaException("Credenciais inválidas.");
        }

        // COLECIONADOR NAO e perfil de login: quem ja tem garagem entra como
        // ENTUSIASTA (mesmo mapeamento de authority em Usuario.getAuthorities()).
        // O tipo REAL continua vindo na resposta para o front montar a sessao.
        TipoUsuario tipoEfetivo = usuario.getTipoUsuario() == TipoUsuario.COLECIONADOR
                ? TipoUsuario.ENTUSIASTA
                : usuario.getTipoUsuario();
        if (!tipoEfetivo.equals(dadosLogin.tipoUsuarioSelecionado())
                && !usuario.getTipoUsuario().equals(dadosLogin.tipoUsuarioSelecionado())) {
            servicoAuditoria.registrar("LOGIN_FALHA", usuario.getLogin(),
                    "tipo de usuário divergente", usuario.getId(), false);
            throw new SenhaIncorretaException("Credenciais inválidas.");
        }

        if (dadosLogin.tipoUsuarioSelecionado() == TipoUsuario.PRESTADOR_SERVICO) {
            if (dadosLogin.tipoPrestador() == null) {
                throw new CampoObrigatorioException("Selecione o tipo de prestador de serviço (Mecânico ou Guincho).");
            }
            PrestadorServico prestador = (PrestadorServico) usuario;
            if (!prestador.getTipoPrestadorServico().equals(dadosLogin.tipoPrestador())) {
                servicoAuditoria.registrar("LOGIN_FALHA", usuario.getLogin(),
                        "tipo de prestador divergente", usuario.getId(), false);
                throw new SenhaIncorretaException("Credenciais inválidas.");
            }
        }

        // AUTORIZACAO (07/11/2026): conta sem e-mail verificado nao entra.
        // A checagem vem DEPOIS das credenciais para nao revelar se a conta existe.
        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            servicoAuditoria.registrar("LOGIN_NEGADO", usuario.getLogin(),
                    "conta não verificada", usuario.getId(), false);
            throw new SenhaIncorretaException(
                    "Conta não verificada. Confirme o código enviado para o seu e-mail antes de fazer login.");
        }

        servicoAuditoria.registrar("LOGIN_SUCESSO", usuario.getLogin(),
                "tipo=" + usuario.getTipoUsuario(), usuario.getId(), true);
        return construirResposta(usuario);
    }

    /**
     * SPRINT 6 (11.1): renova o par de tokens a partir do refresh. O refresh
     * nao abre rotas (tipo proprio); a validacao de usuario existe AQUI (uma
     * consulta por renovacao, nao por requisicao): conta desativada para de
     * renovar e o access restante expira em minutos.
     */
    public LoginResponseDTO renovarTokens(String refreshToken) {
        ServicoJwt.TokenAutenticado dados = servicoJwt.validar(refreshToken, ServicoJwt.TIPO_REFRESH);

        Usuario usuario = repositorioUsuario.findById(dados.idUsuario())
                .orElseThrow(() -> new TokenJwtInvalidoException(
                        "Token inválido ou expirado. Faça login novamente."));

        if (!Boolean.TRUE.equals(usuario.getAtivo())) {
            servicoAuditoria.registrar("RENOVACAO_NEGADA", usuario.getLogin(),
                    "conta não verificada", usuario.getId(), false);
            throw new TokenJwtInvalidoException(
                    "Conta não verificada. Confirme o código enviado para o seu e-mail antes de fazer login.");
        }

        servicoAuditoria.registrar("TOKEN_RENOVADO", usuario.getLogin(), null, usuario.getId(), true);
        return construirResposta(usuario);
    }

    private LoginResponseDTO construirResposta(Usuario usuario) {
        TipoPrestadorServico tipoPrestador = usuario instanceof PrestadorServico prestador
                ? prestador.getTipoPrestadorServico()
                : null;

        return new LoginResponseDTO(
                servicoJwt.gerarTokenAcesso(usuario),
                servicoJwt.gerarTokenRefresh(usuario),
                "Bearer",
                servicoJwt.validadeAcessoEmSegundos(),
                usuario.getId(),
                usuario.getNome(),
                usuario.getTipoUsuario(),
                tipoPrestador);
    }

    // ==================================================================
    // ESQUECI MINHA SENHA (fluxo deslogado)
    // A alteracao de senha estando logado acontece no perfil de cada usuario.
    // ==================================================================

    public void solicitarCodigoRedefinicaoSenha(String email) {
        servicoAlteracaoSenha.solicitarCodigoRedefinicao(email);
    }

    public void redefinirSenha(RedefinicaoSenhaDTO dados) {
        servicoAlteracaoSenha.redefinirSenha(dados);
    }
}
