package carshub.com.br.backend.services.credenciais;

import carshub.com.br.backend.exceptions.CampoObrigatorioException;
import carshub.com.br.backend.models.entities.Usuario;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.regex.Pattern;

@RequiredArgsConstructor
public class CredencialService {

    private static final String SENHA_REGEX =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$";

    private static final Pattern SENHA_PADRAO = Pattern.compile(SENHA_REGEX);

    private final PasswordEncoder codificadorSenha;

    /**
     * Gera o hash BCrypt de uma senha em texto puro.
     */
    public String codificar(String senhaTexto) {
        exigirSenhaInformada(senhaTexto);
        return codificadorSenha.encode(senhaTexto);
    }

    /**
     * Compara a senha informada com o hash armazenado.
     * Deve ser o UNICO caminho de verificacao de senha (nunca equals()).
     */
    public boolean verificar(String senhaTexto, String senhaArmazenada) {
        if (senhaTexto == null || senhaArmazenada == null) {
            return false;
        }
        return codificadorSenha.matches(senhaTexto, senhaArmazenada);
    }

    /**
     * Valida a politica de senha. Lanca excecao (HTTP 400) quando nao atende.
     */
    public void validarPolitica(String senhaTexto) {
        exigirSenhaInformada(senhaTexto);
        if (!SENHA_PADRAO.matcher(senhaTexto).matches()) {
            throw new IllegalArgumentException(
                    "A senha deve ter no mínimo 8 caracteres, uma letra maiúscula, uma minúscula, um número e um caractere especial");
        }
    }

    /**
     * Aplica a troca de senha de um usuario: valida a politica, gera o hash e
     * grava na entidade. E o metodo que os servicos devem usar.
     */
    public void atualizarSenha(Usuario usuario, String senhaTexto) {
        validarPolitica(senhaTexto);
        usuario.setSenha(codificar(senhaTexto));
    }

    private void exigirSenhaInformada(String senhaTexto) {
        if (senhaTexto == null || senhaTexto.isBlank()) {
            throw new CampoObrigatorioException("A senha é obrigatória.");
        }
    }
}