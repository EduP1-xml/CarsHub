package carshub.com.br.backend.controllers.servicos;

import carshub.com.br.backend.models.dtos.servicos.HistoricoManutencaoResponseDTO;
import carshub.com.br.backend.services.servicos.PainelServicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Historico de manutencoes do veiculo. Fica num controller proprio para nao
 * alterar o CarroController (arquivo compartilhado com o outro desenvolvedor).
 */
@RestController
@RequestMapping("/api/carshub/colecionador/carros")
@RequiredArgsConstructor
public class HistoricoManutencaoController {

    private final PainelServicoService painelServico;

    @GetMapping("/{idCarro}/historico-manutencoes")
    public ResponseEntity<List<HistoricoManutencaoResponseDTO>> obterHistoricoDoVeiculo(
            @PathVariable UUID idCarro,
            @RequestHeader("user-id") UUID id) {

        return ResponseEntity.ok(painelServico.obterHistoricoDoVeiculo(idCarro, id));
    }
}