package carshub.com.br.backend.controllers.servicos;

import carshub.com.br.backend.models.dtos.PaginaRespostaDTO;
import carshub.com.br.backend.models.dtos.servicos.PrestadorCompativelDTO;
import carshub.com.br.backend.models.dtos.servicos.ServicoRespostaDTO;
import carshub.com.br.backend.services.servicos.PainelServicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<PaginaRespostaDTO<ServicoRespostaDTO>> listarSolicitacoesAtivas(
            @RequestHeader("user-id") UUID id,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamanho", defaultValue = "5") int tamanho) {

        List<ServicoRespostaDTO> solicitacoes = painelServico.listarSolicitacoesAtivas(id);
        return ResponseEntity.ok(PaginaRespostaDTO.de(solicitacoes, pagina, tamanho));
    }

    /**
     * Ranking de prestadores compativeis de UM chamado, do melhor
     * score para o pior. Prestadores com cota cheia (2 ativos) NAO aparecem.
     */
    @GetMapping("/{idServico}/prestadores-compativeis")
    public ResponseEntity<PaginaRespostaDTO<PrestadorCompativelDTO>> listarPrestadoresCompativeis(
            @PathVariable UUID idServico,
            @RequestHeader("user-id") UUID id,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamanho", defaultValue = "5") int tamanho) {

        List<PrestadorCompativelDTO> prestadores = painelServico.listarPrestadoresCompativeis(idServico, id);
        return ResponseEntity.ok(PaginaRespostaDTO.de(prestadores, pagina, tamanho));
    }
}