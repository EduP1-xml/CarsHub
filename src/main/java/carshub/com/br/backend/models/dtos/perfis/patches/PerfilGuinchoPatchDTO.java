package carshub.com.br.backend.models.dtos.perfis.patches;

public record PerfilGuinchoPatchDTO(
        String nome,
        String senha,
        String telefone,
        String placa
) {}