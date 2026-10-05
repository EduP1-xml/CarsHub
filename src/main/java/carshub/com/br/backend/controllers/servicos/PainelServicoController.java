package carshub.com.br.backend.controllers.servicos;

import carshub.com.br.backend.models.dtos.servicos.ServicoRespostaDTO;
import carshub.com.br.backend.services.servicos.PainelServicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * Rota PADRAO do painel do colecionador: devolve as solicitacoes ativas dos dois
 * tipos. As listas separadas ficam em /servicos/mecanicos e /servicos/guinchos.
 */
@RestController
@RequestMapping("/api/carshub/colecionador/servicos")
@RequiredArgsConstructor
public class PainelServicoController {

    private final PainelServicoService painelServico;

    @GetMapping
    public ResponseEntity<List<ServicoRespostaDTO>> listarSolicitacoesAtivas(@RequestHeader("user-id") UUID id) {
        return ResponseEntity.ok(painelServico.listarSolicitacoesAtivas(id));
    }
}