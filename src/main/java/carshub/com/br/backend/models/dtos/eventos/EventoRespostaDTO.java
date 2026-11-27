package carshub.com.br.backend.models.dtos.eventos;

import carshub.com.br.backend.models.enums.eventos.StatusEvento;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * F29 + AVISO 7.4: o evento devolve cidade/UF/CEP e as FOTOS do evento,
 * usadas no card/detalhe (carrossel).
 */
public record EventoRespostaDTO(
        UUID idEvento,
        String nome,
        BigDecimal preco,
        String descricao,
        Integer limiteParticipantes,
        String rua,
        String numero,
        String complemento,
        String bairro,
        String cidade,
        String uf,
        String cep,
        Double latitude,
        Double longitude,
        StatusEvento status,
        String nomeOrganizador,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        List<String> urlFotoEvento,
        /** null = evento aberto a todos os carros. */
        carshub.com.br.backend.models.dtos.clubes.FiltroCarroDTO filtroCarro
) {}
