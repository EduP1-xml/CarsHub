package carshub.com.br.backend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Confere, na subida, se o script de localizacao espacial foi aplicado.
 *
 * A coluna POINT gerada e o SPATIAL INDEX, que nao tem como ser criados pelo Hibernate.
 * Essa classe apenas LE o information_schema e avisa alto quando alguem dropar o banco e
 * esquecer o script do SPATIAL INDEX — sem isso, o painel falharia
 * em runtime com um erro de sintaxe SQL.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class VerificadorLocalizacaoEspacial implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(ApplicationArguments args) {
        verificar("ServicoMecanico", "localizacao_ServicoMecanico", "idx_servicoMecanico_localizacao");
        verificar("ServicoGuincho", "localizacao_ServicoGuincho", "idx_servicoGuincho_localizacao");
    }

    private void verificar(String tabela, String coluna, String indice) {
        Integer colunas = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.COLUMNS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?
                """, Integer.class, tabela, coluna);

        Integer indices = jdbcTemplate.queryForObject("""
                SELECT COUNT(*) FROM information_schema.STATISTICS
                 WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND INDEX_NAME = ?
                """, Integer.class, tabela, indice);

        if (colunas == null || colunas == 0 || indices == null || indices == 0) {
            log.error("""
                    ==================================================================
                    LOCALIZACAO ESPACIAL AUSENTE no banco: {}
                    Rode:  db/servico-localizacao-espacial.sql
                    (esperado: coluna {} e indice {})
                    ==================================================================""",
                    tabela, coluna, indice);
            return;
        }

        log.info("Localização espacial OK em {} (coluna {} + índice {}).", tabela, coluna, indice);
    }
}