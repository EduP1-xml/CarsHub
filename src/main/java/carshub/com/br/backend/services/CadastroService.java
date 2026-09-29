package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.CampoObrigatorioException;
import carshub.com.br.backend.exceptions.DocumentoJaCadastradoException;
import carshub.com.br.backend.exceptions.EmailJaCadastradoException;
import carshub.com.br.backend.exceptions.PlacaJaCadastradaException;
import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.exceptions.RegraDeNegocioException;
import carshub.com.br.backend.models.dtos.CadastroDTO;
import carshub.com.br.backend.models.dtos.CadastroIndividualDTO;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.Guincho;
import carshub.com.br.backend.models.entities.Mecanico;
import carshub.com.br.backend.models.entities.OrganizadorEvento;
import carshub.com.br.backend.models.entities.PrestadorServico;
import carshub.com.br.backend.models.entities.Usuario;
import carshub.com.br.backend.models.enums.tiposUsuarios.TipoPrestadorServico;
import carshub.com.br.backend.repositories.EntusiastaRepository;
import carshub.com.br.backend.repositories.GuinchoRepository;
import carshub.com.br.backend.repositories.OrganizadorEventoRepository;
import carshub.com.br.backend.repositories.PrestadorServicoRepository;
import carshub.com.br.backend.repositories.UsuarioRepository;
import carshub.com.br.backend.services.credenciais.CodigoVerificacaoService;
import carshub.com.br.backend.services.credenciais.CredencialService;
import carshub.com.br.backend.services.email.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CadastroService implements UserDetailsService {

    private final UsuarioRepository repositorioUsuario;
    private final EntusiastaRepository repositorioEntusiasta;
    private final OrganizadorEventoRepository repositorioOrganizadorEvento;
    private final PrestadorServicoRepository repositorioPrestadorServico;
    private final GuinchoRepository repositorioGuincho;
    private final CredencialService servicoCredencial;
    private final CodigoVerificacaoService servicoCodigoVerificacao;
    private final EmailService servicoEmail;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return repositorioUsuario.findByLogin(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado com o email: " + email));
    }

    // ==================================================================
    // ETAPA 1 - DADOS DE USUARIO
    // ==================================================================

    @Transactional
    public void registrarUsuario(CadastroDTO dados) {
        if (repositorioUsuario.findByLogin(dados.login()).isPresent()) {
            throw new EmailJaCadastradoException("E-mail já cadastrado na plataforma.");
        }

        servicoCredencial.validarPolitica(dados.senha());

        Usuario novoUsuario = instanciarSubtipo(dados);

        novoUsuario.setNome(dados.nome());
        novoUsuario.setLogin(dados.login());
        novoUsuario.setSenha(servicoCredencial.codificar(dados.senha()));
        novoUsuario.setDataNascimento(dados.dataNascimento());
        novoUsuario.setTelefone(dados.telefone());
        novoUsuario.setTipoUsuario(dados.tipoUsuario());
        novoUsuario.setAtivo(false); // Conta nasce inativa ate validar o e-mail

        String codigo = servicoCodigoVerificacao.gerarCodigo();
        novoUsuario.setCodigoVerificacao(codigo);
        novoUsuario.setExpiracaoCodigoVerificacao(servicoCodigoVerificacao.calcularExpiracao());

        repositorioUsuario.save(novoUsuario);

        try {
            servicoEmail.enviarCodigoVerificacao(novoUsuario.getLogin(), codigo);
        }
        catch (Exception e) {
            throw new RuntimeException("Falha ao enviar e-mail de ativação. Tente novamente mais tarde.");
        }
    }

    // ==================================================================
    // ETAPA 2 - DADOS ESPECIFICOS DA CLASSE CONCRETA
    // ==================================================================

    @Transactional
    public void registrarDadosIndividuais(CadastroIndividualDTO dados) {
        Usuario usuario = repositorioUsuario.findByLogin(dados.email())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado para o e-mail informado."));

        if (Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new RegraDeNegocioException("Esta conta já foi confirmada. Os dados individuais não podem mais ser alterados.");
        }

        switch (usuario.getTipoUsuario()) {
            case ENTUSIASTA, COLECIONADOR -> preencherDadosEntusiasta((Entusiasta) usuario, dados);
            case ORGANIZADOR_EVENTO -> preencherDadosOrganizador((OrganizadorEvento) usuario, dados);
            case PRESTADOR_SERVICO -> preencherDadosPrestador((PrestadorServico) usuario, dados);
            default -> throw new CampoObrigatorioException("Tipo de usuário inválido para preenchimento de dados individuais.");
        }
    }

    private void preencherDadosEntusiasta(Entusiasta entusiasta, CadastroIndividualDTO dados) {
        if (entusiasta.getCpf() != null && !entusiasta.getCpf().isBlank()) {
            throw new DocumentoJaCadastradoException("O CPF desta conta já foi informado.");
        }
        if (dados.cpf() == null || dados.cpf().isBlank()) {
            throw new CampoObrigatorioException("O CPF é obrigatório para o cadastro de Entusiasta/Colecionador.");
        }
        if (repositorioEntusiasta.existsByCpf(dados.cpf())) {
            throw new DocumentoJaCadastradoException("Este CPF já está cadastrado.");
        }

        entusiasta.setCpf(dados.cpf());
        repositorioUsuario.save(entusiasta);
    }

    private void preencherDadosOrganizador(OrganizadorEvento organizador, CadastroIndividualDTO dados) {
        if (organizador.getCnpj() != null && !organizador.getCnpj().isBlank()) {
            throw new DocumentoJaCadastradoException("O CNPJ desta conta já foi informado.");
        }
        if (dados.cnpj() == null || dados.cnpj().isBlank()) {
            throw new CampoObrigatorioException("O CNPJ é obrigatório para o cadastro de Organizador de Eventos.");
        }
        if (repositorioOrganizadorEvento.existsByCnpj(dados.cnpj())) {
            throw new DocumentoJaCadastradoException("Este CNPJ já está cadastrado.");
        }

        organizador.setCnpj(dados.cnpj());
        repositorioUsuario.save(organizador);
    }

    private void preencherDadosPrestador(PrestadorServico prestador, CadastroIndividualDTO dados) {
        if (prestador.getCnpj() != null && !prestador.getCnpj().isBlank()) {
            throw new DocumentoJaCadastradoException("O CNPJ desta conta já foi informado.");
        }
        if (dados.cnpj() == null || dados.cnpj().isBlank()) {
            throw new CampoObrigatorioException("O CNPJ é obrigatório para prestadores de serviço.");
        }
        if (repositorioPrestadorServico.existsByCnpj(dados.cnpj())) {
            throw new DocumentoJaCadastradoException("Este CNPJ já está cadastrado.");
        }
        prestador.setCnpj(dados.cnpj());

        if (prestador instanceof Guincho guincho) {
            if (dados.placa() == null || dados.placa().isBlank()) {
                throw new CampoObrigatorioException("A placa é obrigatória para Guinchos.");
            }
            if (repositorioGuincho.existsByPlaca(dados.placa())) {
                throw new PlacaJaCadastradaException("Esta placa já está cadastrada para outro guincho.");
            }
            guincho.setPlaca(dados.placa());
        }
        else if (prestador instanceof Mecanico mecanico) {
            if (dados.servicosOfertados() == null || dados.servicosOfertados().isEmpty()) {
                throw new CampoObrigatorioException("Mecânicos devem selecionar ao menos um serviço oferecido.");
            }
            mecanico.setServicos(dados.servicosOfertados());
        }
        else {
            throw new CampoObrigatorioException("Instância inválida para Prestador de Serviço. Esperado Guincho ou Mecanico.");
        }

        repositorioUsuario.save(prestador);
    }

    // ==================================================================
    // ETAPA 3 - VERIFICACAO DO E-MAIL
    // ==================================================================

    @Transactional
    public void verificarRegistro(String email, String codigo) {
        Usuario usuario = repositorioUsuario.findByLogin(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado para o e-mail informado."));

        // Caso 1: a conta existe mas o e-mail ja foi confirmado antes.
        if (Boolean.TRUE.equals(usuario.getAtivo())) {
            throw new RegraDeNegocioException("Esta conta já foi verificada anteriormente. Faça login para continuar.");
        }

        // Caso 2: a conta existe, ainda nao foi verificada, mas o codigo nao confere ou expirou.
        servicoCodigoVerificacao.validar(
                usuario.getCodigoVerificacao(),
                usuario.getExpiracaoCodigoVerificacao(),
                codigo);

        usuario.setAtivo(true);
        usuario.setCodigoVerificacao(null);
        usuario.setExpiracaoCodigoVerificacao(null);
        repositorioUsuario.save(usuario);
    }

    // ==================================================================
    // METODOS AUXILIARES
    // ==================================================================

    private Usuario instanciarSubtipo(CadastroDTO dados) {
        return switch (dados.tipoUsuario()) {
            case ENTUSIASTA, COLECIONADOR -> new Entusiasta();

            case ORGANIZADOR_EVENTO -> new OrganizadorEvento();

            case PRESTADOR_SERVICO -> {
                if (dados.tipoPrestadorServico() == null) {
                    throw new CampoObrigatorioException("É necessário informar se o prestador é MECANICO ou GUINCHO.");
                }
                if (dados.tipoPrestadorServico() == TipoPrestadorServico.GUINCHO) {
                    Guincho guincho = new Guincho();
                    guincho.setTipoPrestadorServico(dados.tipoPrestadorServico());
                    yield guincho;
                }
                Mecanico mecanico = new Mecanico();
                mecanico.setTipoPrestadorServico(dados.tipoPrestadorServico());
                yield mecanico;
            }

            default -> throw new CampoObrigatorioException("Tipo de usuário selecionado é inválido.");
        };
    }
}