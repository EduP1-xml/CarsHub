package carshub.com.br.backend.models.dtos;

import carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

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
 *
 * Os campos abaixo sao opcionais no DTO e a obrigatoriedade e verificada no
 * CadastroService, porque depende do tipoUsuario. As anotacoes @Pattern
 * validam apenas o formato, quando o campo for informado.
 */
public record CadastroIndividualDTO(
        @NotBlank(message = "O e-mail é obrigatório para identificar a conta")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @Pattern(regexp = "\\d{11}", message = "O CPF deve conter exatamente 11 dígitos numéricos")
        String cpf,

        @Pattern(regexp = "\\d{14}", message = "O CNPJ deve conter exatamente 14 dígitos numéricos")
        String cnpj,

        @Pattern(regexp = "^[A-Za-z0-9]{7}$", message = "A placa deve conter exatamente 7 caracteres alfanuméricos")
        String placa,

        List<ServicosOfertados> servicosOfertados
) {}