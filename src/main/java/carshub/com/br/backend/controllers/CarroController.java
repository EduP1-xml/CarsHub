package carshub.com.br.backend.controllers;

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
public class CarroController {

    private final CarroService servicoCarro;

    public CarroController(CarroService servicoCarro) {
        this.servicoCarro = servicoCarro;
    }

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<CarroRespostaDTO> registrarCarro(
            @RequestPart("dados") @Valid CarroRegistroDTO dados,
            @RequestPart(value = "arquivos", required = false) List<MultipartFile> arquivos,
            @RequestHeader("user-id") UUID idEntusiasta) {

        CarroRespostaDTO carroSalvo = servicoCarro.registrarCarro(dados, arquivos, idEntusiasta);
        return ResponseEntity.status(HttpStatus.CREATED).body(carroSalvo);
    }

    @GetMapping
    public ResponseEntity<List<CarroRespostaDTO>> listarCarros(
            @RequestHeader("user-id") UUID idEntusiasta) {
        List<CarroRespostaDTO> carros = servicoCarro.listarCarrosDoColecionador(idEntusiasta);
        return ResponseEntity.ok(carros);
    }

    @GetMapping("/{idCarro}")
    public ResponseEntity<CarroRespostaDTO> buscarCarro(
            @PathVariable UUID idCarro,
            @RequestHeader("user-id") UUID idEntusiasta) {
        CarroRespostaDTO carro = servicoCarro.buscarCarroPorId(idCarro, idEntusiasta);
        return ResponseEntity.ok(carro);
    }

    @PutMapping("/{idCarro}")
    public ResponseEntity<CarroRespostaDTO> substituirCarro(
            @PathVariable UUID idCarro,
            @RequestHeader("user-id") UUID idEntusiasta,
            @Valid @RequestBody CarroRegistroDTO dados) {
        CarroRespostaDTO carroAtualizado = servicoCarro.substituirCarro(idCarro, dados, idEntusiasta);
        return ResponseEntity.ok(carroAtualizado);
    }

    @PatchMapping("/{idCarro}")
    public ResponseEntity<CarroRespostaDTO> atualizarCarroParcial(
            @PathVariable UUID idCarro,
            @RequestHeader("user-id") UUID idEntusiasta,
            @RequestBody CarroPatchDTO dados) {
        CarroRespostaDTO carroAtualizado = servicoCarro.atualizarCarroCondicional(idCarro, dados, idEntusiasta);
        return ResponseEntity.ok(carroAtualizado);
    }

    @DeleteMapping("/{idCarro}")
    public ResponseEntity<Void> removerCarro(
            @PathVariable UUID idCarro,
            @RequestHeader("user-id") UUID idEntusiasta) {
        servicoCarro.removerCarro(idCarro, idEntusiasta);
        return ResponseEntity.noContent().build();
    }
}