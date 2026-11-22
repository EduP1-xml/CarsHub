package carshub.com.br.backend.models.dtos.carros;

import carshub.com.br.backend.models.enums.carros.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * F13: o carro devolve TODOS os campos da entidade (inclusive as fotos) no
 * detalhe (GET /api/carshub/colecionador/carros/{idCarro}). Na listagem da
 * garagem a projecao nao carrega as fotos (vem lista vazia).
 */
public record CarroRespostaDTO(
        UUID idCarro,
        String marca,
        String modelo,
        String placa,
        String nomeProprietario,
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
        Boolean vidrosEletricos,
        Boolean som,
        Boolean arCondicionado,
        Boolean tetoSolar,
        List<String> fotosCarro
) {}
