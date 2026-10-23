package carshub.com.br.backend.testsupport;

import carshub.com.br.backend.models.entities.Carro;
import carshub.com.br.backend.models.entities.Colecionador;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.Guincho;
import carshub.com.br.backend.models.entities.Mecanico;
import carshub.com.br.backend.models.entities.OrganizadorEvento;
import carshub.com.br.backend.models.entities.Usuario;
import carshub.com.br.backend.models.entities.servicos.ServicoGuincho;
import carshub.com.br.backend.models.entities.servicos.ServicoMecanico;
import carshub.com.br.backend.models.enums.carros.TipoAlimentacao;
import carshub.com.br.backend.models.enums.carros.TipoCambio;
import carshub.com.br.backend.models.enums.carros.TipoCarroceria;
import carshub.com.br.backend.models.enums.carros.TipoCombustivel;
import carshub.com.br.backend.models.enums.carros.TipoDirecao;
import carshub.com.br.backend.models.enums.carros.TipoFreio;
import carshub.com.br.backend.models.enums.carros.TipoInjecao;
import carshub.com.br.backend.models.enums.carros.TipoMaterialRevestimentoInterno;
import carshub.com.br.backend.models.enums.carros.TipoMotor;
import carshub.com.br.backend.models.enums.carros.TipoRoda;
import carshub.com.br.backend.models.enums.carros.TipoSuspensao;
import carshub.com.br.backend.models.enums.carros.TipoTracao;
import carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados;
import carshub.com.br.backend.models.enums.servicos.StatusServico;
import carshub.com.br.backend.models.enums.tiposUsuarios.TipoPrestadorServico;
import carshub.com.br.backend.models.enums.tiposUsuarios.TipoUsuario;
import carshub.com.br.backend.repositories.CarroRepository;
import carshub.com.br.backend.repositories.EntusiastaRepository;
import carshub.com.br.backend.repositories.GuinchoRepository;
import carshub.com.br.backend.repositories.MecanicoRepository;
import carshub.com.br.backend.repositories.OrganizadorEventoRepository;
import carshub.com.br.backend.repositories.servicos.ServicoGuinchoRepository;
import carshub.com.br.backend.repositories.servicos.ServicoMecanicoRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Fixtures gravadas DIRETO no banco (via repositorios), usadas apenas na
 * PREPARACAO dos cenarios. O comportamento sob teste sempre passa pelos
 * endpoints reais. Nada e limpo apos o teste: os dados permanecem no banco.
 */
public final class FabricaDeDados {

    public static final String SENHA_PADRAO = "Senha@123";
    public static final double LATITUDE_PADRAO = -23.5505;
    public static final double LONGITUDE_PADRAO = -46.6333;

    private FabricaDeDados() {
    }

    public static String sufixoUnico() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    public static String emailUnico(String prefixo) {
        String sufixo = sufixoUnico();
        String email = prefixo + "-" + sufixo + "@teste.com";
        if (email.length() <= 45) {
            return email;
        }
        return prefixo.substring(0, Math.max(1, 45 - sufixo.length() - 11)) + "-" + sufixo + "@teste.com";
    }

    public static String cpfUnico() {
        return String.format("%011d", (long) (Math.random() * 100_000_000_000L));
    }

    public static String cnpjUnico() {
        return String.format("%014d", (long) (Math.random() * 100_000_000_000_000L));
    }

    public static String placaUnico() {
        return String.format("%07d", (int) (Math.random() * 10_000_000));
    }

    private static void preencherUsuario(Usuario usuario, String login, String nome, TipoUsuario tipoUsuario) {
        usuario.setLogin(login);
        usuario.setSenha(SENHA_PADRAO);
        usuario.setNome(nome);
        usuario.setDataNascimento(LocalDate.of(1990, 1, 15));
        usuario.setTelefone("11999999999");
        usuario.setTipoUsuario(tipoUsuario);
        usuario.setAtivo(true);
    }

    /** Entusiasta que ja e colecionador (garagem criada; id compartilhado com o Colecionador). */
    public static Entusiasta criarColecionador(EntusiastaRepository repositorio, String login, String cpf) {
        Entusiasta entusiasta = new Entusiasta();
        preencherUsuario(entusiasta, login, "Colecionador " + login, TipoUsuario.COLECIONADOR);
        entusiasta.setCpf(cpf);
        entusiasta.criaGaragem();
        return repositorio.saveAndFlush(entusiasta);
    }

    public static Mecanico criarMecanico(MecanicoRepository repositorio, String login, String cnpj,
                                         List<ServicosOfertados> servicos, Double latitude, Double longitude) {
        Mecanico mecanico = new Mecanico();
        preencherUsuario(mecanico, login, "Mecanico " + login, TipoUsuario.PRESTADOR_SERVICO);
        mecanico.setTipoPrestadorServico(TipoPrestadorServico.MECANICO);
        mecanico.setCnpj(cnpj);
        mecanico.setServicos(servicos);
        mecanico.setLatitude(latitude);
        mecanico.setLongitude(longitude);
        return repositorio.saveAndFlush(mecanico);
    }

