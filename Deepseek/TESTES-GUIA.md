# Testes automatizados do CarHub — guia de execucao

Este arquivo e um artefato AUXILIAR (pode ser apagado depois). Os testes em si ficam
em `src/test/java`, espelhando os pacotes de `src/main/java`.

## 1. Pre-requisitos (obrigatorios ANTES de rodar os testes)

1. **Docker Desktop rodando** e o banco no ar:
   ```
   docker compose up -d db
   ```
2. **Script espacial aplicado UMA vez** (depois que o Hibernate criar as tabelas):
   ```
   Get-Content .\db\servico-localizacao-espacial.sql -Raw | docker exec -i carshub-db mysql -u root -proot carshub
   ```
   Sem ele, os testes que usam o painel `/disponiveis` (busca ST_Within) FALHAM —
   e de proposito: e exatamente o que o `VerificadorLocalizacaoEspacial` avisa no boot.
3. **JDK 21** (`JAVA_HOME`):
   ```
   $env:JAVA_HOME = "$env:USERPROFILE\.jdks\azul-21.0.12.1"
   ```
4. Rede para o Maven baixar as dependencias de teste na primeira execucao.

## 2. Como rodar

```powershell
# TUDO (unitarios + integracao) — exige banco no ar
.\mvnw.cmd test

# So os UNITARIOS (nao precisam de banco)
.\mvnw.cmd test "-Dtest=CalculadoraMatchTest,DistanciaGeograficaTest,GeradorChaveIdempotenciaTest,PaginaRespostaDTOTest,ControleCooldownTest,SolicitacaoExpiradaTaskTest,PropostaExpiradaTaskTest,PainelServicoServiceTest"

# So uma classe de integracao
.\mvnw.cmd test "-Dtest=ServicoMecanicoIntegrationTest"
```

## 3. Persistencia dos dados (decisao do time)

Os testes de integracao rodam contra o **MySQL real de localhost:3307** (sem
Testcontainers, sem rollback, sem limpeza). TUDO o que for criado PERMANECE no
banco: usuarios, carros, chamados, propostas, eventos, inscricoes, fotos em
`uploads/`. Cada execucao usa emails/CPF/CNPJ/placas unicos, entao pode rodar
quantas vezes quiser sem colisao — mas os registros acumulam de proposito.

## 4. Mapa de cobertura

| Arquivo de teste | Cobre | Tipo |
|---|---|---|
| CalculadoraMatchTest | bayesiana (0/1/20 aval.), notaMatch 0..100, limites | unit |
| DistanciaGeograficaTest | Haversine 2,65 km, caixa, WKT, polos | unit |
| GeradorChaveIdempotenciaTest | SHA-256, ordem de servicos, variacoes | unit |
| PaginaRespostaDTOTest | envelope, default 5, bordas | unit |
| ControleCooldownTest | 5 min / 1 min, mensagens, registro | unit |
| SolicitacaoExpiradaTaskTest | limpeza de SOLICITADO > 3 h | unit |
| PropostaExpiradaTaskTest | delegacao aos dois services | unit |
| PainelServicoServiceTest | roteamento do ranking M4, 404 | unit |
| ServicoMecanicoIntegrationTest | E2E chamado->FINALIZADO, 409, 422, 403, 404 | integracao |
| ServicoGuinchoIntegrationTest | E2E guincho, cota cheia 422, 409 | integracao |
| MatchRankingIntegrationTest | ranking M4 (ordem, cota cheia, neutro 4.0), N14 (11a proposta), rejeicao | integracao |
| CadastroLoginIntegrationTest | cadastro 3 etapas, login, 409/400/401/422 | integracao |
| CarroIntegrationTest | CRUD, virada para colecionador, 409/403 | integracao |
| EventoInscricaoIntegrationTest | evento, catalogo, inscricao, lotacao, dashboard | integracao |

Mockados nos testes de integracao: `EmailService` (sem e-mail real) e
`GeocodificacaoService` (sem chamadas ao Google). Agendamento desligado pelo
profile `test` (`application-test.yaml`).

## 5. Observacoes

- `BackendApplicationTests.contextLoads` (ja existia) tambem exige o banco no ar.
- Rodar `mvn test` com a app do Spring Boot aberta na porta 8080 NAO conflita
  (MockMvc nao abre porta), mas o banco precisa estar de pe.
- Se um teste de cooldown falhar sozinho, confira a hora do sistema: as janelas
  de 5 min/1 min usam `LocalDateTime.now()`.