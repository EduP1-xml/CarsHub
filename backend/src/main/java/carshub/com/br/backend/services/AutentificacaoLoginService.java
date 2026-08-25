package carshub.com.br.backend.services;

import carshub.com.br.backend.models.dtos.RequisicaoLoginDTO;
import carshub.com.br.backend.models.entities.Usuario;
import carshub.com.br.backend.repositories.EntusiastaRepository;
import carshub.com.br.backend.repositories.OrganizadorEventoRepository;
import carshub.com.br.backend.repositories.PrestadorServicoRepository;
import carshub.com.br.backend.repositories.UsuarioRepository;
import org.springframework.stereotype.Service;

@Service
public class AutentificacaoLoginService {
    private final UsuarioRepository usuarioRepository;
    private final EntusiastaRepository entusiastaRepository;
    private final OrganizadorEventoRepository organizadorEventoRepository;
    private final PrestadorServicoRepository prestadorServicoRepository;

    public AutentificacaoLoginService(UsuarioRepository usuarioRepository, EntusiastaRepository entusiastaRepository, OrganizadorEventoRepository organizadorEventoRepository, PrestadorServicoRepository prestadorServicoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.entusiastaRepository = entusiastaRepository;
        this.organizadorEventoRepository = organizadorEventoRepository;
        this.prestadorServicoRepository = prestadorServicoRepository;
    }

    public String autenticar(RequisicaoLoginDTO dtoLogin) {
        Usuario usuario = usuarioRepository.findByLogin(dtoLogin.login())
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado."));

        // OBS: Mais tarde, usaremos BCrypt para comparar senhas criptografadas.
        // Por enquanto, faremos uma comparação direta para testar o endpoint.
        if (!usuario.getSenha().equals(dtoLogin.senha())) {
            throw new RuntimeException("Senha incorreta.");
        }

        // Validação de tipo de usuário e exigência de registros específicos (Sprint 1)
        validarPerfilCompleto(usuario);

        return "Login realizado com sucesso! Tipo: " + usuario.getTipoUsuario();
    }

    private void validarPerfilCompleto(Usuario usuario) {
        boolean perfilValido = switch (usuario.getTipoUsuario()) {
            case ENTUSIASTA, COLECIONADOR -> entusiastaRepository.existsByUsuarioId(usuario.getId());
            case ORGANIZADOR_EVENTO -> organizadorEventoRepository.existsByUsuarioId(usuario.getId());
            case PRESTADOR_SERVICO -> prestadorServicoRepository.existsByUsuarioId(usuario.getId());
        };

        if (!perfilValido) {
            throw new RuntimeException("Acesso negado: O seu perfil ainda não possui o documento obrigatório (CPF/CNPJ) cadastrado.");
        }
    }
}
