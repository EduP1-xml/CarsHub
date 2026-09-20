package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.CampoObrigatorioException;
import carshub.com.br.backend.exceptions.SenhaIncorretaException;
import carshub.com.br.backend.exceptions.UsuarioNaoEncontradoException;
import carshub.com.br.backend.models.dtos.LoginDTO;
import carshub.com.br.backend.models.entities.*;
import carshub.com.br.backend.models.enums.tiposUsuarios.TipoUsuario;
import carshub.com.br.backend.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LoginService {
    private final UsuarioRepository usuarioRepository;

    public String autenticar(LoginDTO loginDTO) {
        Usuario usuario = usuarioRepository.findByLogin(loginDTO.login())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado."));

        // OBS: Mais tarde, usaremos BCrypt para comparar senhas criptografadas.
        // Por enquanto, faremos uma comparação direta para testar o endpoint.
        if (!usuario.getSenha().equals(loginDTO.senha())) {
            throw new SenhaIncorretaException("Senha incorreta.");
        }

        // 3. Validação do Tipo de Usuário Principal
        if (!usuario.getTipoUsuario().equals(loginDTO.tipoUsuarioSelecionado())) {
            throw new SenhaIncorretaException("Login ou senha inválidos."); // Mantém a mensagem genérica
        }

        // 4. Validação Específica para Prestadores de Serviço
        if (loginDTO.tipoUsuarioSelecionado() == TipoUsuario.PRESTADOR_SERVICO) {

            // O front-end precisa enviar se é Mecânico ou Guincho
            if (loginDTO.tipoUsuarioSelecionado() == null) {
                throw new CampoObrigatorioException("Selecione o tipo de prestador de serviço (Mecânico ou Guincho).");
            }

            // Faz o cast seguro e verifica se o subtipo no banco é igual ao que ele selecionou na tela
            PrestadorServico prestador = (PrestadorServico) usuario;
            if (!prestador.getTipoPrestadorServico().equals(loginDTO.tipoUsuarioSelecionado())) {
                throw new SenhaIncorretaException("Login ou senha inválidos.");
            }
        }

        // 5. Retorno de Sucesso Formatado
        String subtipo = "";
        if (usuario instanceof PrestadorServico prestador) {
            subtipo = " (" + prestador.getTipoPrestadorServico() + ")";
        }
        return "Login realizado com sucesso! Bem-vindo, " + usuario.getNome() +
                " registrado com sucesso! ID: " + usuario.getId() + ", seu registro é do tipo:" + usuario.getTipoUsuario();
    }
}
