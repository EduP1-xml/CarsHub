package carshub.com.br.backend.models.dtos.eventos;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * F29 + AVISO 7.4: o card do catalogo devolve cidade/UF/CEP e as FOTOS do
 * evento (carrossel no card/detalhe).
 */
public record CatalogoEventoResponseDTO(
        UUID idEvento,
        String nome,
        String descricao,
        BigDecimal preco,
        String rua,
        String numero,
        String bairro,
        String cidade,
        String uf,
        String cep,
        Double latitude,
        Double longitude,
        String nomeOrganizador,
        String emailOrganizador,
        Integer vagasDisponiveis,
        Boolean inscrito,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        List<String> fotos
) {}
