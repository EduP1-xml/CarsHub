package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.CarroRegistroDTO;
import carshub.com.br.backend.models.dtos.CarroResponseDTO;
import carshub.com.br.backend.services.CarroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/carshub/colecionador/carros")
public class CarroController {

    private final CarroService carroService;

    public CarroController(CarroService carroService) {
        this.carroService = carroService;
    }

    @PostMapping
    public ResponseEntity<CarroResponseDTO> registrarCarro(
            @RequestHeader("user-id") UUID entusiastaId,
            @Valid @RequestBody CarroRegistroDTO dto) {
        CarroResponseDTO carroSalvo = carroService.registrarCarro(dto, entusiastaId);
        return ResponseEntity.status(HttpStatus.CREATED).body(carroSalvo);
    }

    @GetMapping
    public ResponseEntity<List<CarroResponseDTO>> listarCarros(
            @RequestHeader("user-id") UUID entusiastaId) {
        List<CarroResponseDTO> carros = carroService.listarCarrosDoColecionador(entusiastaId);
        return ResponseEntity.ok(carros);
    }

    @GetMapping("/{idCarro}")
    public ResponseEntity<CarroResponseDTO> buscarCarro(
            @PathVariable UUID idCarro,
            @RequestHeader("user-id") UUID entusiastaId) {
        CarroResponseDTO carro = carroService.buscarCarroPorId(idCarro, entusiastaId);
        return ResponseEntity.ok(carro);
    }

    @PutMapping("/{idCarro}")
    public ResponseEntity<CarroResponseDTO> atualizarCarro(
            @PathVariable UUID idCarro,
            @RequestHeader("user-id") UUID entusiastaId,
            @Valid @RequestBody CarroRegistroDTO dto) {
        CarroResponseDTO carroAtualizado = carroService.atualizarCarro(idCarro, dto, entusiastaId);
        return ResponseEntity.ok(carroAtualizado);
    }

    @DeleteMapping("/{idCarro}")
    public ResponseEntity<Void> removerCarro(
            @PathVariable UUID idCarro,
            @RequestHeader("user-id") UUID entusiastaId) {
        carroService.removerCarro(idCarro, entusiastaId);
        return ResponseEntity.noContent().build();
    }
}