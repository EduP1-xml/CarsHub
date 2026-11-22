package carshub.com.br.backend.config;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SPRINT 6 (11.4): substitui o header "user-id" nos controllers. O parametro
 * anotado (tipo UUID) e resolvido do SecurityContext pelo
 * {@link UsuarioAutenticadoArgumentResolver} — o id vem do subject do JWT
 * validado pelo {@link FiltroJwtAutenticacao}, nunca de um header que o
 * cliente pode forjar sem assinatura.
 *
 * required=false devolve null para requisicoes anonimas (ex.: catalogo de
 * eventos, que usa o usuario apenas para o flag "inscrito").
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UsuarioAutenticado {

    boolean required() default true;
}
