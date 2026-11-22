package carshub.com.br.backend.config;

import carshub.com.br.backend.exceptions.NaoAutenticadoException;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

/**
 * SPRINT 6 (11.4): resolve parametros @UsuarioAutenticado UUID a partir do
 * SecurityContext (o FiltroJwtAutenticacao grava o subject do JWT como
 * principal). Registrado no WebConfig.
 *
 * Parametro obrigatorio sem autenticacao -> NaoAutenticadoException (401) —
 * defesa em profundidade: as rotas protegidas ja barram anonimos na camada de
 * URL, entao este caminho so dispara se uma regra futura afrouxar a URL.
 */
@Component
public class UsuarioAutenticadoArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(UsuarioAutenticado.class)
                && UUID.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        UsuarioAutenticado anotacao = parameter.getParameterAnnotation(UsuarioAutenticado.class);
        boolean obrigatorio = anotacao == null || anotacao.required();

        Authentication autenticacao = SecurityContextHolder.getContext().getAuthentication();
        if (autenticacao != null && autenticacao.getPrincipal() instanceof UUID idUsuario) {
            return idUsuario;
        }

        if (obrigatorio) {
            throw new NaoAutenticadoException("Acesso negado: autenticação necessária.");
        }
        return null;
    }
}
