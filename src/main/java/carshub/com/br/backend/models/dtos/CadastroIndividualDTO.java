package carshub.com.br.backend.models.dtos;

import carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

/**
 * Segunda etapa do cadastro: dados especificos da classe concreta.
 *
 * O e-mail identifica a conta criada na primeira etapa. A conta ainda nao
 * esta ativa (o e-mail nao foi verificado), por isso a identificacao e feita
 * pelo e-mail e nao por sessao.
 *
 * Qual campo preencher depende do tipoUsuario gravado na primeira etapa:
 *   ENTUSIASTA / COLECIONADOR  -> cpf
 *   ORGANIZADOR_EVENTO         -> cnpj
 *   PRESTADOR_SERVICO (GUINCHO)  -> cnpj + placa
 *   PRESTADOR_SERVICO (MECANICO) -> cnpj + servicosOfertados
 */
public record CadastroIndividualDTO(
        @NotBlank(message = "O e-mail é obrigatório para identificar a conta")
        @Email(message = "Formato de e-mail inválido")
        String email,

        String cpf,
        String cnpj,
        String placa,
        List<ServicosOfertados> servicosOfertados
) {}