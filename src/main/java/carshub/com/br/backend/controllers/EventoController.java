package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.dtos.PaginaRespostaDTO;
import carshub.com.br.backend.models.dtos.eventos.EventoPatchDTO;
import carshub.com.br.backend.models.dtos.eventos.EventoPutDTO;
import carshub.com.br.backend.models.dtos.eventos.EventoRegistroDTO;
import carshub.com.br.backend.models.dtos.eventos.EventoRespostaDTO;
import carshub.com.br.backend.models.enums.eventos.StatusEvento;
import carshub.com.br.backend.services.EventoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/carshub/organizadores/eventos")
@RequiredArgsConstructor
public class EventoController {

    private final EventoService servicoEvento;

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<EventoRespostaDTO> criarEvento(
            @RequestPart("dados") @Valid EventoRegistroDTO dados,
            @RequestPart(value = "arquivos", required = false) List<MultipartFile> arquivos,
            @RequestHeader("user-id") UUID idOrganizador) {

        EventoRespostaDTO eventoSalvo = servicoEvento.criarEvento(dados, arquivos, idOrganizador);
        return ResponseEntity.status(HttpStatus.CREATED).body(eventoSalvo);
    }

    @GetMapping
    public ResponseEntity<PaginaRespostaDTO<EventoRespostaDTO>> listarMeusEventos(
            @RequestHeader("user-id") UUID idOrganizador,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamanho", defaultValue = "5") int tamanho) {
        return ResponseEntity.ok(PaginaRespostaDTO.de(
                servicoEvento.listarMeusEventos(idOrganizador, pagina, tamanho)));
    }

    @GetMapping("/{idEvento}")
    public ResponseEntity<EventoRespostaDTO> buscarEvento(
            @PathVariable UUID idEvento,
            @RequestHeader("user-id") UUID idOrganizador) {
        EventoRespostaDTO evento = servicoEvento.buscarMeuEventoPorId(idEvento, idOrganizador);
        return ResponseEntity.ok(evento);
    }

    @PutMapping("/{idEvento}")
    public ResponseEntity<EventoRespostaDTO> substituirEvento(
            @PathVariable UUID idEvento,
            @RequestHeader("user-id") UUID idOrganizador,
            @Valid @RequestBody EventoPutDTO dados) {
        EventoRespostaDTO eventoAtualizado = servicoEvento.substituirEvento(idEvento, dados, idOrganizador);
        return ResponseEntity.ok(eventoAtualizado);
    }

    @PatchMapping("/{idEvento}")
    public ResponseEntity<EventoRespostaDTO> atualizarEventoParcial(
            @PathVariable UUID idEvento,
            @RequestHeader("user-id") UUID idOrganizador,
            @RequestBody EventoPatchDTO dados) {
        EventoRespostaDTO eventoAtualizado = servicoEvento.atualizarEventoCondicional(idEvento, dados, idOrganizador);
        return ResponseEntity.ok(eventoAtualizado);
    }

    @PatchMapping("/{idEvento}/status")
    public ResponseEntity<EventoRespostaDTO> alterarStatus(
            @PathVariable UUID idEvento,
            @RequestHeader("user-id") UUID idOrganizador,
            @RequestParam StatusEvento novoStatus) {
        EventoRespostaDTO eventoAtualizado = servicoEvento.alterarStatusEvento(idEvento, novoStatus, idOrganizador);
        return ResponseEntity.ok(eventoAtualizado);
    }

    @DeleteMapping("/{idEvento}")
    public ResponseEntity<Void> excluirEvento(
            @PathVariable UUID idEvento,
            @RequestHeader("user-id") UUID idOrganizador) {
        servicoEvento.excluirEvento(idEvento, idOrganizador);
        return ResponseEntity.noContent().build();
    }
}