# Plano de ação — Densidade de valor visível

> **Feedback da banca:** "Densidade de valor visível: o primeiro minuto precisa mostrar o que a plataforma faz além de listar eventos e carros. Hoje isso é navegação, não falta de backend."

---

## 1. Leitura do feedback

A banca reconhece que o backend já entrega mais do que a interface mostra. O CarsHub tem **três pilares**:

1. **Garagem com histórico de manutenção** de cada clássico.
2. **Socorro e manutenção sob demanda** (mecânico e guincho), com um **ranking de compatibilidade** (match) que combina reputação e distância, além de evidência por foto no início e no fim de cada serviço e avaliação nos dois sentidos.
3. **Eventos** com inscrição, controle de vagas e dashboard do organizador.

Hoje quem abre o sistema só vê o terceiro pilar, e mesmo assim como uma lista simples. Os outros dois ficam a três ou cinco cliques de distância e só aparecem depois do login, do cadastro de um carro e da abertura de um chamado.

**Conclusão:** o problema está em **descoberta, roteamento e apresentação**. Não falta regra de negócio nem endpoint. O plano abaixo mexe principalmente no frontend. No backend entra apenas uma carga de dados de demonstração, sem regra nova.

---

## 2. Diagnóstico no código

### 2.1 O primeiro minuto hoje

| Momento | O que acontece | Onde |
|---|---|---|
| Abrir `http://localhost:5173/` | Redireciona direto para `/eventos` | [App.tsx](../Frontend/src/App.tsx) — `<Route path="/" element={<Navigate to="/eventos" />} />` |
| Primeira tela (convidado) | Título "Eventos" e uma pilha de cards. Nenhuma frase diz o que é o CarsHub | [Catalogo.tsx](../Frontend/src/pages/Catalogo/Catalogo.tsx) |
| Menu do convidado | Só "Eventos", "Entrar" e "Criar conta". Garagem e serviços não aparecem | [NavBar.tsx](../Frontend/src/components/NavBar.tsx) |
| Tela de login | "🚗 CarsHub — Entre com a sua conta", sem proposta de valor | [Login.tsx](../Frontend/src/pages/Login/Login.tsx) |
| Home do entusiasta/colecionador após o login | `/eventos`, a **mesma tela do convidado** | [AuthContext.tsx](../Frontend/src/context/AuthContext.tsx) — `rotaInicialParaTipo` |
| Home do organizador | Dashboard com 2 números e uma lista de histórico | [Dashboard.tsx](../Frontend/src/pages/Organizador/Dashboard.tsx) |
| Rodapé | "frontend React (Vite) · API em http://localhost:8080/api/carshub", texto técnico | [Layout.tsx](../Frontend/src/components/Layout.tsx) |

### 2.2 Diferenciais que já existem e estão escondidos

| Diferencial (já implementado) | Backend | Onde aparece hoje | Cliques a partir do login |
|---|---|---|---|
| Ranking de prestadores (60% reputação bayesiana + 40% distância) | `CalculadoraMatch`, `GET /colecionador/servicos/{id}/prestadores-compativeis` | Aba dentro de [ServicoDetalhe.tsx](../Frontend/src/pages/Servicos/ServicoDetalhe.tsx), só com um chamado aberto | 4 ou mais (exige carro + chamado) |
| Histórico de manutenção por carro | `GET /colecionador/carros/{id}/historico-manutencoes` | Botão "Histórico" no card da garagem | 2 ou 3 |
| Fotos de evidência (inicial imutável + final) | `foto-inicial`, `foto-final` | Seções do detalhe do serviço | 4 ou mais |
| Avaliação nos dois sentidos | `/avaliacao` (colecionador e prestador) | Detalhe do serviço finalizado | 4 ou mais |
| "Virada" de Entusiasta para Colecionador | Regra no `CarroService` | Só um toast depois do 1º carro | invisível |
| Chamados próximos com nota de match para o prestador | `GET /{mecanicos\|guinchos}/servicos/disponiveis` | Home do prestador (este perfil está bem resolvido) | 0 |

