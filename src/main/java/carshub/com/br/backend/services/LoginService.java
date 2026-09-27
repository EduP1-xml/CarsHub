package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.CampoObrigatorioException;
import carshub.com.br.backend.exceptions.SenhaIncorretaException;
import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.models.dtos.LoginDTO;
import carshub.com.br.backend.models.entities.PrestadorServico;
import carshub.com.br.backend.models.entities.Usuario;
import carshub.com.br.backend.models.enums.tiposUsuarios.TipoUsuario;
import carshub.com.br.backend.repositories.UsuarioRepository;
import carshub.com.br.backend.services.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class LoginService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder codificadorSenha;
    private final EmailService emailService;

    public String autenticar(LoginDTO dadosLogin) {
        Usuario usuario = usuarioRepository.findByLogin(dadosLogin.login()).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

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


    @Transactional
    public void esqueciSenha(String email) {
        Usuario user = usuarioRepository.findByLogin(email).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        if (user == null || !user.isEnabled()) {
            throw new IllegalArgumentException("Usuário não encontrado, verifique se o email ja foi cadastrado!");
        }
        String code = String.format("%06d", new SecureRandom().nextInt(999999));
        user.setCodigoVerificacao(code);
        user.setExpiracaoCodigoVerificacao(LocalDateTime.now().plusMinutes(10)); // Código de reset expira em 10 minutos
        usuarioRepository.save(user);
        try {
            emailService.sendPasswordResetCode(email, code);
        }
        catch (Exception e) {
            throw new RuntimeException("Erro ao enviar email de recuperação.", e);
        }
    }

    @Transactional
    public void resetarSenha(String email, String code, String newPassword) {
        Usuario user = usuarioRepository.findByLogin(email).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        if (user == null) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }

        if (user.getCodigoVerificacao() == null || !user.getCodigoVerificacao().equals(code)) {
            throw new IllegalArgumentException("Código de verificação inválido.");
        }

        if (user.getExpiracaoCodigoVerificacao().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Código de verificação expirado. Por favor, solicite um novo.");
        }

        if (newPassword == null || newPassword.isBlank()) {
            throw new IllegalArgumentException("A nova senha não pode estar em branco.");
        }
        validaSenha(newPassword);

        user.setSenha(codificadorSenha.encode(newPassword));
        user.setCodigoVerificacao(null); //Sem codigo de verificação para trocar senha por enquanto
        user.setExpiracaoCodigoVerificacao(null);
        usuarioRepository.save(user);
    }

    public void validaSenha(String senha) {
        String SENHA_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        Pattern SENHA_PATTERN = Pattern.compile(SENHA_REGEX);
        if (senha == null || !SENHA_PATTERN.matcher(senha).matches()) {
            throw new IllegalArgumentException("A senha deve ter no mínimo 8 caracteres, uma letra maiúscula, uma minúscula, um número e um caractere especial");
        }
    }

}