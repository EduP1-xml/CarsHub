package carshub.com.br.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final UsuarioAutenticadoArgumentResolver resolverUsuario;

    public WebConfig(UsuarioAutenticadoArgumentResolver resolverUsuario) {
        this.resolverUsuario = resolverUsuario;
    }

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        // SPRINT 6 (11.4): resolve @UsuarioAutenticado UUID (subject do JWT)
        // no lugar do antigo @RequestHeader("user-id").
        resolvers.add(resolverUsuario);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Pega o caminho absoluto da pasta uploads
        Path uploadPath = Paths.get(uploadDir).toAbsolutePath().normalize();
        String uploadAbsolutePath = uploadPath.toUri().toString();

        registry.addResourceHandler("/uploads/**")
                .addResourceLocations(uploadAbsolutePath);
    }

}