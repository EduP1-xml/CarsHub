package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.CampoObrigatorioCadastroException;
import carshub.com.br.backend.exceptions.DocumentoJaCadastradoException;
import carshub.com.br.backend.exceptions.EmailJaCadastradoException;
import carshub.com.br.backend.exceptions.PlacaJaCadastradaException;
import carshub.com.br.backend.models.dtos.CadastroDTO;
import carshub.com.br.backend.models.entities.*;
import carshub.com.br.backend.repositories.*;
import org.springframework.stereotype.Service;

@Service
public class CadastroService {
    private final UsuarioRepository usuarioRepository;
    private final EntusiastaRepository entusiastaRepository;
    private final OrganizadorEventoRepository organizadorEventoRepository;
    private final PrestadorServicoRespository prestadorServicoRespository;
    private final GuinchoRepository guinchoRepository;

    public CadastroService(UsuarioRepository usuarioRepository, EntusiastaRepository entusiastaRepository, OrganizadorEventoRepository organizadorEventoRepository, PrestadorServicoRespository prestadorServicoRespository, GuinchoRepository guinchoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.entusiastaRepository = entusiastaRepository;
        this.organizadorEventoRepository = organizadorEventoRepository;
        this.prestadorServicoRespository = prestadorServicoRespository;
        this.guinchoRepository = guinchoRepository;
    }

    public Usuario registrarUsuario(CadastroDTO cadastroDTO) {
        if (usuarioRepository.findByLogin(cadastroDTO.login()).isPresent()) {
            throw new EmailJaCadastradoException("E-mail já cadastrado na plataforma.");
        }

        if (cadastroDTO.telefone() == null || cadastroDTO.telefone().isBlank()) {
            throw new CampoObrigatorioCadastroException("O telefone é obrigatório para o cadastro.");
        }

        Usuario novoUsuario = instanciarUsuarioPorTipo(cadastroDTO);

        novoUsuario.setNome(cadastroDTO.nome());
        novoUsuario.setLogin(cadastroDTO.login());
        novoUsuario.setSenha(cadastroDTO.senha());
        novoUsuario.setDataNascimento(cadastroDTO.dataNascimento());
        novoUsuario.setTipoUsuario(cadastroDTO.tipoUsuario());
        novoUsuario.setTelefone(cadastroDTO.telefone());

        return usuarioRepository.save(novoUsuario);
    }

    private Usuario instanciarUsuarioPorTipo(CadastroDTO dto) {
        return switch (dto.tipoUsuario()) {
            case ENTUSIASTA -> {
                if (dto.cpf() == null || dto.cpf().isBlank()) {
                    throw new CampoObrigatorioCadastroException("O CPF é obrigatório para o cadastro de Entusiasta/Colecionador.");
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
                    throw new CampoObrigatorioCadastroException("O CNPJ é obrigatório para o cadastro de Organizador de Eventos.");
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
                    throw new CampoObrigatorioCadastroException("O CNPJ é obrigatório para prestadores de serviço.");
                }
                if (prestadorServicoRespository.existsByCnpj(dto.cnpj())) {
                    throw new DocumentoJaCadastradoException("Este CNPJ já está cadastrado.");
                }
                if (dto.tipoPrestadorServico() == null) {
                    throw new CampoObrigatorioCadastroException("É necessário informar se o prestador é MECANICO ou GUINCHO.");
                }

                if (dto.tipoPrestadorServico().name().equals("GUINCHO")) {
                    if (dto.placa() == null || dto.placa().isBlank()) {
                        throw new CampoObrigatorioCadastroException("A placa é obrigatória para Guinchos.");
                    }
                    if (guinchoRepository.existsByPlaca(dto.placa())) {
                        throw new PlacaJaCadastradaException("Esta placa já está cadastrada para outro guincho.");
                    }
                    Guincho g = new Guincho();
                    g.setCnpj(dto.cnpj());
                    g.setTipoPrestadorServico(dto.tipoPrestadorServico());
                    g.setPlaca(dto.placa());
                    yield g;
                }
                else {
                    if (dto.servicosOfertados() == null || dto.servicosOfertados().isEmpty()) {
                        throw new CampoObrigatorioCadastroException("Mecânicos devem selecionar ao menos um serviço oferecido.");
                    }
                    Mecanico m = new Mecanico();
                    m.setCnpj(dto.cnpj());
                    m.setTipoPrestadorServico(dto.tipoPrestadorServico());
                    m.setServicos(dto.servicosOfertados());
                    yield m;
                }
            }
            default -> throw new CampoObrigatorioCadastroException("Tipo de usuário selecionado é inválido.");
        };
    }
}