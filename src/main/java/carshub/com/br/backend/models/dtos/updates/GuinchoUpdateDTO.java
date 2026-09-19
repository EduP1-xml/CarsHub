package carshub.com.br.backend.models.dtos.updates;

public record GuinchoUpdateDTO(
        String nome,
        String senha,
        String telefone,
        String placa
) {}