package carshub.com.br.backend;

import org.junit.platform.suite.api.SelectPackages;
import org.junit.platform.suite.api.Suite;

/**
 * SUITE COMPLETA: roda TODOS os testes do projeto (unitarios + integracao)
 * quando voce da play nesta classe pela IDE (IntelliJ: botao verde ao lado do
 * nome da classe).
 *
 * Nome SEM o sufixo "Test" de proposito: o Maven Surefire nao executa esta
 * classe automaticamente (evita rodar a suíte duas vezes em "mvn test").
 *
 * PRE-REQUISITOS: Docker Desktop com o MySQL no ar ("docker compose up -d db")
 * e o script espacial aplicado uma vez (ver Deepseek/TESTES-GUIA.md).
 */
@Suite
@SelectPackages("carshub.com.br.backend")
public class SuiteCompleta {
}
