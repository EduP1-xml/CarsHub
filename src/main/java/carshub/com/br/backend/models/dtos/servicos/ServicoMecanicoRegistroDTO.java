package carshub.com.br.backend.models.dtos.servicos;

import carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * A localizacao da ocorrencia tem TRES origens, nesta ordem de prioridade:
 *
 *   1. latitude + longitude  -> HTML5 Geolocation no dispositivo (dado exato);
 *   2. endereco              -> input manual; o BACK traduz com a Geocoding API
 *                               ("geolocalizacao imputada", ex.: o carro quebrou
 *                               em outro lugar);
 *   3. nada informado        -> usa a ultima localizacao do perfil do colecionador.
 */
public record ServicoMecanicoRegistroDTO(
        @NotNull(message = "O ID do carro é obrigatório para solicitar o serviço.")
        UUID idCarro,

        @NotEmpty(message = "Informe pelo menos um serviço a ser realizado no veículo.")
        List<ServicosOfertados> servicosOfertados,

        Double latitude,
        Double longitude,

        /* Endereco digitado pelo usuario. Ignorado quando latitude/longitude vem preenchidos. */
        String endereco
) {}