package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.carros.CarroPatchDTO;
import carshub.com.br.backend.models.dtos.carros.CarroRegistroDTO;
import carshub.com.br.backend.models.dtos.carros.CarroRespostaDTO;
import carshub.com.br.backend.models.dtos.eventos.EventoRegistroDTO;
import carshub.com.br.backend.models.entities.Colecionador;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.OrganizadorEvento;
import carshub.com.br.backend.services.CarroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    public ResponseEntity<CarroRespostaDTO> registrarCarro(@RequestPart("data") @Valid CarroRegistroDTO data, @RequestPart(value = "file", required = false) List<MultipartFile> files, @AuthenticationPrincipal Entusiasta usuario) {
        CarroRespostaDTO carroSalvo = carroService.registrarCarro(data, files,  usuario.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(carroSalvo);
    }

    @GetMapping
    public ResponseEntity<List<CarroRespostaDTO>> listarCarros(
            @RequestHeader("user-id") UUID entusiastaId) {
        List<CarroRespostaDTO> carros = carroService.listarCarrosDoColecionador(entusiastaId);
        return ResponseEntity.ok(carros);
    }

    @GetMapping("/{idCarro}")
    public ResponseEntity<CarroRespostaDTO> buscarCarro(
            @PathVariable UUID idCarro,
            @RequestHeader("user-id") UUID entusiastaId) {
        CarroRespostaDTO carro = carroService.buscarCarroPorId(idCarro, entusiastaId);
        return ResponseEntity.ok(carro);
    }

    // PUT: Substituição completa do carro
    @PutMapping("/{idCarro}")
    public ResponseEntity<CarroRespostaDTO> substituirCarro(
            @PathVariable UUID idCarro,
            @RequestHeader("user-id") UUID entusiastaId,
            @Valid @RequestBody CarroRegistroDTO dto) {
        CarroRespostaDTO carroAtualizado = carroService.substituirCarro(idCarro, dto, entusiastaId);
        return ResponseEntity.ok(carroAtualizado);
    }

    // PATCH: Edição independente de campos
    @PatchMapping("/{idCarro}")
    public ResponseEntity<CarroRespostaDTO> atualizarCarroParcial(
            @PathVariable UUID idCarro,
            @RequestHeader("user-id") UUID entusiastaId,
            @RequestBody CarroPatchDTO dto) {
        CarroRespostaDTO carroAtualizado = carroService.atualizarCarroCondicional(idCarro, dto, entusiastaId);
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