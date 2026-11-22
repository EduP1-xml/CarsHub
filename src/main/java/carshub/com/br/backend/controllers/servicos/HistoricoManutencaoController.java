package carshub.com.br.backend.controllers.servicos;

import carshub.com.br.backend.config.UsuarioAutenticado;

import org.springframework.security.access.prepost.PreAuthorize;

import carshub.com.br.backend.models.dtos.PaginaRespostaDTO;
import carshub.com.br.backend.models.dtos.servicos.HistoricoManutencaoResponseDTO;
import carshub.com.br.backend.services.servicos.PainelServicoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Historico de manutencoes do veiculo. Fica num controller proprio para nao
 * alterar o CarroController (arquivo compartilhado com o outro desenvolvedor).
 */
@RestController
@RequestMapping("/api/carshub/colecionador/carros")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ENTUSIASTA')")
public class HistoricoManutencaoController {

    private final PainelServicoService painelServico;

    @GetMapping("/{idCarro}/historico-manutencoes")
    public ResponseEntity<PaginaRespostaDTO<HistoricoManutencaoResponseDTO>> obterHistoricoDoVeiculo(
            @PathVariable UUID idCarro,
            @UsuarioAutenticado UUID id,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamanho", defaultValue = "5") int tamanho) {

        return ResponseEntity.ok(painelServico.obterHistoricoDoVeiculo(idCarro, id, pagina, tamanho));
    }
}