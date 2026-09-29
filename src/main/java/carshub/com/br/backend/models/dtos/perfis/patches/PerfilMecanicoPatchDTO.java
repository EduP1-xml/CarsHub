package carshub.com.br.backend.models.dtos.perfis.patches;

import carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados;

import java.util.List;

public record PerfilMecanicoPatchDTO(
        String nome,
        String telefone,
        List<ServicosOfertados> servicosOfertados
) {}