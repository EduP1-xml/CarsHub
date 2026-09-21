package carshub.com.br.backend.models.dtos.perfis.patchs;


import carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados;

import java.util.List;

public record PerfilMecanicoPatchDTO(
        String nome,
        String senha,
        String telefone,
        List<ServicosOfertados> servicosOfertados
) {}