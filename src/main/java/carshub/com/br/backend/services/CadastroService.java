package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.CampoObrigatorioException;
import carshub.com.br.backend.exceptions.DocumentoJaCadastradoException;
import carshub.com.br.backend.exceptions.EmailJaCadastradoException;
import carshub.com.br.backend.exceptions.PlacaJaCadastradaException;
import carshub.com.br.backend.models.dtos.CadastroDTO;
import carshub.com.br.backend.models.entities.*;
import carshub.com.br.backend.repositories.*;
import carshub.com.br.backend.services.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CadastroService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;
    private final EntusiastaRepository entusiastaRepository;
    private final OrganizadorEventoRepository organizadorEventoRepository;
    private final PrestadorServicoRepository prestadorServicoRepository; // Mantido com o erro de digitação original por enquanto
    private final GuinchoRepository guinchoRepository;
    private final PasswordEncoder codificadorSenha;
    private final EmailService emailService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return usuarioRepository.findByLogin(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com o email: " + email));
    }

    @Transactional
    public void registrarUsuario(CadastroDTO dto) {
        if (usuarioRepository.findByLogin(dto.login()).isPresent()) {
            throw new EmailJaCadastradoException("E-mail já cadastrado na plataforma.");
        }

        validaSenha(dto.senha());

        // 1. Instancia a entidade filha correta baseada no Enum do DTO
        Usuario novoUsuario = instanciarEMapearSubtipo(dto);

        // 2. Preenche os dados comuns da classe pai (Usuario)
        novoUsuario.setNome(dto.nome());
        novoUsuario.setLogin(dto.login());
        novoUsuario.setSenha(codificadorSenha.encode(dto.senha())); // CRIPTOGRAFIA GARANTIDA
        novoUsuario.setDataNascimento(dto.dataNascimento());
        novoUsuario.setTelefone(dto.telefone());
        novoUsuario.setTipoUsuario(dto.tipoUsuario());
        novoUsuario.setAtivo(false); // Conta nasce inativa até validar e-mail

        // (Nota: o dataHora já está sendo gerado pelo @CreationTimestamp da entidade)

        // 3. Gera e atribui o código de verificação
        String code = String.format("%06d", new Random().nextInt(999999));
        novoUsuario.setCodigoVerificacao(code);
        novoUsuario.setExpiracaoCodigoVerificacao(LocalDateTime.now().plusMinutes(5));

        // 4. Salva no banco
        usuarioRepository.save(novoUsuario);

        // 5. Envia o e-mail
        try {
            emailService.sendVerificationCode(novoUsuario.getLogin(), code);
        } catch (Exception e) {
            throw new RuntimeException("Falha ao enviar e-mail de ativação. Tente novamente mais tarde.");
        }
    }

    private Usuario instanciarEMapearSubtipo(CadastroDTO dto) {
        return switch (dto.tipoUsuario()) {
            case ENTUSIASTA, COLECIONADOR -> {
                if (dto.cpf() == null || dto.cpf().isBlank()) {
                    throw new CampoObrigatorioException("O CPF é obrigatório para o cadastro de Entusiasta/Colecionador.");
                }
                if (entusiastaRepository.existsByCpf(dto.cpf())) {
                    throw new DocumentoJaCadastradoException("Este CPF já está cadastrado.");
                }
                Entusiasta e = new Entusiasta();
                e.setCpf(dto.cpf());
                yield e;
            }

            case ORGANIZADOR_EVENTO -> {
                if (dto.cnpj() == null || dto.cnpj().isBlank()) {
                    throw new CampoObrigatorioException("O CNPJ é obrigatório para o cadastro de Organizador de Eventos.");
                }
                if (organizadorEventoRepository.existsByCnpj(dto.cnpj())) {
                    throw new DocumentoJaCadastradoException("Este CNPJ já está cadastrado.");
                }
                OrganizadorEvento o = new OrganizadorEvento();
                o.setCnpj(dto.cnpj());
                yield o;
            }

            case PRESTADOR_SERVICO -> {
                if (dto.cnpj() == null || dto.cnpj().isBlank()) {
                    throw new CampoObrigatorioException("O CNPJ é obrigatório para prestadores de serviço.");
                }
                if (prestadorServicoRepository.existsByCnpj(dto.cnpj())) {
                    throw new DocumentoJaCadastradoException("Este CNPJ já está cadastrado.");
                }
                if (dto.tipoPrestadorServico() == null) {
                    throw new CampoObrigatorioException("É necessário informar se o prestador é MECANICO ou GUINCHO.");
                }

                if (dto.tipoPrestadorServico().name().equals("GUINCHO")) {
                    if (dto.placa() == null || dto.placa().isBlank()) {
                        throw new CampoObrigatorioException("A placa é obrigatória para Guinchos.");
                    }
                    if (guinchoRepository.existsByPlaca(dto.placa())) {
                        throw new PlacaJaCadastradaException("Esta placa já está cadastrada para outro guincho.");
                    }
                    Guincho g = new Guincho();
                    g.setCnpj(dto.cnpj());
                    g.setTipoPrestadorServico(dto.tipoPrestadorServico());
                    g.setPlaca(dto.placa());
                    yield g;
                } else {
                    if (dto.servicosOfertados() == null || dto.servicosOfertados().isEmpty()) {
                        throw new CampoObrigatorioException("Mecânicos devem selecionar ao menos um serviço oferecido.");
                    }
                    Mecanico m = new Mecanico();
                    m.setCnpj(dto.cnpj());
                    m.setTipoPrestadorServico(dto.tipoPrestadorServico());
                    m.setServicos(dto.servicosOfertados());
                    yield m;
                }
            }
            default -> throw new CampoObrigatorioException("Tipo de usuário selecionado é inválido.");
        };
    }

    public void validaSenha(String senha) {
        String SENHA_REGEX = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";
        Pattern SENHA_PATTERN = Pattern.compile(SENHA_REGEX);
        if (senha == null || !SENHA_PATTERN.matcher(senha).matches()) {
            throw new IllegalArgumentException("A senha deve ter no mínimo 8 caracteres, uma letra maiúscula, uma minúscula, um número e um caractere especial");
        }
    }

    @Transactional
    public void verificarRegistro(String email, String codigo) {
        Usuario usuario = usuarioRepository.findByLogin(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado ou já verificado."));

        if (usuario.getCodigoVerificacao() == null || !usuario.getCodigoVerificacao().equals(codigo)) {
            throw new IllegalArgumentException("Código de verificação inválido.");
        }

        if (usuario.getExpiracaoCodigoVerificacao().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Código de verificação expirado. Por favor, faça um novo cadastro.");
        }

        usuario.setAtivo(true);
        usuario.setCodigoVerificacao(null);
        usuario.setExpiracaoCodigoVerificacao(null);
        usuarioRepository.save(usuario);
    }
}