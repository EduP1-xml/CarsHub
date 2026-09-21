package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.CampoObrigatorioException;
import carshub.com.br.backend.exceptions.SenhaIncorretaException;
import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.models.dtos.LatitudeLogintudeDTO;
import carshub.com.br.backend.models.dtos.LoginDTO;
import carshub.com.br.backend.models.dtos.UserPerfilDTO;
import carshub.com.br.backend.models.entities.*;
import carshub.com.br.backend.models.enums.tiposUsuarios.TipoUsuario;
import carshub.com.br.backend.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class LoginService {
    private final UsuarioRepository usuarioRepository;

    public String autenticar(LoginDTO loginDTO) {
        Usuario usuario = usuarioRepository.findByLogin(loginDTO.login()).orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

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
            subtipo = ", com a especificidade de " + prestador.getTipoPrestadorServico();
        }
        return "Login realizado com sucesso! Bem-vindo, " + usuario.getNome() +
                " registrado com sucesso! ID: " + usuario.getId() + ", seu registro é do tipo:" + usuario.getTipoUsuario() + subtipo;
    }

    public Usuario update(UserPerfilDTO dados, Usuario usuarioAutenticado) {
        Usuario entity = usuarioRepository.findById(usuarioAutenticado.getId()).orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));
        if (!entity.getId().equals(usuarioAutenticado.getId())) {
            throw new SecurityException("Você não tem permissão para editar");
        }
        updateUser(entity, dados);
        return usuarioRepository.save(entity);
    }

    private void updateUser(Usuario entity, UserPerfilDTO dados) {
        if (dados.nome() != null) {
            entity.setNome(dados.nome());
        }
        if (dados.telefone() != null && !dados.telefone().isEmpty()) {
            entity.setTelefone(validaPhone(dados.telefone()));
        }
        if (dados.urlFotoPerfil() != null) {
            entity.setUrlFotoPerfil(dados.urlFotoPerfil());
        }
    }

    public String validaPhone(String phone) {
        String PHONE_REGEX = "^(?:\\([1-9]{2}\\)|[1-9]{2})\\s?(?:[2-8]|9\\d)\\d{3}\\-?\\d{4}$";
        Pattern PHONE_PATTERN = Pattern.compile(PHONE_REGEX);
        if (!PHONE_PATTERN.matcher(phone).matches()) {
            throw new IllegalArgumentException("Formato inválido. Use (DD) 9XXXX-XXXX");
        }
        return phone;
    }

    public Usuario updateLoc(LatitudeLogintudeDTO dados, Usuario usuarioAutenticado) {
        Usuario entity = usuarioRepository.findById(usuarioAutenticado.getId()).orElseThrow(() -> new RuntimeException("Perfil não encontrado!"));
        updateLocs(entity, dados);
        return usuarioRepository.save(entity);
    }

    private void updateLocs(Usuario entity, LatitudeLogintudeDTO dados) {
        if (dados.latitude() != null) {
            entity.setLatitude(dados.latitude());
        }
        if (dados.longitude() != null) {
            entity.setLongitude(dados.longitude());
        }
    }
}
