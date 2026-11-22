package carshub.com.br.backend.models.dtos.carros;

import carshub.com.br.backend.models.enums.carros.*;
import java.math.BigDecimal;

/**
 * AVISO 7.8: as fotos deixaram de vir como lista de URLs no JSON — o PATCH
 * multipart aceita as partes opcionais foto1/foto2 (troca slot a slot).
 */
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
        TipoSuspensao suspensao,
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
        Boolean tetoSolar
) {}
