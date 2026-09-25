package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.CampoObrigatorioException;
import carshub.com.br.backend.exceptions.SenhaIncorretaException;
import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.models.dtos.LoginDTO;
import carshub.com.br.backend.models.entities.PrestadorServico;
import carshub.com.br.backend.models.entities.Usuario;
import carshub.com.br.backend.models.enums.tiposUsuarios.TipoUsuario;
import carshub.com.br.backend.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final UsuarioRepository repositorioUsuario;
    private final PasswordEncoder codificadorSenha;

    public String autenticar(LoginDTO dadosLogin) {
        Usuario usuario = repositorioUsuario.findByLogin(dadosLogin.login())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        if (!codificadorSenha.matches(dadosLogin.senha(), usuario.getSenha())) {
            throw new SenhaIncorretaException("Senha incorreta.");
        }

        if (!usuario.getTipoUsuario().equals(dadosLogin.tipoUsuarioSelecionado())) {
            throw new SenhaIncorretaException("Login ou senha inválidos.");
        }

        if (dadosLogin.tipoUsuarioSelecionado() == TipoUsuario.PRESTADOR_SERVICO) {

            if (dadosLogin.tipoPrestador() == null) {
                throw new CampoObrigatorioException("Selecione o tipo de prestador de serviço (Mecânico ou Guincho).");
            }

            PrestadorServico prestador = (PrestadorServico) usuario;
            if (!prestador.getTipoPrestadorServico().equals(dadosLogin.tipoPrestador())) {
                throw new SenhaIncorretaException("Login ou senha inválidos.");
            }
        }

        String subtipo = "";
        if (usuario instanceof PrestadorServico prestador) {
            subtipo = ", com a especificidade de " + prestador.getTipoPrestadorServico();
        }

        return "Login realizado com sucesso! Bem-vindo, " + usuario.getNome() +
                ". Registrado com sucesso! ID: " + usuario.getId() + ", seu registro é do tipo: " + usuario.getTipoUsuario() + subtipo;
    }
}