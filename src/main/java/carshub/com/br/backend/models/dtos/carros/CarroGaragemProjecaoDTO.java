package carshub.com.br.backend.models.dtos.carros;

import carshub.com.br.backend.models.enums.carros.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * AJUSTE 1.1b: projecao de leitura da garagem — os escalares que o Page&lt;T&gt;
 * traz direto do SQL (a colecao fotosCarro e lazy e nao entra em projecao).
 * Nao e contrato de API: o CarroService converte para CarroRespostaDTO.
 */
public record CarroGaragemProjecaoDTO(
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
        Boolean tetoSolar
) {}
