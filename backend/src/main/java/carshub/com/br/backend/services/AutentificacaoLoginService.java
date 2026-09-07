package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.SenhaIncorretaException;
import carshub.com.br.backend.exceptions.UsuarioNaoEncontradoException;
import carshub.com.br.backend.models.dtos.LoginDTO;
import carshub.com.br.backend.models.entities.*;
import carshub.com.br.backend.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;

@Service
public class AutentificacaoLoginService {
    private final UsuarioRepository usuarioRepository;

    public AutentificacaoLoginService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public String autenticar(LoginDTO loginDTO) {
        Usuario usuario = usuarioRepository.findByLogin(loginDTO.login())
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Usuário não encontrado."));

        // OBS: Mais tarde, usaremos BCrypt para comparar senhas criptografadas.
        // Por enquanto, faremos uma comparação direta para testar o endpoint.
        if (!usuario.getSenha().equals(loginDTO.senha())) {
            throw new SenhaIncorretaException("Senha incorreta.");
        }
        return "Login realizado com sucesso! Bem-vindo, " + usuario.getNome() + ", seu registro é do tipo:" + usuario.getTipoUsuario();
    }
}
