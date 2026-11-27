package carshub.com.br.backend.models.dtos.clubes;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Criacao (POST) e substituicao (PUT) do clube — parte "dados" do multipart.
 * fotosClube segue o urlFotoEvento do EventoRegistroDTO: sem anotacao, o limite
 * de 5 e validado no ClubeService; no PUT sem arquivos, mantem as fotos listadas.
 */
public record ClubeRegistroDTO(
        @NotBlank(message = "O nome do clube é obrigatório")
        @Size(max = 50, message = "O nome do clube não pode exceder 50 caracteres")
        String nome,

        @Size(max = 500, message = "A descrição do clube não pode exceder 500 caracteres")
        String descricao,

        @NotNull(message = "O gosto do clube (filtro de carros) é obrigatório")
        @Valid
        FiltroCarroDTO filtro,

        List<String> fotosClube
) {}
