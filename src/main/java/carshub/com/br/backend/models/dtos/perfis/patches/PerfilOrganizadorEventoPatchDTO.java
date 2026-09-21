package carshub.com.br.backend.models.dtos.perfis.patchs;

public record PerfilOrganizadorEventoPatchDTO(
        String nome,
        String senha,
        String telefone
) {}