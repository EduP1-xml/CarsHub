package carshub.com.br.backend.models.dtos.clubes;

import java.util.UUID;

/** Resumo de um carro vinculado a um clube (sem placa, por privacidade). */
public record CarroVinculadoResumoDTO(
        UUID idCarro,
        String marca,
        String modelo,
        Integer ano,
        String cor,
        String fotoCapa
) {}