### 2.3 Atritos de navegação

- **Entusiasta não descobre os serviços.** "Meus serviços" só aparece para `COLECIONADOR` ([NavBar.tsx](../Frontend/src/components/NavBar.tsx)). Ao acessar `/servicos`, a guarda `ExigirPerfil` redireciona sem explicar nada ([App.tsx](../Frontend/src/App.tsx)).
- **Faltam atalhos entre os módulos.** [CarroDetalhe.tsx](../Frontend/src/pages/Garagem/CarroDetalhe.tsx) não tem "Pedir mecânico/guincho para este carro". O [NovoServico.tsx](../Frontend/src/pages/Servicos/NovoServico.tsx) só pré-seleciona o carro quando a garagem tem um único carro e não lê `?idCarro=` da URL.
- **Linguagem técnica na tela.** Aparecem textos como "notaMatch (0..100)" ([ServicoDetalhe.tsx:251](../Frontend/src/pages/Servicos/ServicoDetalhe.tsx#L251), [ServicoDetalhePrestador.tsx:134](../Frontend/src/pages/Prestador/ServicoDetalhePrestador.tsx#L134)), "Em andamento (PENDENTE)" e "Prestador no local (CHEGOU)" ([enums.ts](../Frontend/src/utils/enums.ts)), e "Inscrições CONFIRMADAS" ([Dashboard.tsx](../Frontend/src/pages/Organizador/Dashboard.tsx)). Isso descreve o modelo de dados, não o benefício para o usuário.
- **Banco vazio na demonstração.** Não há seed (nenhum `CommandLineRunner` nem `data.sql`). Numa instalação nova, o primeiro minuto mostra estados vazios ("Nenhum evento ativo", "Garagem vazia") e o ranking de match não tem prestadores para ordenar.

---

## 3. Objetivo e critério de sucesso

**Teste dos 60 segundos:** uma pessoa que nunca viu o CarsHub abre `http://localhost:5173/` e, em até 1 minuto e sem ajuda, consegue:

1. dizer com as próprias palavras os **3 pilares** da plataforma;
2. ver **pelo menos um exemplo real** de cada pilar, incluindo um ranking de match com nota e distância;
3. chegar a qualquer pilar em **no máximo 2 cliques** a partir da tela inicial.

Indicadores secundários:

- Nenhum termo técnico de enum ou campo de DTO na interface.
- Toda tela de estado vazio oferece uma ação que leva ao próximo passo.
- O entusiasta sem carro **vê** que existem serviços e o que precisa fazer para usá-los.

---

## 4. Ações

Legenda de esforço: **P** = até meio dia · **M** = 1 a 2 dias · **G** = 3 dias ou mais.

### P0 — Obrigatórias (resolvem diretamente o feedback)

#### A1. Página inicial pública (landing) em `/`
- **O quê:** nova página `pages/Inicio/Inicio.tsx`, com:
  - **Hero** com a proposta em uma frase (ex.: "Sua garagem de clássicos, o socorro certo e os encontros da comunidade, num só lugar") e dois botões: "Criar conta" e "Ver eventos".
  - **3 cards de pilar** (Garagem e histórico · Mecânico e guincho com match · Eventos), cada um com uma linha de benefício e um link.
  - **"Como escolhemos o prestador"**: explicação simples do match (60% reputação, 40% distância, quem ainda não tem avaliações parte de 4,0) com um exemplo visual de 2 ou 3 linhas de ranking. Os dados podem ser fixos e marcados como "exemplo".
  - **Próximos eventos**: os 3 primeiros do catálogo via `listarCatalogo(0, 3)`, um endpoint que já é público.
  - **"Para quem é"**: 3 entradas de perfil (Colecionador · Mecânico/Guincho · Organizador), cada uma levando a `/cadastro?tipo=...`.
- **Onde:** [App.tsx](../Frontend/src/App.tsx) (trocar o `Navigate` de `/` pela nova página), [NavBar.tsx](../Frontend/src/components/NavBar.tsx) (para o convidado, `destinoMarca` passa a ser `/`), [global.css](../Frontend/src/styles/global.css).
- **Backend:** nenhum.
- **Esforço:** M.

#### A2. Painel inicial do entusiasta/colecionador (`/painel`)
- **O quê:** uma home que **resume os 3 pilares** para quem está logado, em vez de jogar a pessoa no catálogo:
  - **Garagem:** quantidade de carros (`listarCarros(0, 1).totalElementos`) e atalho para o último carro.
  - **Serviços em andamento:** até 3 itens de `listarPainelConsolidado`, com status em linguagem amigável e botões "Pedir mecânico" e "Pedir guincho".
  - **Minhas próximas inscrições:** até 3 itens de `listarMinhasInscricoes`.
  - **Eventos em destaque:** 3 itens de `listarCatalogo`.
  - **Checklist de "próximos passos"** para conta nova: completar localização, cadastrar o 1º carro (que transforma a conta em Colecionador e libera os serviços), inscrever-se num evento.
- **Onde:** nova `pages/Painel/Painel.tsx`. Em [AuthContext.tsx](../Frontend/src/context/AuthContext.tsx), `rotaInicialParaTipo` passa a retornar `/painel` para `ENTUSIASTA` e `COLECIONADOR`. Incluir "Início" na [NavBar.tsx](../Frontend/src/components/NavBar.tsx).
- **Backend:** nenhum. Todos os endpoints já existem. Para o entusiasta, os cards de serviço ficam em modo "bloqueado" e não fazem chamadas ao backend, porque `/colecionador/**` depende do perfil.
- **Esforço:** M.

#### A3. Dados de demonstração (seed) e contas de demo
- **O quê:** um popular de dados ativo **só no profile `demo`**, que cria:
  - 1 organizador com 4 a 6 eventos `ATIVO` com fotos, mais 2 `CONCLUIDO` e 1 `CANCELADO` (para o dashboard ter conteúdo).
  - 1 colecionador com 2 ou 3 carros com fotos, **serviços finalizados** (histórico preenchido) e **1 chamado aberto** com ranking visível.
  - 4 a 6 mecânicos e guinchos em **distâncias e números de avaliações diferentes**, para o ranking mostrar o efeito da média bayesiana. Por exemplo, um prestador com 5,0 e uma única avaliação fica abaixo de outro com 4,7 e trinta avaliações.
  - Na tela de login, um bloco "Contas de demonstração" que só aparece quando `import.meta.env.DEV` (ou uma flag `VITE_MODO_DEMO`) estiver ativo.
- **Onde:** nova classe `config/PopuladorDemo.java` (`@Profile("demo")`, `CommandLineRunner`), [docker-compose.yaml](../docker-compose.yaml) / [.env.example](../.env.example) (`SPRING_PROFILES_ACTIVE=demo`), [leiame.txt](../leiame.txt) e [Login.tsx](../Frontend/src/pages/Login/Login.tsx).
- **Backend:** só a carga de dados, usando os services e repositórios existentes. Nenhuma regra nova.
- **Esforço:** M.
- **Por que é P0:** sem dados, as ações A1 e A2 mostram estados vazios e a banca volta a ver "listas".

### P1 — Recomendadas (encurtam caminhos e deixam o valor mais claro)

#### A4. Atalhos contextuais entre módulos
- [CarroDetalhe.tsx](../Frontend/src/pages/Garagem/CarroDetalhe.tsx): botões **"Pedir mecânico para este carro"** e **"Pedir guincho"** levando a `/servicos/novo-mecanico?idCarro=...`.
- [NovoServico.tsx](../Frontend/src/pages/Servicos/NovoServico.tsx): ler `?idCarro=` com `useSearchParams` e pré-selecionar o carro.
- [ServicoDetalhe.tsx](../Frontend/src/pages/Servicos/ServicoDetalhe.tsx): com o serviço `FINALIZADO`, mostrar o link **"Ver no histórico do carro"**.
- [CatalogoDetalhe.tsx](../Frontend/src/pages/Catalogo/CatalogoDetalhe.tsx): depois da inscrição, mostrar o link **"Ver minhas inscrições"**.
- [Garagem.tsx](../Frontend/src/pages/Garagem/Garagem.tsx): no card de cada carro, um botão "Pedir serviço".
- **Backend:** nenhum. **Esforço:** P.

#### A5. Serviços visíveis (e explicados) para o entusiasta
- Mostrar o item **"Serviços"** na [NavBar.tsx](../Frontend/src/components/NavBar.tsx) também para `ENTUSIASTA`.
- Na rota `/servicos`, em vez do redirecionamento silencioso da guarda `ExigirPerfil` ([App.tsx](../Frontend/src/App.tsx)), mostrar para o entusiasta uma **tela de desbloqueio**: o que são os serviços, como funciona o match e um botão "Cadastre seu primeiro carro para liberar".
- **Backend:** nenhum. A regra continua no backend, que segue bloqueando `/colecionador/**`. **Esforço:** P.

#### A6. Linguagem de produto no lugar de linguagem técnica
| Hoje | Proposta | Arquivo |
|---|---|---|
| `notaMatch (0..100)` | **"Compatibilidade: 87%"** com uma dica explicando o cálculo | [ServicoDetalhe.tsx](../Frontend/src/pages/Servicos/ServicoDetalhe.tsx), [ServicoDetalhePrestador.tsx](../Frontend/src/pages/Prestador/ServicoDetalhePrestador.tsx), `BadgeNotaMatch` |
| `Em andamento (PENDENTE)` | "Prestador a caminho" | [enums.ts](../Frontend/src/utils/enums.ts) |
| `Prestador no local (CHEGOU)` | "Prestador no local" | [enums.ts](../Frontend/src/utils/enums.ts) |
| `Participantes (CONFIRMADAS)` | "Participantes confirmados" | [Dashboard.tsx](../Frontend/src/pages/Organizador/Dashboard.tsx) |
| Rodapé com URL da API | "CarsHub · garagem, serviços e eventos para clássicos" | [Layout.tsx](../Frontend/src/components/Layout.tsx) |
| "Entre com a sua conta" | Uma linha de proposta de valor acima do formulário | [Login.tsx](../Frontend/src/pages/Login/Login.tsx) |

- **Backend:** nenhum. Os valores dos enums enviados no JSON continuam os mesmos; muda só o rótulo exibido. **Esforço:** P.

#### A7. Linha do tempo do serviço
- No [ServicoDetalhe.tsx](../Frontend/src/pages/Servicos/ServicoDetalhe.tsx), mostrar os status `SOLICITADO → PROPOSTA → PENDENTE → CHEGOU → FINALIZADO` como uma **linha do tempo horizontal** com a etapa atual destacada e as fotos inicial e final lado a lado ("antes/depois").
- Isso deixa visível de uma vez o fluxo completo, a segurança (fotos de evidência) e a avaliação, que hoje estão espalhados em seções.
- **Backend:** nenhum. **Esforço:** M.

### P2 — Desejáveis (reforçam a impressão, se houver tempo)

#### A8. Catálogo de eventos mais visual
- Grade de cards com a foto em destaque, data em formato de "selo" (dia/mês), cidade e vagas, em vez da pilha de cards de texto em [Catalogo.tsx](../Frontend/src/pages/Catalogo/Catalogo.tsx).
- Cabeçalho com uma linha de contexto ("Encontros, exposições e passeios de clássicos").
- **Backend:** nenhum. Filtro por cidade ou proximidade fica **fora do escopo**, porque exigiria parâmetros novos no `CatalogoEventoController`. **Esforço:** P/M.

#### A9. Dashboard do organizador com mais leitura
- **Taxa de ocupação** por evento (`totalParticipantes / limiteVagas`), calculada no frontend a partir de `listarHistoricoDashboard`.
- Bloco **"Próximos eventos ativos"** usando `listarMeusEventos`, e botão "Criar evento" no topo.
- **Backend:** nenhum. **Esforço:** P.

#### A10. Cabeçalho-resumo no painel do prestador
- No topo de [PainelDisponiveis.tsx](../Frontend/src/pages/Prestador/PainelDisponiveis.tsx): quantidade de chamados disponíveis no raio, quantidade na "Minha fila" (`listarFilaDeTrabalho(0, 1).totalElementos`) e uma frase explicando o que é a compatibilidade.
- **Backend:** nenhum. **Esforço:** P.

---

## 5. Ordem de execução sugerida

| Etapa | Ações | Resultado visível |
|---|---|---|
| 1 | **A3** (seed) | Toda tela passa a ter conteúdo real para demonstrar |
| 2 | **A1** (landing) + **A6** (linguagem) | O primeiro minuto do convidado comunica os 3 pilares |
| 3 | **A2** (painel) + **A5** (serviços visíveis) | O primeiro minuto depois do login também comunica os 3 pilares |
| 4 | **A4** (atalhos) + **A7** (linha do tempo) | Os diferenciais ficam a 1 ou 2 cliques |
| 5 | **A8**, **A9**, **A10** | Acabamento visual |

A seed vem primeiro porque é ela que permite validar visualmente todas as outras ações.

---

## 6. Roteiro de 60 segundos para a apresentação

| Tempo | Tela | O que mostrar |
|---|---|---|
| 0–10 s | Landing `/` | Proposta em uma frase e os 3 pilares |
| 10–20 s | Landing, seção "Como escolhemos o prestador" | O match em linguagem simples (reputação + distância) |
| 20–35 s | Login com a conta demo de colecionador → `/painel` | Garagem com carros, serviço em andamento, inscrições |
| 35–50 s | Clique no serviço em andamento | Linha do tempo, ranking de compatibilidade, fotos de antes/depois |
| 50–60 s | Clique em "Ver histórico do carro" | Manutenções anteriores com prestador, itens e valor |

---

## 7. Fora do escopo e cuidados

- **Nenhuma regra de negócio nova.** Todo o plano usa endpoints existentes. A autorização continua no backend (`SecurityConfigurations`); o frontend só esconde ou mostra elementos.
- **A seed não pode rodar em produção.** Ela fica restrita a `@Profile("demo")`, e as contas de demonstração na tela de login só aparecem com a flag de demo ou de dev ativa.
- **Cuidado com chamadas N+1.** A [Garagem.tsx](../Frontend/src/pages/Garagem/Garagem.tsx) já faz uma chamada de detalhe por carro para buscar as fotos. O painel (A2) não deve repetir esse padrão: usar só listagens com `tamanho` pequeno.
- **Filtros novos no catálogo** (cidade, raio, data) ficam para depois, porque exigem mudanças no backend.

---

## 8. Checklist de aceite

- [ ] `/` abre uma landing com proposta de valor, 3 pilares e uma explicação do match.
- [ ] A landing mostra eventos reais vindos do catálogo público.
- [ ] Entusiasta e colecionador entram em `/painel`, não em `/eventos`.
- [ ] O entusiasta vê "Serviços" no menu e encontra uma explicação de como desbloqueá-los.
- [ ] O detalhe do carro tem atalho para pedir mecânico ou guincho com o carro pré-selecionado.
- [ ] Nenhuma tela mostra nomes de enum em maiúsculas (`PENDENTE`, `CONFIRMADAS`) nem o termo `notaMatch`.
- [ ] O rodapé não mostra URL nem tecnologia.
- [ ] Com o profile `demo`, todas as telas principais têm conteúdo e o ranking mostra o efeito da média bayesiana.
- [ ] Teste dos 60 segundos aplicado com pelo menos 2 pessoas de fora do projeto, com os 3 pilares identificados.
