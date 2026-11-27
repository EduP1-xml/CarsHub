package carshub.com.br.backend.models.dtos.clubes;

import carshub.com.br.backend.models.enums.carros.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

/**
 * Filtro de carro (gosto do clube / carros aceitos no evento). Todos os campos
 * sao opcionais: campo ausente, lista vazia ou faixa sem pontas = criterio nao
 * marcado (qualquer valor passa). Os limites das faixas sao os mesmos da
 * entidade Carro. Faixa invertida (min > max) -> 422 no FiltroCarroMapper.
 *
 * Usado tanto na entrada quanto na resposta (na resposta, as listas vem
 * ordenadas e os campos nao marcados vem vazios/nulos).
 */
public record FiltroCarroDTO(
        @Size(max = 20, message = "Informe no máximo 20 marcas")
        List<@Size(max = 30, message = "Cada marca pode ter no máximo 30 caracteres") String> marcas,

        @Size(max = 20, message = "Informe no máximo 20 modelos")
        List<@Size(max = 30, message = "Cada modelo pode ter no máximo 30 caracteres") String> modelos,

        @Size(max = 20, message = "Informe no máximo 20 cores")
        List<@Size(max = 20, message = "Cada cor pode ter no máximo 20 caracteres") String> cores,

        Set<TipoCarroceria> carrocerias,
        Set<TipoMotor> motores,
        Set<TipoInjecao> injecoes,
        Set<TipoAlimentacao> alimentacoes,
        Set<TipoCombustivel> combustiveis,
        Set<TipoCambio> cambios,
        Set<TipoTracao> tracoes,
        Set<TipoSuspensao> suspensoes,
        Set<TipoFreio> freios,
        Set<TipoDirecao> direcoes,
        Set<TipoRoda> rodas,
        Set<TipoMaterialRevestimentoInterno> materiaisRevestimentoInterno,

        @Min(value = 1886, message = "O ano mínimo é 1886")
        Integer anoMin,
        @Min(value = 1886, message = "O ano mínimo é 1886")
        Integer anoMax,

        @Min(value = 1, message = "O número mínimo de cilindros é 1")
        @Max(value = 16, message = "O número máximo de cilindros é 16")
        Integer quantidadeCilindrosMin,
        @Min(value = 1, message = "O número mínimo de cilindros é 1")
        @Max(value = 16, message = "O número máximo de cilindros é 16")
        Integer quantidadeCilindrosMax,

        @DecimalMin(value = "0.049", message = "A litragem mínima é 0,049 L")
        @DecimalMax(value = "15.000", message = "A litragem máxima é 15,000 L")
        BigDecimal litragemMotorMin,
        @DecimalMin(value = "0.049", message = "A litragem mínima é 0,049 L")
        @DecimalMax(value = "15.000", message = "A litragem máxima é 15,000 L")
        BigDecimal litragemMotorMax,

        @Min(value = 8, message = "O número mínimo do aro é 8")
        @Max(value = 32, message = "O número máximo do aro é 32")
        Integer aroMin,
        @Min(value = 8, message = "O número mínimo do aro é 8")
        @Max(value = 32, message = "O número máximo do aro é 32")
        Integer aroMax,

        @Min(value = 1, message = "O número mínimo de portas é 1")
        @Max(value = 5, message = "O número máximo de portas é 5")
        Integer numeroPortasMin,
        @Min(value = 1, message = "O número mínimo de portas é 1")
        @Max(value = 5, message = "O número máximo de portas é 5")
        Integer numeroPortasMax,

        @Min(value = 1, message = "O número mínimo de assentos é 1")
        @Max(value = 50, message = "O número máximo de assentos é 50")
        Integer numeroAssentosMin,
        @Min(value = 1, message = "O número mínimo de assentos é 1")
        @Max(value = 50, message = "O número máximo de assentos é 50")
        Integer numeroAssentosMax,

        /** true = exige som; false/null = nao e criterio. */
        Boolean som
) {}
