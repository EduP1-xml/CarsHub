package carshub.com.br.backend.models.dtos.clubes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Membro do clube. Para o entusiasta sem garagem, carrosVinculados vem vazio;
 * para o colecionador, traz os carros que ele vinculou a este clube.
 */
public record MembroClubeRespostaDTO(
        UUID idEntusiasta,
        String nome,
        String urlFotoPerfil,
        boolean administrador,
        LocalDateTime dataEntrada,
        List<CarroVinculadoResumoDTO> carrosVinculados
) {}
