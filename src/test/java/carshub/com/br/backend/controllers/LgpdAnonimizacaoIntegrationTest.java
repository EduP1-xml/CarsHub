package carshub.com.br.backend.controllers;

import carshub.com.br.backend.models.entities.Carro;
import carshub.com.br.backend.models.entities.Entusiasta;
import carshub.com.br.backend.models.entities.Mecanico;
import carshub.com.br.backend.models.entities.servicos.ServicoMecanico;
import carshub.com.br.backend.models.enums.servicos.StatusServico;
import carshub.com.br.backend.tasks.LgpdAnonimizacaoTask;
import carshub.com.br.backend.testsupport.TesteIntegracaoBase;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.util.List;

import static carshub.com.br.backend.models.enums.mecanicos.ServicosOfertados.TROCA_DE_OLEO;
import static carshub.com.br.backend.testsupport.FabricaDeDados.LATITUDE_PADRAO;
import static carshub.com.br.backend.testsupport.FabricaDeDados.LONGITUDE_PADRAO;
import static carshub.com.br.backend.testsupport.FabricaDeDados.cnpjUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.cpfUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.criarCarro;
import static carshub.com.br.backend.testsupport.FabricaDeDados.criarChamadoMecanico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.criarColecionador;
import static carshub.com.br.backend.testsupport.FabricaDeDados.criarMecanico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.emailUnico;
import static carshub.com.br.backend.testsupport.FabricaDeDados.placaUnico;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * SPRINT 6 (11.11 — LGPD): a LgpdAnonimizacaoTask apaga os COMENTARIOS de
 * avaliacao (texto livre) dos servicos FINALIZADOS ha mais que a retencao
 * configurada e preserva os recentes. O @Scheduled fica desligado no profile
 * de teste; o metodo da task e chamado diretamente.
 * Banco MySQL real: os dados criados PERMANECEM gravados apos o teste.
 */
class LgpdAnonimizacaoIntegrationTest extends TesteIntegracaoBase {

    @Autowired
    protected LgpdAnonimizacaoTask taskLgpd;

    @Test
    @DisplayName("comentarios de servicos FINALIZADOS antigos sao anonimizados; recentes nao")
    void anonimizaComentariosDeServicosAntigos() {
        Entusiasta colecionador = criarColecionador(repositorioEntusiasta, emailUnico("lgpd"), cpfUnico());
        Carro carro = criarCarro(repositorioCarro, colecionador.getColecionador(), placaUnico());
        Mecanico mecanico = criarMecanico(repositorioMecanico, emailUnico("lgpdmec"), cnpjUnico(),
                List.of(TROCA_DE_OLEO), LATITUDE_PADRAO, LONGITUDE_PADRAO);

        ServicoMecanico antigo = criarChamadoMecanico(repositorioServicoMecanico, colecionador, carro,
                mecanico, StatusServico.FINALIZADO, LATITUDE_PADRAO, LONGITUDE_PADRAO);
        antigo.setDataHoraFinalizacao(LocalDateTime.now().minusDays(400));
        antigo.setComentarioAvaliacaoEntusiasta("Comentario antigo do entusiasta");
        antigo.setComentarioAvaliacaoMecanico("Comentario antigo do mecanico");
        repositorioServicoMecanico.saveAndFlush(antigo);

        ServicoMecanico recente = criarChamadoMecanico(repositorioServicoMecanico, colecionador, carro,
                mecanico, StatusServico.FINALIZADO, LATITUDE_PADRAO, LONGITUDE_PADRAO);
        recente.setDataHoraFinalizacao(LocalDateTime.now());
        recente.setComentarioAvaliacaoEntusiasta("Comentario recente");
        repositorioServicoMecanico.saveAndFlush(recente);

        taskLgpd.anonimizarComentariosDeServicosAntigos();

        ServicoMecanico antigoApos = repositorioServicoMecanico.findById(antigo.getId()).orElseThrow();
        assertThat(antigoApos.getComentarioAvaliacaoEntusiasta()).isNull();
        assertThat(antigoApos.getComentarioAvaliacaoMecanico()).isNull();

        ServicoMecanico recenteApos = repositorioServicoMecanico.findById(recente.getId()).orElseThrow();
        assertThat(recenteApos.getComentarioAvaliacaoEntusiasta()).isEqualTo("Comentario recente");
    }
}
