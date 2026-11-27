package carshub.com.br.backend.controllers;

import carshub.com.br.backend.config.UsuarioAutenticado;
import carshub.com.br.backend.models.dtos.PaginaRespostaDTO;
import carshub.com.br.backend.models.dtos.carros.CarroRespostaDTO;
import carshub.com.br.backend.models.dtos.clubes.CarroCompativelDTO;
import carshub.com.br.backend.models.dtos.clubes.ClubeRegistroDTO;
import carshub.com.br.backend.models.dtos.clubes.ClubeRespostaDTO;
import carshub.com.br.backend.models.dtos.clubes.EntradaClubeDTO;
import carshub.com.br.backend.models.dtos.clubes.MembroClubeRespostaDTO;
import carshub.com.br.backend.models.dtos.eventos.CatalogoEventoResponseDTO;
import carshub.com.br.backend.services.clubes.ClubeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Clubes de entusiastas. Authority ENTUSIASTA (o colecionador tambem a tem).
 * Papel de administrador e participacao sao validados no ClubeService.
 */
@RestController
@RequestMapping("/api/carshub/clubes")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('ENTUSIASTA')")
public class ClubeController {

    private final ClubeService servicoClube;

    // ==================================================================
    // CLUBE
    // ==================================================================

    /** Multipart como o EventoController: "dados" (JSON) + "arquivos" opcionais (0 a 5 fotos). */
    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<ClubeRespostaDTO> criarClube(
            @RequestPart("dados") @Valid ClubeRegistroDTO dados,
            @RequestPart(value = "arquivos", required = false) List<MultipartFile> arquivos,
            @UsuarioAutenticado UUID idEntusiasta) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicoClube.criarClube(dados, arquivos, idEntusiasta));
    }

    @GetMapping
    public ResponseEntity<PaginaRespostaDTO<ClubeRespostaDTO>> listarClubes(
            @UsuarioAutenticado UUID idEntusiasta,
            @RequestParam(name = "busca", required = false) String busca,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamanho", defaultValue = "5") int tamanho) {
        return ResponseEntity.ok(PaginaRespostaDTO.de(servicoClube.listarClubes(busca, idEntusiasta, pagina, tamanho)));
    }

    @GetMapping("/meus")
    public ResponseEntity<PaginaRespostaDTO<ClubeRespostaDTO>> listarMeusClubes(
            @UsuarioAutenticado UUID idEntusiasta,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamanho", defaultValue = "5") int tamanho) {
        return ResponseEntity.ok(PaginaRespostaDTO.de(servicoClube.listarMeusClubes(idEntusiasta, pagina, tamanho)));
    }

    @GetMapping("/{idClube}")
    public ResponseEntity<ClubeRespostaDTO> detalharClube(
            @PathVariable UUID idClube,
            @UsuarioAutenticado UUID idEntusiasta) {
        return ResponseEntity.ok(servicoClube.detalharClube(idClube, idEntusiasta));
    }

    /** So o admin. Com arquivos, as fotos sao substituidas; sem, vale "fotosClube" do JSON. */
    @PutMapping(value = "/{idClube}", consumes = {"multipart/form-data"})
    public ResponseEntity<ClubeRespostaDTO> editarClube(
            @PathVariable UUID idClube,
            @UsuarioAutenticado UUID idEntusiasta,
            @RequestPart("dados") @Valid ClubeRegistroDTO dados,
            @RequestPart(value = "arquivos", required = false) List<MultipartFile> arquivos) {
        return ResponseEntity.ok(servicoClube.editarClube(idClube, dados, arquivos, idEntusiasta));
    }

    @DeleteMapping("/{idClube}")
    public ResponseEntity<Void> excluirClube(
            @PathVariable UUID idClube,
            @UsuarioAutenticado UUID idEntusiasta) {
        servicoClube.excluirClube(idClube, idEntusiasta);
        return ResponseEntity.noContent().build();
    }

    // ==================================================================
    // MEMBROS E ADMINISTRACAO
    // ==================================================================

    /** Entrada livre; corpo opcional com os carros a vincular ja na entrada. */
    @PostMapping("/{idClube}/membros")
    public ResponseEntity<ClubeRespostaDTO> entrarNoClube(
            @PathVariable UUID idClube,
            @UsuarioAutenticado UUID idEntusiasta,
            @RequestBody(required = false) @Valid EntradaClubeDTO dados) {
        return ResponseEntity.status(HttpStatus.CREATED).body(servicoClube.entrarNoClube(idClube, dados, idEntusiasta));
    }

    @DeleteMapping("/{idClube}/membros/eu")
    public ResponseEntity<Void> sairDoClube(
            @PathVariable UUID idClube,
            @UsuarioAutenticado UUID idEntusiasta) {
        servicoClube.sairDoClube(idClube, idEntusiasta);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{idClube}/membros/{idMembro}")
    public ResponseEntity<Void> removerMembro(
            @PathVariable UUID idClube,
            @PathVariable UUID idMembro,
            @UsuarioAutenticado UUID idEntusiasta) {
        servicoClube.removerMembro(idClube, idMembro, idEntusiasta);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{idClube}/membros")
    public ResponseEntity<PaginaRespostaDTO<MembroClubeRespostaDTO>> listarMembros(
            @PathVariable UUID idClube,
            @UsuarioAutenticado UUID idEntusiasta,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamanho", defaultValue = "10") int tamanho) {
        return ResponseEntity.ok(PaginaRespostaDTO.de(servicoClube.listarMembros(idClube, idEntusiasta, pagina, tamanho)));
    }

    @PatchMapping("/{idClube}/administrador/{idNovoAdministrador}")
    public ResponseEntity<ClubeRespostaDTO> transferirAdministracao(
            @PathVariable UUID idClube,
            @PathVariable UUID idNovoAdministrador,
            @UsuarioAutenticado UUID idEntusiasta) {
        return ResponseEntity.ok(servicoClube.transferirAdministracao(idClube, idNovoAdministrador, idEntusiasta));
    }

    // ==================================================================
    // CARROS
    // ==================================================================

    @GetMapping("/{idClube}/meus-carros-compativeis")
    public ResponseEntity<List<CarroCompativelDTO>> listarMeusCarrosCompativeis(
            @PathVariable UUID idClube,
            @UsuarioAutenticado UUID idEntusiasta) {
        return ResponseEntity.ok(servicoClube.listarMeusCarrosCompativeis(idClube, idEntusiasta));
    }

    @PostMapping("/{idClube}/carros/{idCarro}")
    public ResponseEntity<Void> vincularCarro(
            @PathVariable UUID idClube,
            @PathVariable UUID idCarro,
            @UsuarioAutenticado UUID idEntusiasta) {
        servicoClube.vincularCarro(idClube, idCarro, idEntusiasta);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{idClube}/carros/{idCarro}")
    public ResponseEntity<Void> desvincularCarro(
            @PathVariable UUID idClube,
            @PathVariable UUID idCarro,
            @UsuarioAutenticado UUID idEntusiasta) {
        servicoClube.desvincularCarro(idClube, idCarro, idEntusiasta);
        return ResponseEntity.noContent().build();
    }

    /** Aba Carros: so os carros vinculados (sem placa). */
    @GetMapping("/{idClube}/carros")
    public ResponseEntity<PaginaRespostaDTO<CarroRespostaDTO>> listarCarrosVinculados(
            @PathVariable UUID idClube,
            @UsuarioAutenticado UUID idEntusiasta,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamanho", defaultValue = "6") int tamanho) {
        return ResponseEntity.ok(PaginaRespostaDTO.de(
                servicoClube.listarCarrosVinculados(idClube, idEntusiasta, pagina, tamanho)));
    }

    // ==================================================================
    // EVENTOS
    // ==================================================================

    /** Aba Eventos: eventos ATIVOS compativeis com o gosto do clube. */
    @GetMapping("/{idClube}/eventos")
    public ResponseEntity<PaginaRespostaDTO<CatalogoEventoResponseDTO>> listarEventosCompativeis(
            @PathVariable UUID idClube,
            @UsuarioAutenticado UUID idEntusiasta,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamanho", defaultValue = "5") int tamanho) {
        return ResponseEntity.ok(PaginaRespostaDTO.de(
                servicoClube.listarEventosCompativeis(idClube, idEntusiasta, pagina, tamanho)));
    }
}
