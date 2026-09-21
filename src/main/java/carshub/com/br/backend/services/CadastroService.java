package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.CampoObrigatorioException;
import carshub.com.br.backend.exceptions.DocumentoJaCadastradoException;
import carshub.com.br.backend.exceptions.EmailJaCadastradoException;
import carshub.com.br.backend.exceptions.PlacaJaCadastradaException;
import carshub.com.br.backend.models.entities.*;
import carshub.com.br.backend.repositories.*;
import carshub.com.br.backend.services.email.EmailSenha;
import carshub.com.br.backend.services.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CadastroService implements UserDetailsService {
    private final UsuarioRepository usuarioRepository;
    private final EntusiastaRepository entusiastaRepository;
    private final OrganizadorEventoRepository organizadorEventoRepository;
    private final PrestadorServicoRespository prestadorServicoRespository;
    private final GuinchoRepository guinchoRepository;
    private final PasswordEncoder encoder;
    private final EmailSenha emailSenha;
    private final EmailService emailService;
    @Lazy
    @Autowired
    private AuthenticationManager authenticationManager;



    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario user = (Usuario) usuarioRepository.findByEmail(email);
        if (user == null) {
            throw new UsernameNotFoundException("Usuário não encontrado com o email: " + email);
        }
        return user;
    }

    public void registrarUsuario(Usuario novoUsuario) {
        Optional<Usuario> existUser = usuarioRepository.findByLogin(novoUsuario.getLogin());

        if (existUser.isPresent()) {
            throw new EmailJaCadastradoException("E-mail já cadastrado na plataforma.");
        }

        if (novoUsuario.getSenha() == null) {
            throw new IllegalArgumentException("Senha Está em Branco.");
        }

        if (novoUsuario.getTelefone() == null || novoUsuario.getTelefone().isBlank()) {
            throw new CampoObrigatorioException("O telefone é obrigatório para o cadastro.");
        }

        if (novoUsuario.getVerificationCodeExpiration() != null && novoUsuario.getVerificationCodeExpiration().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Aguarde o tempo de 5 minutos para expiração do código anterior para solicitar um novo.");
        }

        validarSubtipoUsuario(novoUsuario);
        validaSenha(novoUsuario.getSenha());

        String senhaEnconde = encoder.encode(novoUsuario.getSenha());

        novoUsuario.setSenha(senhaEnconde);
        novoUsuario.setDataHora(LocalDateTime.now());

        String code = String.format("%06d", new Random().nextInt(999999));
        novoUsuario.setVerificationCode(code);
        novoUsuario.setVerificationCodeExpiration(LocalDateTime.now().plusMinutes(5));

        usuarioRepository.save(novoUsuario);

        try {
            emailService.sendVerificationCode(novoUsuario.getLogin(), code);
        }
        catch (Exception e) {
            throw new RuntimeException("Falha ao enviar e-mail. Tente novamente mais tarde.");
        }
    }

    public void validaSenha(String senha) {
        String SENHA_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        Pattern SENHA_PATTERN = Pattern.compile(SENHA_REGEX);
        if (senha == null || !SENHA_PATTERN.matcher(senha).matches()) {
            throw new IllegalArgumentException("A senha deve ter no mínimo 8 caracteres, uma letra maiúscula, uma minúscula, um número e um caractere especial");
        }
    }

    @Transactional
    public void verifyRegister(String email, String code) {
        Usuario existUser = (Usuario) usuarioRepository.findByEmail(email);

        if (existUser == null) {
            throw new IllegalArgumentException("Usuário não encontrado ou já verificado.");
        }

        if (existUser.getVerificationCode() == null || !existUser.getVerificationCode().equals(code)) {
            throw new IllegalArgumentException("Código de verificação inválido.");
        }

        if (existUser.getVerificationCodeExpiration().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Código de verificação expirado. Por favor, solicite um novo.");
        }

        existUser.setEnabled(true);
        existUser.setVerificationCode(null);
        existUser.setVerificationCodeExpiration(null);
        usuarioRepository.save(existUser);
    }

    private void validarSubtipoUsuario(Usuario usuario) {
        switch (usuario.getTipoUsuario()) {
            case ENTUSIASTA -> {
                if (usuario instanceof Entusiasta e) {
                    if (e.getCpf() == null || e.getCpf().isBlank()) {
                        throw new CampoObrigatorioException("O CPF é obrigatório para o cadastro de Entusiasta/Colecionador.");
                    }
                    if (entusiastaRepository.existsByCpf(e.getCpf())) {
                        throw new DocumentoJaCadastradoException("Este CPF já está cadastrado.");
                    }
                } else {
                    throw new IllegalArgumentException("Instância inválida para Entusiasta.");
                }
            }
            case ORGANIZADOR_EVENTO -> {
                if (usuario instanceof OrganizadorEvento o) {
                    if (o.getCnpj() == null || o.getCnpj().isBlank()) {
                        throw new CampoObrigatorioException("O CNPJ é obrigatório para o cadastro de Organizador de Eventos.");
                    }
                    if (organizadorEventoRepository.existsByCnpj(o.getCnpj())) {
                        throw new DocumentoJaCadastradoException("Este CNPJ já está cadastrado.");
                    }
                } else {
                    throw new IllegalArgumentException("Instância inválida para Organizador de Eventos.");
                }
            }
            case PRESTADOR_SERVICO -> {
                if (usuario instanceof Guincho g) {
                    if (g.getCnpj() == null || g.getCnpj().isBlank()) {
                        throw new CampoObrigatorioException("O CNPJ é obrigatório para prestadores de serviço.");
                    }
                    if (prestadorServicoRespository.existsByCnpj(g.getCnpj())) {
                        throw new DocumentoJaCadastradoException("Este CNPJ já está cadastrado.");
                    }
                    if (g.getTipoPrestadorServico() == null) {
                        throw new CampoObrigatorioException("É necessário informar se o prestador é MECANICO ou GUINCHO.");
                    }
                    if (g.getPlaca() == null || g.getPlaca().isBlank()) {
                        throw new CampoObrigatorioException("A placa é obrigatória para Guinchos.");
                    }
                    if (guinchoRepository.existsByPlaca(g.getPlaca())) {
                        throw new PlacaJaCadastradaException("Esta placa já está cadastrada para outro guincho.");
                    }
                } else if (usuario instanceof Mecanico m) {
                    if (m.getCnpj() == null || m.getCnpj().isBlank()) {
                        throw new CampoObrigatorioException("O CNPJ é obrigatório para prestadores de serviço.");
                    }
                    if (prestadorServicoRespository.existsByCnpj(m.getCnpj())) {
                        throw new DocumentoJaCadastradoException("Este CNPJ já está cadastrado.");
                    }
                    if (m.getTipoPrestadorServico() == null) {
                        throw new CampoObrigatorioException("É necessário informar se o prestador é MECANICO ou GUINCHO.");
                    }
                    if (m.getServicos() == null || m.getServicos().isEmpty()) {
                        throw new CampoObrigatorioException("Mecânicos devem selecionar ao menos um serviço oferecido.");
                    }
                } else {
                    throw new IllegalArgumentException("Instância inválida para Prestador de Serviço. Esperado Guincho ou Mecanico.");
                }
            }
            default -> throw new CampoObrigatorioException("Tipo de usuário selecionado é inválido.");
        }
    }
}