    public static Guincho criarGuincho(GuinchoRepository repositorio, String login, String cnpj, String placa,
                                       Double latitude, Double longitude) {
        Guincho guincho = new Guincho();
        preencherUsuario(guincho, login, "Guincho " + login, TipoUsuario.PRESTADOR_SERVICO);
        guincho.setTipoPrestadorServico(TipoPrestadorServico.GUINCHO);
        guincho.setCnpj(cnpj);
        guincho.setPlaca(placa);
        guincho.setLatitude(latitude);
        guincho.setLongitude(longitude);
        return repositorio.saveAndFlush(guincho);
    }

    public static OrganizadorEvento criarOrganizador(OrganizadorEventoRepository repositorio, String login, String cnpj) {
        OrganizadorEvento organizador = new OrganizadorEvento();
        preencherUsuario(organizador, login, "Organizador " + login, TipoUsuario.ORGANIZADOR_EVENTO);
        organizador.setCnpj(cnpj);
        return repositorio.saveAndFlush(organizador);
    }

    public static Carro criarCarro(CarroRepository repositorio, Colecionador colecionador, String placa) {
        Carro carro = new Carro();
        carro.setMarca("Volkswagen");
        carro.setModelo("Gol");
        carro.setCarroceria(TipoCarroceria.HATCH);
        carro.setAno(2015);
        carro.setCor("Prata");
        carro.setQuantidadeCilindros(4);
        carro.setMotor(TipoMotor.LINHA);
        carro.setLitragemMotor(new BigDecimal("1.6"));
        carro.setInjecao(TipoInjecao.ELETRONICA);
        carro.setAlimentacao(TipoAlimentacao.ASPIRADO);
        carro.setCombustivel(TipoCombustivel.FLEX);
        carro.setCambio(TipoCambio.MANUAL);
        carro.setTracao(TipoTracao.DIANTEIRA);
        carro.setSuspencao(TipoSuspensao.FIXA);
        carro.setFreio(TipoFreio.DISCO);
        carro.setDirecao(TipoDirecao.HIDRAULICA);
        carro.setRoda(TipoRoda.LIGA_LEVE);
        carro.setAro(15);
        carro.setNumeroPortas(4);
        carro.setNumeroAssentos(5);
        carro.setMaterialRevestimentoInterno(TipoMaterialRevestimentoInterno.TECIDO);
        carro.setPlaca(placa);
        carro.setVidrosEletricos(true);
        carro.setSom(true);
        carro.setArCondicionado(true);
        carro.setTetoSolar(false);
        carro.setFotosCarro(List.of());
        carro.setColecionador(colecionador);
        return repositorio.saveAndFlush(carro);
    }

    /** Chamado mecanico semeado direto no banco (cenarios de cota/ranking, sem cooldown de API). */
    public static ServicoMecanico criarChamadoMecanico(ServicoMecanicoRepository repositorio, Entusiasta entusiasta,
                                                       Carro carro, Mecanico mecanico, StatusServico status,
                                                       double latitude, double longitude) {
        ServicoMecanico servico = new ServicoMecanico();
        servico.setEntusiasta(entusiasta);
        servico.setCarro(carro);
        servico.setMecanico(mecanico);
        servico.setStatus(status);
        servico.setServicos(List.of(ServicosOfertados.TROCA_DE_OLEO));
        servico.setVersao(0L);
        servico.setQuantidadePropostas(0);
        servico.setLatitudeAtualEntusiasta(latitude);
        servico.setLongitudeAtualEntusiasta(longitude);
        servico.setEnderecoAtualEntusiasta("Rua Teste, 100 - Centro");
        servico.setDataHoraSolicitacao(LocalDateTime.now());
        if (status == StatusServico.PENDENTE) {
            servico.setDataHoraPendente(LocalDateTime.now());
        }
        return repositorio.saveAndFlush(servico);
    }

    /** Chamado de guincho semeado direto no banco (cenarios de cota). */
    public static ServicoGuincho criarChamadoGuincho(ServicoGuinchoRepository repositorio, Entusiasta entusiasta,
                                                     Carro carro, Guincho guincho, StatusServico status,
                                                     double latitude, double longitude) {
        ServicoGuincho servico = new ServicoGuincho();
        servico.setEntusiasta(entusiasta);
        servico.setCarro(carro);
        servico.setGuincho(guincho);
        servico.setStatus(status);
        servico.setVersao(0L);
        servico.setQuantidadePropostas(0);
        servico.setLatitudeAtualEntusiasta(latitude);
        servico.setLongitudeAtualEntusiasta(longitude);
        servico.setEnderecoAtualEntusiasta("Rua Teste, 100 - Centro");
        servico.setDataHoraSolicitacao(LocalDateTime.now());
        if (status == StatusServico.PENDENTE) {
            servico.setDataHoraPendente(LocalDateTime.now());
        }
        return repositorio.saveAndFlush(servico);
    }
}