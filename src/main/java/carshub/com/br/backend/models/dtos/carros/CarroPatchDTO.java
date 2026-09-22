package carshub.com.br.backend.models.dtos.carros;

import carshub.com.br.backend.models.enums.carros.*;
import java.math.BigDecimal;
import java.util.List;

public record CarroPatchDTO(
        String marca,
        String modelo,
        TipoCarroceria carroceria,
        Integer ano,
        String cor,
        Integer quantidadeCilindros,
        TipoMotor motor,
        BigDecimal litragemMotor,
        TipoInjecao injecao,
        TipoAlimentacao alimentacao,
        TipoCombustivel combustivel,
        TipoCambio cambio,
        TipoTracao tracao,
        TipoSuspencao suspencao,
        TipoFreio freio,
        TipoDirecao direcao,
        TipoRoda roda,
        Integer aro,
        Integer numeroPortas,
        Integer numeroAssentos,
        TipoMaterialRevestimentoInterno materialRevestimentoInterno,
        String placa,
        Boolean vidrosEletricos,
        Boolean som,
        Boolean arCondicionado,
        Boolean tetoSolar,
        List<String> fotosCarro
) {}