package carshub.com.br.backend.controllers.servicos;

import carshub.com.br.backend.models.dtos.servicos.PropostaServicoDTO;
import carshub.com.br.backend.models.dtos.servicos.ServicoDisponivelDTO;
import carshub.com.br.backend.models.dtos.servicos.ServicoRespostaDTO;
import carshub.com.br.backend.services.servicos.ServicoGuinchoService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Painel de trabalho do guincho: chamados da regiao e negociacoes em andamento. */
@RestController
@RequestMapping("/api/carshub/guinchos/servicos")
@RequiredArgsConstructor
public class PainelGuinchoController {

    private final ServicoGuinchoService servicoGuincho;

    /**
     * Chamados SOLICITADO na regiao, ESTRITAMENTE do mais proximo para o mais
     * distante. Origem: latitude/longitude da query > endereco geocodificado >
     * localizacao do perfil.
     */
    @GetMapping("/disponiveis")
    public ResponseEntity<List<ServicoDisponivelDTO>> listarDisponiveis(
            @RequestHeader("user-id") UUID id,
            @RequestParam(name = "raioKm", required = false) Double raioKm,
            @RequestParam(name = "latitude", required = false) Double latitude,
            @RequestParam(name = "longitude", required = false) Double longitude,
            @RequestParam(name = "endereco", required = false) String endereco) {

        return ResponseEntity.ok(servicoGuincho.listarServicosDisponiveis(id, raioKm, latitude, longitude, endereco));
    }

    @GetMapping
    public ResponseEntity<List<ServicoRespostaDTO>> listarMinhaFilaDeTrabalho(@RequestHeader("user-id") UUID id) {
        return ResponseEntity.ok(servicoGuincho.listarMinhaFilaDeTrabalho(id));
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

    @PatchMapping("/{idServico}/confirmar-conclusao")
    public ResponseEntity<ServicoRespostaDTO> confirmarConclusao(
            @PathVariable UUID idServico,
            @RequestHeader("user-id") UUID id) {

        return ResponseEntity.ok(servicoGuincho.confirmarConclusaoPeloGuincho(idServico, id));
    }
}