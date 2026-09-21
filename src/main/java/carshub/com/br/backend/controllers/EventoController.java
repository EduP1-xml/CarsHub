package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.eventos.EventoPatchDTO;
import carshub.com.br.backend.models.dtos.eventos.EventoRegistroDTO;
import carshub.com.br.backend.models.dtos.eventos.EventoRespostaDTO;
import carshub.com.br.backend.models.enums.eventos.StatusEvento;
import carshub.com.br.backend.services.EventoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/carshub/organizadores/eventos")
public class EventoController {

    private final EventoService eventoService;

    public EventoController(EventoService eventoService) {
        this.eventoService = eventoService;
    }

    @PostMapping
    public ResponseEntity<EventoRespostaDTO> criarEvento(
            @RequestHeader("user-id") UUID organizadorId,
            @Valid @RequestBody EventoRegistroDTO dto) {
        EventoRespostaDTO eventoSalvo = eventoService.criarEvento(dto, organizadorId);
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoSalvo);
    }

    @GetMapping
    public ResponseEntity<List<EventoRespostaDTO>> listarMeusEventos(
            @RequestHeader("user-id") UUID organizadorId) {
        List<EventoRespostaDTO> eventos = eventoService.listarMeusEventos(organizadorId);
        return ResponseEntity.ok(eventos);
    }

    @GetMapping("/{idEvento}")
    public ResponseEntity<EventoRespostaDTO> buscarEvento(
            @PathVariable UUID idEvento,
            @RequestHeader("user-id") UUID organizadorId) {
        EventoRespostaDTO evento = eventoService.buscarMeuEventoPorId(idEvento, organizadorId);
        return ResponseEntity.ok(evento);
    }

    @PutMapping("/{idEvento}")
    public ResponseEntity<EventoRespostaDTO> substituirEvento(
            @PathVariable UUID idEvento,
            @RequestHeader("user-id") UUID organizadorId,
            @Valid @RequestBody EventoRegistroDTO dto) {
        EventoRespostaDTO eventoAtualizado = eventoService.substituirEvento(idEvento, dto, organizadorId);
        return ResponseEntity.ok(eventoAtualizado);
    }

    @PatchMapping("/{idEvento}")
    public ResponseEntity<EventoRespostaDTO> atualizarEventoParcial(
            @PathVariable UUID idEvento,
            @RequestHeader("user-id") UUID organizadorId,
            @RequestBody EventoPatchDTO dto) {
        EventoRespostaDTO eventoAtualizado = eventoService.atualizarEventoCondicional(idEvento, dto, organizadorId);
        return ResponseEntity.ok(eventoAtualizado);
    }

    @PatchMapping("/{idEvento}/status")
    public ResponseEntity<EventoRespostaDTO> alterarStatus(
            @PathVariable UUID idEvento,
            @RequestHeader("user-id") UUID organizadorId,
            @RequestParam StatusEvento novoStatus) {
        EventoRespostaDTO eventoAtualizado = eventoService.alterarStatusEvento(idEvento, novoStatus, organizadorId);
        return ResponseEntity.ok(eventoAtualizado);
    }

    @DeleteMapping("/{idEvento}")
    public ResponseEntity<Void> excluirEvento(
            @PathVariable UUID idEvento,
            @RequestHeader("user-id") UUID organizadorId) {
        eventoService.excluirEvento(idEvento, organizadorId);
        return ResponseEntity.noContent().build();
    }
}