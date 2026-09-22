package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.PerfilIncompativelException;
import carshub.com.br.backend.exceptions.PlacaJaCadastradaException;
import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.models.dtos.carros.CarroPatchDTO;
import carshub.com.br.backend.models.dtos.carros.CarroRegistroDTO;
import carshub.com.br.backend.models.dtos.carros.CarroRespostaDTO;
import carshub.com.br.backend.models.entities.Carro;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.repositories.CarroRepository;
import carshub.com.br.backend.repositories.EntusiastaRepository;
import carshub.com.br.backend.services.file.FileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CarroService {

    private final CarroRepository carroRepository;
    private final EntusiastaRepository entusiastaRepository;
    private final FileStorageService fileStorageService;


    // POST: Criar Carro
    @Transactional
    public CarroRespostaDTO registrarCarro(CarroRegistroDTO dto, List<MultipartFile> files, UUID entusiastaId) {
        if (carroRepository.existsByPlaca(dto.placa())) {
            throw new PlacaJaCadastradaException("Esta placa já está registrada em outro veículo no CarsHub.");
        }
        Entusiasta proprietario = entusiastaRepository.findById(entusiastaId).orElseThrow(() -> new RecursoNaoEncontradoException("Sessão inválida: Proprietário não encontrado."));

        if (files != null && files.size() > 2) {
            throw new IllegalArgumentException("É permitido enviar no máximo 2 fotos por carro.");
        }

        proprietario.criaGaragem();
        entusiastaRepository.save(proprietario);

        Carro carro = new Carro();
        mapearRegistroDtoParaEntidade(carro, dto);
        carro.setColecionador(proprietario.getColecionador());

        List<String> todasAsUrls = new ArrayList<>();
        if (files != null && !files.isEmpty()) {
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    String fileDownloadUri = fileStorageService.salvarArquivo(file);
                    todasAsUrls.add(fileDownloadUri);
                }
            }
        }
        carro.setFotosCarro(todasAsUrls);
        Carro carroSalvo = carroRepository.save(carro);
        return mapearEntidadeParaResponse(carroSalvo, proprietario.getNome());
    }

    public List<CarroRespostaDTO> listarCarrosDoColecionador(UUID entusiastaId) {
        Entusiasta proprietario = entusiastaRepository.findById(entusiastaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão inválida."));

        if (proprietario.getColecionador() == null) {
            return List.of();
        }

        return proprietario.getColecionador().getCarros().stream()
                .map(carro -> mapearEntidadeParaResponse(carro, proprietario.getNome()))
                .collect(Collectors.toList());
    }

    public CarroRespostaDTO buscarCarroPorId(UUID carroId, UUID entusiastaId) {
        Carro carro = verificarPropriedadeDoCarro(carroId, entusiastaId);
        return mapearEntidadeParaResponse(carro, carro.getColecionador().getEntusiasta().getNome());
    }

    // Método PUT: Substituição Completa
    @Transactional
    public CarroRespostaDTO substituirCarro(UUID carroId, CarroRegistroDTO dto, UUID entusiastaId) {
        Carro carro = verificarPropriedadeDoCarro(carroId, entusiastaId);
        // Verifica se a nova placa não pertence a outro carro
        if (!carro.getPlaca().equals(dto.placa()) && carroRepository.existsByPlaca(dto.placa())) {
            throw new PlacaJaCadastradaException("A nova placa informada já está registrada em outro veículo.");
        }

        mapearRegistroDtoParaEntidade(carro, dto);
        Carro carroAtualizado = carroRepository.save(carro);

        return mapearEntidadeParaResponse(carroAtualizado, carroAtualizado.getColecionador().getEntusiasta().getNome());
    }

    // Método PATCH: Atualização Parcial
    @Transactional
    public CarroRespostaDTO atualizarCarroCondicional(UUID carroId, CarroPatchDTO dto, UUID entusiastaId) {
        Carro carro = verificarPropriedadeDoCarro(carroId, entusiastaId);

        if (dto.placa() != null && !dto.placa().isBlank() && !carro.getPlaca().equals(dto.placa())) {
            if (carroRepository.existsByPlaca(dto.placa())) {
                throw new PlacaJaCadastradaException("A nova placa informada já está registrada em outro veículo.");
            }
            carro.setPlaca(dto.placa());
        }

        if (dto.marca() != null && !dto.marca().isBlank()) carro.setMarca(dto.marca());
        if (dto.modelo() != null && !dto.modelo().isBlank()) carro.setModelo(dto.modelo());
        if (dto.carroceria() != null) carro.setCarroceria(dto.carroceria());
        if (dto.ano() != null) carro.setAno(dto.ano());
        if (dto.cor() != null && !dto.cor().isBlank()) carro.setCor(dto.cor());
        if (dto.quantidadeCilindros() != null) carro.setQuantidadeCilindros(dto.quantidadeCilindros());
        if (dto.motor() != null) carro.setMotor(dto.motor());
        if (dto.litragemMotor() != null) carro.setLitragemMotor(dto.litragemMotor());
        if (dto.injecao() != null) carro.setInjecao(dto.injecao());
        if (dto.alimentacao() != null) carro.setAlimentacao(dto.alimentacao());
        if (dto.combustivel() != null) carro.setCombustivel(dto.combustivel());
        if (dto.cambio() != null) carro.setCambio(dto.cambio());
        if (dto.tracao() != null) carro.setTracao(dto.tracao());
        if (dto.suspencao() != null) carro.setSuspencao(dto.suspencao());
        if (dto.freio() != null) carro.setFreio(dto.freio());
        if (dto.direcao() != null) carro.setDirecao(dto.direcao());
        if (dto.roda() != null) carro.setRoda(dto.roda());
        if (dto.aro() != null) carro.setAro(dto.aro());
        if (dto.numeroPortas() != null) carro.setNumeroPortas(dto.numeroPortas());
        if (dto.numeroAssentos() != null) carro.setNumeroAssentos(dto.numeroAssentos());
        if (dto.materialRevestimentoInterno() != null) carro.setMaterialRevestimentoInterno(dto.materialRevestimentoInterno());
        if (dto.vidrosEletricos() != null) carro.setVidrosEletricos(dto.vidrosEletricos());
        if (dto.som() != null) carro.setSom(dto.som());
        if (dto.arCondicionado() != null) carro.setArCondicionado(dto.arCondicionado());
        if (dto.tetoSolar() != null) carro.setTetoSolar(dto.tetoSolar());
        if (dto.fotosCarro() != null) {
            carro.setFotosCarro(dto.fotosCarro());
        }

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

        proprietario.verificaExcluiGaragem();
        entusiastaRepository.save(proprietario);
    }

    //metodos auxiliares
    private Carro verificarPropriedadeDoCarro(UUID carroId, UUID entusiastaId) {
        Carro carro = carroRepository.findById(carroId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carro não encontrado."));

        if (!carro.getColecionador().getId().equals(entusiastaId)) {
            throw new PerfilIncompativelException("Acesso Negado: Este veículo não pertence à sua coleção.");
        }
        return carro;
    }

    private void mapearRegistroDtoParaEntidade(Carro carro, CarroRegistroDTO dto) {
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
    }

    private CarroRespostaDTO mapearEntidadeParaResponse(Carro carro, String nomeProprietario) {
        return new CarroRespostaDTO(
                carro.getId(),
                carro.getMarca(),
                carro.getModelo(),
                carro.getPlaca(),
                nomeProprietario
        );
    }
}