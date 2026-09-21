package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.PerfilIncompativelException;
import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.exceptions.RegraDeNegocioException;
import carshub.com.br.backend.models.dtos.inscricoes.InscricaoRegistroDTO;
import carshub.com.br.backend.models.dtos.inscricoes.InscricaoResponseDTO;
import carshub.com.br.backend.models.entities.Carro;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.Evento;
import carshub.com.br.backend.models.entities.Inscricao;
import carshub.com.br.backend.models.enums.eventos.StatusEvento;
import carshub.com.br.backend.models.enums.inscricoes.StatusInscricao;
import carshub.com.br.backend.repositories.CarroRepository;
import carshub.com.br.backend.repositories.EntusiastaRepository;
import carshub.com.br.backend.repositories.EventoRepository;
import carshub.com.br.backend.repositories.InscricaoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class InscricaoService {

    private final InscricaoRepository inscricaoRepository;
    private final EntusiastaRepository entusiastaRepository;
    private final EventoRepository eventoRepository;
    private final CarroRepository carroRepository;
    
    @Transactional
    public InscricaoResponseDTO realizarInscricao(UUID idEntusiasta, InscricaoRegistroDTO dto) {
        Entusiasta entusiasta = entusiastaRepository.findById(idEntusiasta)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão inválida: Usuário não encontrado."));

        Evento evento = eventoRepository.findById(dto.idEvento())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Evento não encontrado."));

        // 1. Valida se o evento está aberto para o público
        if (evento.getStatus() != StatusEvento.ATIVO) {
            throw new RegraDeNegocioException("As inscrições para este evento estão fechadas ou o evento foi cancelado.");
        }

        // 2. Valida se já não está inscrito
        if (inscricaoRepository.existsByEntusiasta_IdAndEvento_IdEventoAndStatus(idEntusiasta, dto.idEvento(), StatusInscricao.CONFIRMADA)) {
            throw new RegraDeNegocioException("Você já possui uma inscrição ativa para este evento.");
        }

        // 3. Prevenção de Overbooking
        int vagasOcupadas = inscricaoRepository.countByEvento_IdEventoAndStatus(dto.idEvento(), StatusInscricao.CONFIRMADA);
        if (vagasOcupadas >= evento.getLimiteParticipantes()) {
            throw new RegraDeNegocioException("Infelizmente o limite de participantes deste evento já foi atingido (Lotação Esgotada).");
        }

        Inscricao inscricao = new Inscricao();
        inscricao.setEntusiasta(entusiasta);
        inscricao.setEvento(evento);

        // 4. Se o entusiasta mandou um carro, valida a propriedade e vincula
        if (dto.idCarro() != null) {
            Carro carro = carroRepository.findById(dto.idCarro())
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Carro não encontrado."));
            
            if (!carro.getColecionador().getId().equals(idEntusiasta)) {
                throw new PerfilIncompativelException("Acesso Negado: Este veículo não pertence à sua coleção.");
            }
            inscricao.setCarro(carro);
        }

        Inscricao inscricaoSalva = inscricaoRepository.save(inscricao);
        return mapearParaDTO(inscricaoSalva);
    }

    public List<InscricaoResponseDTO> listarMinhasInscricoes(UUID idEntusiasta) {
        return inscricaoRepository.findAllByEntusiasta_Id(idEntusiasta).stream()
                .map(this::mapearParaDTO)
                .collect(Collectors.toList());
    }

    @Transactional
    public void cancelarInscricao(UUID idInscricao, UUID idEntusiasta) {
        Inscricao inscricao = inscricaoRepository.findById(idInscricao)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Inscrição não encontrada."));

        if (!inscricao.getEntusiasta().getId().equals(idEntusiasta)) {
            throw new PerfilIncompativelException("Acesso Negado: Esta inscrição não pertence a você.");
        }

        if (inscricao.getStatus() == StatusInscricao.CANCELADA) {
            throw new RegraDeNegocioException("Esta inscrição já se encontra cancelada.");
        }

        inscricao.setStatus(StatusInscricao.CANCELADA);
        inscricaoRepository.save(inscricao); // O status muda, liberando a vaga no evento para outra pessoa!
    }

    private InscricaoResponseDTO mapearParaDTO(Inscricao inscricao) {
        String infoCarro = inscricao.getCarro() != null 
                ? inscricao.getCarro().getMarca() + " " + inscricao.getCarro().getModelo() + " (" + inscricao.getCarro().getPlaca() + ")" 
                : "Sem veículo vinculado";

        return new InscricaoResponseDTO(
                inscricao.getIdInscricao(),
                inscricao.getEvento().getNome(),
                inscricao.getEntusiasta().getNome(),
                infoCarro,
                inscricao.getStatus(),
                inscricao.getData()
        );
    }
}