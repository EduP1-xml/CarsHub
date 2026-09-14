package carshub.com.br.backend.models.dtos.updates;


import carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados;

import java.util.List;

public record MecanicoUpdateDTO(
        String nome,
        String senha,
        String telefone,
        List<ServicosOfertados> servicosOfertados
) {}