package carshub.com.br.backend.services;

import carshub.com.br.backend.exceptions.PerfilIncompativelException;
import carshub.com.br.backend.exceptions.PlacaJaCadastradaException;
import carshub.com.br.backend.exceptions.RecursoNaoEncontradoException;
import carshub.com.br.backend.models.dtos.PaginaRespostaDTO;
import carshub.com.br.backend.models.dtos.carros.CarroGaragemProjecaoDTO;
import carshub.com.br.backend.models.dtos.carros.CarroPatchDTO;
import carshub.com.br.backend.models.dtos.carros.CarroRegistroDTO;
import carshub.com.br.backend.models.dtos.carros.CarroRespostaDTO;
import carshub.com.br.backend.models.entities.Carro;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.FiltroCarro;
import carshub.com.br.backend.models.entities.MembroClube;
import carshub.com.br.backend.repositories.CarroRepository;
import carshub.com.br.backend.repositories.EntusiastaRepository;
import carshub.com.br.backend.repositories.MembroClubeRepository;
import carshub.com.br.backend.services.file.FileStorageService;
import carshub.com.br.backend.services.clubes.CarregadorFiltroCarro;
import carshub.com.br.backend.services.clubes.CompatibilidadeFiltro;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CarroService {

    private final CarroRepository carroRepository;
    private final EntusiastaRepository entusiastaRepository;
    private final FileStorageService fileStorageService;
    private final MembroClubeRepository membroClubeRepository;
    private final CarregadorFiltroCarro carregadorFiltroCarro;


    // POST: Criar Carro
    @Transactional
    public CarroRespostaDTO registrarCarro(CarroRegistroDTO dto, List<MultipartFile> arquivos, UUID entusiastaId) {
        if (carroRepository.existsByPlaca(dto.placa())) {
            throw new PlacaJaCadastradaException("Esta placa já está registrada em outro veículo no CarsHub.");
        }
        Entusiasta proprietario = entusiastaRepository.findById(entusiastaId).orElseThrow(() -> new RecursoNaoEncontradoException("Sessão inválida: Proprietário não encontrado."));

        if (arquivos != null && arquivos.size() > 2) {
            throw new IllegalArgumentException("É permitido enviar no máximo 2 fotos por carro.");
        }

        proprietario.criaGaragem();
        entusiastaRepository.save(proprietario);

        Carro carro = new Carro();
        mapearRegistroDtoParaEntidade(carro, dto);
        carro.setColecionador(proprietario.getColecionador());

        List<String> todasAsUrls = new ArrayList<>();
        if (arquivos != null && !arquivos.isEmpty()) {
            for (MultipartFile file : arquivos) {
                if (file != null && !file.isEmpty()) {
                    String fileDownloadUri = fileStorageService.salvarArquivo(file, "colecionador", entusiastaId);
                    todasAsUrls.add(fileDownloadUri);
                }
            }
        }
        // F13: urlFotoCarro deixou de ser obrigatoria — quando nao ha arquivos
        // multipart, valem as URLs enviadas no JSON (compatibilidade).
        if (todasAsUrls.isEmpty() && dto.urlFotoCarro() != null) {
            todasAsUrls.addAll(dto.urlFotoCarro());
        }
        carro.setFotosCarro(todasAsUrls);
        Carro carroSalvo = carroRepository.save(carro);
        return mapearEntidadeParaResponse(carroSalvo, proprietario.getNome());
    }

    /**
     * Garagem paginada NATIVA (Page no SQL). Deixou de carregar a
     * colecao lazy inteira do colecionador. Garagem vazia devolve Page vazio.
     * AJUSTE 1.1b: projecao de escalares — o SQL devolve o CarroGaragemProjecaoDTO
     * e o service monta o CarroRespostaDTO (fotos vazias na listagem).
     */
    @Transactional(readOnly = true)
    public Page<CarroRespostaDTO> listarCarrosDoColecionador(UUID entusiastaId, int pagina, int tamanho) {
        Entusiasta proprietario = entusiastaRepository.findById(entusiastaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Sessão inválida."));

        if (proprietario.getColecionador() == null) {
            return Page.empty(PaginaRespostaDTO.pageRequest(pagina, tamanho));
        }

        return carroRepository.projetarGaragemDoColecionador(
                        proprietario.getColecionador().getId(), PaginaRespostaDTO.pageRequest(pagina, tamanho))
                .map(CarroService::mapearProjecaoParaResponse);
    }

    // F13: o detalhe devolve TODOS os campos do carro, inclusive as fotos.
    public CarroRespostaDTO buscarCarroPorId(UUID carroId, UUID entusiastaId) {
        Carro carro = verificarPropriedadeDoCarro(carroId, entusiastaId);
        return mapearEntidadeParaResponse(carro, carro.getColecionador().getEntusiasta().getNome());
    }

    // Método PUT: Substituição Completa (AVISO 7.9: multipart com fotos opcionais)
    @Transactional
    public CarroRespostaDTO substituirCarro(UUID carroId, CarroRegistroDTO dto, List<MultipartFile> arquivos, UUID entusiastaId) {
        Carro carro = verificarPropriedadeDoCarro(carroId, entusiastaId);
        // Verifica se a nova placa não pertence a outro carro
        if (!carro.getPlaca().equals(dto.placa()) && carroRepository.existsByPlaca(dto.placa())) {
            throw new PlacaJaCadastradaException("A nova placa informada já está registrada em outro veículo.");
        }

        if (arquivos != null && arquivos.size() > 2) {
            throw new IllegalArgumentException("É permitido enviar no máximo 2 fotos por carro.");
        }

        mapearRegistroDtoParaEntidade(carro, dto);

        // Fotos: arquivos multipart substituem todas; sem arquivos, vale a
        // lista de URLs do JSON; sem os dois, as fotos atuais sao mantidas.
        if (arquivos != null && !arquivos.isEmpty()) {
            List<String> urls = new ArrayList<>();
            for (MultipartFile arquivo : arquivos) {
                if (arquivo != null && !arquivo.isEmpty()) {
                    urls.add(fileStorageService.salvarArquivo(arquivo, "colecionador", entusiastaId));
                }
            }
            carro.setFotosCarro(urls);
        }
        else if (dto.urlFotoCarro() != null) {
            carro.setFotosCarro(dto.urlFotoCarro());
        }

        Carro carroAtualizado = carroRepository.save(carro);
        List<String> clubesDesvinculados = desvincularDeClubesIncompativeis(carroAtualizado);

        return mapearEntidadeParaResponse(carroAtualizado, carroAtualizado.getColecionador().getEntusiasta().getNome())
                .comClubesDesvinculados(clubesDesvinculados);
    }

    // Método PATCH: Atualização Parcial (AVISO 7.8: fotos trocadas slot a slot)
    @Transactional
    public CarroRespostaDTO atualizarCarroCondicional(UUID carroId, CarroPatchDTO dto,
                                                      MultipartFile foto1, MultipartFile foto2, UUID entusiastaId) {
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
        if (dto.suspensao() != null) carro.setSuspensao(dto.suspensao());
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

        // AVISO 7.8: fotos trocadas uma a uma (foto1/foto2), validando o limite de 2.
        List<String> fotos = new ArrayList<>(carro.getFotosCarro() != null ? carro.getFotosCarro() : List.of());
        aplicarFotoNoSlot(fotos, 1, foto1, entusiastaId);
        aplicarFotoNoSlot(fotos, 2, foto2, entusiastaId);
        if (fotos.size() > 2) {
            throw new IllegalArgumentException("É permitido enviar no máximo 2 fotos por carro.");
        }
        carro.setFotosCarro(fotos);

        Carro carroAtualizado = carroRepository.save(carro);
        List<String> clubesDesvinculados = desvincularDeClubesIncompativeis(carroAtualizado);

        return mapearEntidadeParaResponse(carroAtualizado, carroAtualizado.getColecionador().getEntusiasta().getNome())
                .comClubesDesvinculados(clubesDesvinculados);
    }

    // DELETE: Remover carro (com a mágica da revogação do título)
    @Transactional
    public void removerCarro(UUID carroId, UUID entusiastaId) {
        Carro carro = verificarPropriedadeDoCarro(carroId, entusiastaId);
        Entusiasta proprietario = carro.getColecionador().getEntusiasta();

        // CLUBES: os vinculos saem antes do DELETE (a FK de CarrosVinculados_Membro_Clube barraria a exclusao).
        for (Object[] vinculo : membroClubeRepository.buscarVinculosDoCarro(carroId)) {
            ((MembroClube) vinculo[0]).getCarrosVinculados().removeIf(vinculado -> vinculado.getId().equals(carroId));
        }

        proprietario.getColecionador().getCarros().remove(carro); // desassocia ANTES de deletar
        carroRepository.delete(carro);
        carroRepository.flush(); // garante que o DELETE do carro sai antes do flush final

        proprietario.verificaExcluiGaragem();
        entusiastaRepository.save(proprietario);
    }

    //metodos auxiliares

    /**
     * CLUBES: depois de editar o carro, reavalia os clubes em que ele esta
     * vinculado (filtros carregados em lote) e o desvincula dos clubes cujo
     * gosto ele nao atende mais. Devolve os nomes desses clubes.
     */
    private List<String> desvincularDeClubesIncompativeis(Carro carro) {
        List<Object[]> vinculos = membroClubeRepository.buscarVinculosDoCarro(carro.getId());
        if (vinculos.isEmpty()) {
            return List.of();
        }

        Map<UUID, FiltroCarro> filtros = carregadorFiltroCarro.carregar(
                vinculos.stream().map(vinculo -> (UUID) vinculo[1]).toList());

        List<String> clubesDesvinculados = new ArrayList<>();
        for (Object[] vinculo : vinculos) {
            if (!CompatibilidadeFiltro.carroAtende(filtros.get((UUID) vinculo[1]), carro)) {
                ((MembroClube) vinculo[0]).getCarrosVinculados()
                        .removeIf(vinculado -> vinculado.getId().equals(carro.getId()));
                clubesDesvinculados.add((String) vinculo[2]);
            }
        }
        return clubesDesvinculados;
    }

    private Carro verificarPropriedadeDoCarro(UUID carroId, UUID entusiastaId) {
        Carro carro = carroRepository.findById(carroId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Carro não encontrado."));

        if (!carro.getColecionador().getId().equals(entusiastaId)) {
            throw new PerfilIncompativelException("Acesso Negado: Este veículo não pertence à sua coleção.");
        }
        return carro;
    }

    /**
     * AVISO 7.8: substitui a foto do SLOT indicado (1 ou 2). Slots faltantes no
     * meio sao preenchidos com null (as posicoes importam: foto 1 = indice 0,
     * foto 2 = indice 1). O limite de 2 e validado pelo chamador.
     */
    private void aplicarFotoNoSlot(List<String> fotos, int slot, MultipartFile arquivo, UUID idColecionador) {
        if (arquivo == null || arquivo.isEmpty()) {
            return;
        }

        String uri = fileStorageService.salvarArquivo(arquivo, "colecionador", idColecionador);
        int indice = slot - 1;

        while (fotos.size() < indice) {
            fotos.add(null);
        }
        if (fotos.size() == indice) {
            fotos.add(uri);
        }
        else {
            fotos.set(indice, uri);
        }
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
        carro.setSuspensao(dto.suspensao());
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

    private static CarroRespostaDTO mapearProjecaoParaResponse(CarroGaragemProjecaoDTO projecao) {
        return mapearProjecaoParaResponse(projecao, projecao.placa(), List.of());
    }

    /**
     * CLUBES: projecao -> resposta com placa e fotos informadas pelo chamador
     * (a aba Carros do clube omite a placa e completa as fotos em lote).
     */
    public static CarroRespostaDTO mapearProjecaoParaResponse(CarroGaragemProjecaoDTO projecao, String placa,
                                                              List<String> fotos) {
        return new CarroRespostaDTO(
                projecao.idCarro(),
                projecao.marca(),
                projecao.modelo(),
                placa,
                projecao.nomeProprietario(),
                projecao.carroceria(),
                projecao.ano(),
                projecao.cor(),
                projecao.quantidadeCilindros(),
                projecao.motor(),
                projecao.litragemMotor(),
                projecao.injecao(),
                projecao.alimentacao(),
                projecao.combustivel(),
                projecao.cambio(),
                projecao.tracao(),
                projecao.suspensao(),
                projecao.freio(),
                projecao.direcao(),
                projecao.roda(),
                projecao.aro(),
                projecao.numeroPortas(),
                projecao.numeroAssentos(),
                projecao.materialRevestimentoInterno(),
                projecao.vidrosEletricos(),
                projecao.som(),
                projecao.arCondicionado(),
                projecao.tetoSolar(),
                fotos,
                List.of());
    }

    private CarroRespostaDTO mapearEntidadeParaResponse(Carro carro, String nomeProprietario) {
        return new CarroRespostaDTO(
                carro.getId(),
                carro.getMarca(),
                carro.getModelo(),
                carro.getPlaca(),
                nomeProprietario,
                carro.getCarroceria(),
                carro.getAno(),
                carro.getCor(),
                carro.getQuantidadeCilindros(),
                carro.getMotor(),
                carro.getLitragemMotor(),
                carro.getInjecao(),
                carro.getAlimentacao(),
                carro.getCombustivel(),
                carro.getCambio(),
                carro.getTracao(),
                carro.getSuspensao(),
                carro.getFreio(),
                carro.getDirecao(),
                carro.getRoda(),
                carro.getAro(),
                carro.getNumeroPortas(),
                carro.getNumeroAssentos(),
                carro.getMaterialRevestimentoInterno(),
                carro.getVidrosEletricos(),
                carro.getSom(),
                carro.getArCondicionado(),
                carro.getTetoSolar(),
                carro.getFotosCarro() != null ? List.copyOf(carro.getFotosCarro()) : List.of(),
                List.of()
        );
    }
}
