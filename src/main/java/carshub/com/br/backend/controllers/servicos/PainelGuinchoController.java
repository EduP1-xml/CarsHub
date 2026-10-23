package carshub.com.br.backend.controllers.servicos;

import carshub.com.br.backend.models.dtos.PaginaRespostaDTO;
import carshub.com.br.backend.models.dtos.servicos.AvaliacaoServicoDTO;
import carshub.com.br.backend.models.dtos.servicos.PropostaServicoDTO;
import carshub.com.br.backend.models.dtos.servicos.ServicoDisponivelDTO;
import carshub.com.br.backend.models.dtos.servicos.ServicoRespostaDTO;
import carshub.com.br.backend.services.servicos.ServicoGuinchoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/carshub/guinchos/servicos")
@RequiredArgsConstructor
public class PainelGuinchoController {

    private final ServicoGuinchoService servicoGuincho;

    @GetMapping("/disponiveis")
    public ResponseEntity<PaginaRespostaDTO<ServicoDisponivelDTO>> listarDisponiveis(
            @RequestHeader("user-id") UUID id,
            @RequestParam(name = "raioKm", required = false) Double raioKm,
            @RequestParam(name = "latitude", required = false) Double latitude,
            @RequestParam(name = "longitude", required = false) Double longitude,
            @RequestParam(name = "endereco", required = false) String endereco,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamanho", defaultValue = "5") int tamanho) {

        List<ServicoDisponivelDTO> chamados =
                servicoGuincho.listarServicosDisponiveis(id, raioKm, latitude, longitude, endereco);

        return ResponseEntity.ok(PaginaRespostaDTO.de(chamados, pagina, tamanho));
    }

    @GetMapping
    public ResponseEntity<PaginaRespostaDTO<ServicoRespostaDTO>> listarMinhaFilaDeTrabalho(
            @RequestHeader("user-id") UUID id,
            @RequestParam(name = "pagina", defaultValue = "0") int pagina,
            @RequestParam(name = "tamanho", defaultValue = "5") int tamanho) {

        return ResponseEntity.ok(PaginaRespostaDTO.de(servicoGuincho.listarMinhaFilaDeTrabalho(id), pagina, tamanho));
    }

    @GetMapping("/{idServico}")
    public ResponseEntity<ServicoRespostaDTO> buscarServico(
            @PathVariable UUID idServico,
            @RequestHeader("user-id") UUID id) {

        return ResponseEntity.ok(servicoGuincho.buscarServico(idServico, id));
    }

    @PostMapping("/{idServico}/proposta")
    public ResponseEntity<ServicoRespostaDTO> enviarProposta(
            @PathVariable UUID idServico,
            @RequestHeader("user-id") UUID id,
            @Valid @RequestBody PropostaServicoDTO dados) {

        return ResponseEntity.ok(servicoGuincho.enviarProposta(idServico, id, dados));
    }

    @PatchMapping("/{idServico}/confirmar-chegada")
    public ResponseEntity<ServicoRespostaDTO> confirmarChegada(
            @PathVariable UUID idServico,
            @RequestHeader("user-id") UUID id) {

        return ResponseEntity.ok(servicoGuincho.confirmarChegadaPeloGuincho(idServico, id));
    }

    @PostMapping("/{idServico}/foto-final")
    public ResponseEntity<ServicoRespostaDTO> enviarFotoFinal(
            @PathVariable UUID idServico,
            @RequestHeader("user-id") UUID id,
            @RequestParam("arquivo") MultipartFile arquivo) {

        return ResponseEntity.ok(servicoGuincho.enviarFotoFinalPeloGuincho(idServico, id, arquivo));
    }

    @PostMapping("/{idServico}/avaliacao")
    public ResponseEntity<ServicoRespostaDTO> avaliarColecionador(
            @PathVariable UUID idServico,
            @RequestHeader("user-id") UUID id,
            @Valid @RequestBody AvaliacaoServicoDTO dados) {

        return ResponseEntity.ok(servicoGuincho.avaliarColecionador(idServico, id, dados));
    }

    @PatchMapping("/{idServico}/confirmar-conclusao")
    public ResponseEntity<ServicoRespostaDTO> confirmarConclusao(
            @PathVariable UUID idServico,
            @RequestHeader("user-id") UUID id) {

        return ResponseEntity.ok(servicoGuincho.confirmarConclusaoPeloGuincho(idServico, id));
    }
}