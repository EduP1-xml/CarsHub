package carshub.com.br.backend.controllers;

import carshub.com.br.backend.config.UsuarioAutenticado;

import org.springframework.security.access.prepost.PreAuthorize;

import carshub.com.br.backend.models.dtos.PaginaRespostaDTO;
import carshub.com.br.backend.models.dtos.carros.CarroPatchDTO;
import carshub.com.br.backend.models.dtos.carros.CarroRegistroDTO;
import carshub.com.br.backend.models.dtos.carros.CarroRespostaDTO;
import carshub.com.br.backend.services.CarroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/carshub/colecionador/carros")
@PreAuthorize("hasAuthority('ENTUSIASTA')")
public class CarroController {

    private final CarroService servicoCarro;

    public CarroController(CarroService servicoCarro) {
        this.servicoCarro = servicoCarro;
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<CarroRespostaDTO> registrarCarro(
            @RequestPart("dados") @Valid CarroRegistroDTO dados,
            @RequestPart(value = "arquivos", required = false) List<MultipartFile> arquivos,
            @UsuarioAutenticado UUID idEntusiasta) {

        CarroRespostaDTO carroSalvo = servicoCarro.registrarCarro(dados, arquivos, idEntusiasta);
        return ResponseEntity.status(HttpStatus.CREATED).body(carroSalvo);
    }

    @GetMapping
    public ResponseEntity<PaginaRespostaDTO<CarroRespostaDTO>> listarCarros(
            @UsuarioAutenticado UUID idEntusiasta,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamanho", defaultValue = "5") int tamanho) {
        return ResponseEntity.ok(PaginaRespostaDTO.de(
                servicoCarro.listarCarrosDoColecionador(idEntusiasta, pagina, tamanho)));
    }

    @GetMapping("/{idCarro}")
    public ResponseEntity<CarroRespostaDTO> buscarCarro(
            @PathVariable UUID idCarro,
            @UsuarioAutenticado UUID idEntusiasta) {
        CarroRespostaDTO carro = servicoCarro.buscarCarroPorId(idCarro, idEntusiasta);
        return ResponseEntity.ok(carro);
    }

    // AVISO 7.9: PUT multipart como o POST — `dados` + `arquivos` opcionais.
    @PutMapping(value = "/{idCarro}", consumes = {"multipart/form-data"})
    public ResponseEntity<CarroRespostaDTO> substituirCarro(
            @PathVariable UUID idCarro,
            @UsuarioAutenticado UUID idEntusiasta,
            @RequestPart("dados") @Valid CarroRegistroDTO dados,
            @RequestPart(value = "arquivos", required = false) List<MultipartFile> arquivos) {
        CarroRespostaDTO carroAtualizado = servicoCarro.substituirCarro(idCarro, dados, arquivos, idEntusiasta);
        return ResponseEntity.ok(carroAtualizado);
    }

    // AVISO 7.8: PATCH multipart — `dados` + partes opcionais foto1/foto2
    // (as fotos sao trocadas uma a uma, sem exceder o limite de 2).
    @PatchMapping(value = "/{idCarro}", consumes = {"multipart/form-data"})
    public ResponseEntity<CarroRespostaDTO> atualizarCarroParcial(
            @PathVariable UUID idCarro,
            @UsuarioAutenticado UUID idEntusiasta,
            @RequestPart("dados") @Valid CarroPatchDTO dados,
            @RequestPart(value = "foto1", required = false) MultipartFile foto1,
            @RequestPart(value = "foto2", required = false) MultipartFile foto2) {
        CarroRespostaDTO carroAtualizado = servicoCarro.atualizarCarroCondicional(idCarro, dados, foto1, foto2, idEntusiasta);
        return ResponseEntity.ok(carroAtualizado);
    }

    @DeleteMapping("/{idCarro}")
    public ResponseEntity<Void> removerCarro(
            @PathVariable UUID idCarro,
            @UsuarioAutenticado UUID idEntusiasta) {
        servicoCarro.removerCarro(idCarro, idEntusiasta);
        return ResponseEntity.noContent().build();
    }
}