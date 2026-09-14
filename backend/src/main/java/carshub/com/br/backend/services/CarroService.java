package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.PlacaJaCadastradaException;
import carshub.com.br.backend.exceptions.UsuarioNaoEncontradoException;
import carshub.com.br.backend.models.dtos.CarroRegistroDTO;
import carshub.com.br.backend.models.dtos.CarroResponseDTO;
import carshub.com.br.backend.models.entities.Carro;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.repositories.CarroRepository;
import carshub.com.br.backend.repositories.EntusiastaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CarroService {

    private final CarroRepository carroRepository;
    private final EntusiastaRepository entusiastaRepository;

    public CarroService(CarroRepository carroRepository, EntusiastaRepository entusiastaRepository) {
        this.carroRepository = carroRepository;
        this.entusiastaRepository = entusiastaRepository;
    }

    // POST: Criar Carro
    @Transactional
    public CarroResponseDTO registrarCarro(CarroRegistroDTO dto, UUID entusiastaId) {
        if (carroRepository.existsByPlaca(dto.placa())) {
            throw new PlacaJaCadastradaException("Esta placa já está registrada em outro veículo no CarsHub.");
        }

        Entusiasta proprietario = entusiastaRepository.findById(entusiastaId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Sessão inválida: Proprietário não encontrado."));

        proprietario.criaGaragem();
        entusiastaRepository.save(proprietario);

        Carro carro = mapearDtoParaEntidade(new Carro(), dto);
        carro.setColecionador(proprietario.getColecionador());

        Carro carroSalvo = carroRepository.save(carro);
        return mapearEntidadeParaResponse(carroSalvo, proprietario.getNome());
    }

    // GET: Listar todos os carros do usuário
    public List<CarroResponseDTO> listarCarrosDoColecionador(UUID entusiastaId) {
        Entusiasta proprietario = entusiastaRepository.findById(entusiastaId)
                .orElseThrow(() -> new UsuarioNaoEncontradoException("Sessão inválida."));

        if (proprietario.getColecionador() == null) {
            return List.of(); // Retorna lista vazia se ele ainda não tem coleção
        }

        return proprietario.getColecionador().getCarros().stream()
                .map(carro -> mapearEntidadeParaResponse(carro, proprietario.getNome()))
                .collect(Collectors.toList());
    }

    // GET: Buscar um carro específico
    public CarroResponseDTO buscarCarroPorId(UUID carroId, UUID entusiastaId) {
        Carro carro = verificarPropriedadeDoCarro(carroId, entusiastaId);
        return mapearEntidadeParaResponse(carro, carro.getColecionador().getEntusiasta().getNome());
    }

    // PUT: Atualizar informações do carro
    @Transactional
    public CarroResponseDTO atualizarCarro(UUID carroId, CarroRegistroDTO dto, UUID entusiastaId) {
        Carro carro = verificarPropriedadeDoCarro(carroId, entusiastaId);

        // Verifica se a nova placa não pertence a outro carro
        if (!carro.getPlaca().equals(dto.placa()) && carroRepository.existsByPlaca(dto.placa())) {
            throw new PlacaJaCadastradaException("A nova placa informada já está registrada em outro veículo.");
        }

        carro = mapearDtoParaEntidade(carro, dto);
        Carro carroAtualizado = carroRepository.save(carro);

        return mapearEntidadeParaResponse(carroAtualizado, carroAtualizado.getColecionador().getEntusiasta().getNome());
    }

    // DELETE: Remover carro (com a mágica da revogação do título)
    @Transactional
    public void removerCarro(UUID carroId, UUID entusiastaId) {
        Carro carro = verificarPropriedadeDoCarro(carroId, entusiastaId);
        Entusiasta proprietario = carro.getColecionador().getEntusiasta();

        carroRepository.delete(carro);
        proprietario.getColecionador().getCarros().remove(carro);

        proprietario.verificaExcluiGaragem(); // Regra de negócio em POO atuando!
        entusiastaRepository.save(proprietario);
    }

    // MÉTODOS AUXILIARES (Para não repetir código)

    private Carro verificarPropriedadeDoCarro(UUID carroId, UUID entusiastaId) {
        Carro carro = carroRepository.findById(carroId)
                .orElseThrow(() -> new RuntimeException("Carro não encontrado."));

        if (!carro.getColecionador().getId().equals(entusiastaId)) {
            throw new RuntimeException("Acesso Negado: Este veículo não pertence à sua coleção.");
        }
        return carro;
    }

    private Carro mapearDtoParaEntidade(Carro carro, CarroRegistroDTO dto) {
        carro.setMarca(dto.marca());
        carro.setModelo(dto.modelo());
        carro.setCarroceria(dto.carroceria());
        carro.setAno(dto.ano());
        carro.setCor(dto.cor());
        carro.setQuantidadeCilindros(dto.quantidadeCilindros());
        carro.setMotor(dto.motor());
        carro.setLitragemMotor(dto.litragemMotor());
        carro.setInjecao(dto.injecao());
        carro.setAlimentacao(dto.alimentacao());
        carro.setCombustivel(dto.combustivel());
        carro.setCambio(dto.cambio());
        carro.setTracao(dto.tracao());
        carro.setSuspencao(dto.suspencao());
        carro.setFreio(dto.freio());
        carro.setDirecao(dto.direcao());
        carro.setRoda(dto.roda());
        carro.setAro(dto.aro());
        carro.setNumeroPortas(dto.numeroPortas());
        carro.setNumeroAssentos(dto.numeroAssentos());
        carro.setMaterialRevestimentoInterno(dto.materialRevestimentoInterno());
        carro.setPlaca(dto.placa());
        carro.setVidrosEletricos(dto.vidrosEletricos());
        carro.setSom(dto.som());
        carro.setArCondicionado(dto.arCondicionado());
        carro.setTetoSolar(dto.tetoSolar());
        return carro;
    }

    private CarroResponseDTO mapearEntidadeParaResponse(Carro carro, String nomeProprietario) {
        return new CarroResponseDTO(
                carro.getId(),
                carro.getMarca(),
                carro.getModelo(),
                carro.getPlaca(),
                nomeProprietario
        );
    }
}