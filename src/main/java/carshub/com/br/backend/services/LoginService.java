package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.CampoObrigatorioException;
import carshub.com.br.backend.exceptions.SenhaIncorretaException;
import carshub.com.br.backend.models.dtos.LoginDTO;
import carshub.com.br.backend.models.dtos.RedefinicaoSenhaDTO;
import carshub.com.br.backend.models.entities.PrestadorServico;
import carshub.com.br.backend.models.entities.Usuario;
import carshub.com.br.backend.models.enums.tiposUsuarios.TipoUsuario;
import carshub.com.br.backend.repositories.UsuarioRepository;
import carshub.com.br.backend.services.credenciais.AlteracaoSenhaService;
import carshub.com.br.backend.services.credenciais.CredencialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final UsuarioRepository repositorioUsuario;
    private final CredencialService servicoCredencial;
    private final AlteracaoSenhaService servicoAlteracaoSenha;

    public String autenticar(LoginDTO dadosLogin) {
        Usuario usuario = repositorioUsuario.findByLogin(dadosLogin.login())
                .orElseThrow(() -> new SenhaIncorretaException("Credenciais inválidas."));

        // Mensagem unica para usuario inexistente e senha errada: evita
        // que alguem descubra quais e-mails existem na plataforma.
        if (!servicoCredencial.verificar(dadosLogin.senha(), usuario.getSenha())) {
            throw new SenhaIncorretaException("Credenciais inválidas.");
        }

        if (!usuario.getTipoUsuario().equals(dadosLogin.tipoUsuarioSelecionado())) {
            throw new SenhaIncorretaException("Credenciais inválidas.");
        }

        if (dadosLogin.tipoUsuarioSelecionado() == TipoUsuario.PRESTADOR_SERVICO) {
            if (dadosLogin.tipoPrestador() == null) {
                throw new CampoObrigatorioException("Selecione o tipo de prestador de serviço (Mecânico ou Guincho).");
            }
            PrestadorServico prestador = (PrestadorServico) usuario;
            if (!prestador.getTipoPrestadorServico().equals(dadosLogin.tipoPrestador())) {
                throw new SenhaIncorretaException("Credenciais inválidas.");
            }
        }

        String subtipo = "";
        if (usuario instanceof PrestadorServico prestador) {
            subtipo = ", com a especificidade de " + prestador.getTipoPrestadorServico();
        }

        return "Login realizado com sucesso! Bem-vindo, " + usuario.getNome() +
                ". ID: " + usuario.getId() + ", tipo: " + usuario.getTipoUsuario() + subtipo;
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