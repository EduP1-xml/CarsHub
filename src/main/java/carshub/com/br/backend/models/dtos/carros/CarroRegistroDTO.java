package carshub.com.br.backend.models.dtos.carros;

import carshub.com.br.backend.models.enums.carros.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record CarroRegistroDTO(
        @NotBlank(message = "A marca é obrigatória") String marca,
        @NotBlank(message = "O modelo é obrigatório") String modelo,
        @NotNull TipoCarroceria carroceria,
        @NotNull @Min(1886) Integer ano,
        @NotBlank String cor,
        @NotNull @Min(1) @Max(16) Integer quantidadeCilindros,
        @NotNull TipoMotor motor,
        @NotNull @DecimalMin("0.049") @DecimalMax("15.000") BigDecimal litragemMotor,
        @NotNull TipoInjecao injecao,
        @NotNull TipoAlimentacao alimentacao,
        @NotNull TipoCombustivel combustivel,
        @NotNull TipoCambio cambio,
        @NotNull TipoTracao tracao,
        @NotNull TipoSuspencao suspencao,
        @NotNull TipoFreio freio,
        @NotNull TipoDirecao direcao,
        @NotNull TipoRoda roda,
        @NotNull @Min(8) @Max(32) Integer aro,
        @NotNull @Min(1) @Max(5) Integer numeroPortas,
        @NotNull @Min(1) @Max(50) Integer numeroAssentos,
        @NotNull TipoMaterialRevestimentoInterno materialRevestimentoInterno,
        @NotBlank @Pattern(regexp = "^[A-Za-z0-9]{7}$") String placa,
        @NotNull Boolean vidrosEletricos,
        @NotNull Boolean som,
        @NotNull Boolean arCondicionado,
        @NotNull Boolean tetoSolar
) {}