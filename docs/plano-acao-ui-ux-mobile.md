# Plano de ação — UI/UX: mobile-first, galeria e chamado com menos passos

> **Feedback da banca:** "UI/UX: A plataforma deve ter um layout pensado para mobile e outro para computadores. Mobile-first (o público vive no celular), catálogo e detalhe do carro com galeria, fluxo de chamado com menos passos."

---

## 1. Leitura do feedback

A banca pede quatro coisas:

1. **Um padrão único de UI/UX na aplicação toda.** Todas as telas seguem o mesmo padrão visual e de interação, com as adaptações pertinentes a cada uma. Não se trata de melhorar telas isoladas.
2. **Dois layouts:** um para celular e outro para computador, com o celular como ponto de partida (*mobile-first*). Não basta "não quebrar" na tela pequena: a navegação e a disposição das telas devem ser pensadas para o polegar.
3. **Galeria de fotos** em **eventos** (catálogo, detalhe do evento e telas do organizador) e no **detalhe do carro**: fotos grandes, deslizar com o dedo, tela cheia.
4. **Chamado de serviço automotivo (mecânico e guincho) com menos passos**, mais fácil para o **colecionador solicitar**. Ele geralmente está na rua, ao lado do carro, com o celular na mão.

> **Vitrine pública de carros:** também vai existir e terá um plano próprio, a partir de outro parágrafo da banca. Aqui ela entra apenas como **consumidora do padrão**: a `Galeria` (G1), o card com capa (P2) e os modelos de lista e detalhe (P3) devem ser genéricos o suficiente para servir também à vitrine, sem retrabalho.

Este plano se conecta aos outros dois:
- [plano-acao-fotos.md](plano-acao-fotos.md) — limite de 5 fotos, capa, câmera e compressão (as fotos que alimentam a galeria).
- [plano-acao-densidade-valor.md](plano-acao-densidade-valor.md) — página inicial, painel e linguagem de produto.

---

## 2. Diagnóstico no código

### 2.1 O CSS foi escrito para o computador

| Ponto | Situação | Onde |
|---|---|---|
| Estratégia | O estilo base é o de computador, com **uma única** `@media (max-width: 640px)` no final do arquivo, que só ajusta a navbar, os rótulos e o carrossel. Ou seja, o mobile é tratado como exceção. | [global.css](../Frontend/src/styles/global.css), por volta da linha 1124 |
| Largura | `.conteudo` e `.navbar-conteudo` com `max-width: 1080px` e um único padding | [global.css](../Frontend/src/styles/global.css) |
| Navegação | Navbar *sticky* no topo com `flex-wrap`. Com 4 ou 5 links + avatar + "Sair", **quebra em 2 ou 3 linhas** no celular e ocupa boa parte da tela fixa. Não há menu inferior nem menu compacto. | [NavBar.tsx](../Frontend/src/components/NavBar.tsx) |
| Campos de formulário | `.campo-input` com `font-size: 0.95rem` (cerca de 15px). **No iPhone, um campo com menos de 16px faz a página dar zoom ao receber o foco.** | [global.css](../Frontend/src/styles/global.css) |
| Área de toque | `.botao` com `padding: 9px 16px` e `font-size: 0.92rem`, cerca de 38px de altura. O mínimo recomendado para toque é **44px**. `.tab` e `.nav-link` são ainda menores. | [global.css](../Frontend/src/styles/global.css) |
| Modais | Caixa centralizada com `max-width: 520px`. No celular, o ideal é uma folha que sobe da parte de baixo da tela (*bottom sheet*). | [Modal.tsx](../Frontend/src/components/Modal.tsx) |
| Ações principais | Ficam no fim de formulários longos (o carro tem cerca de 26 campos). No celular, o usuário precisa rolar até o final para salvar. | [CarroForm.tsx](../Frontend/src/pages/Garagem/CarroForm.tsx), [EventoForm.tsx](../Frontend/src/pages/Organizador/EventoForm.tsx) |
| Estilos inline | **45** `style={{…}}` em páginas e componentes, que não respondem a media queries | `pages/`, `components/` |
| Viewport | Tem `width=device-width`, mas não tem `viewport-fit=cover` nem `theme-color`, então não dá para tratar o entalhe da tela nem a barra inferior do iPhone | [index.html](../Frontend/index.html) |

### 2.2 Galeria

| Tela | Hoje |
|---|---|
| Catálogo de eventos | Cards empilhados com **só a primeira foto** (`fotos[0]`, `maxHeight: 220` inline) e o texto completo da descrição |
| Detalhe do evento (público e do organizador) | Fotos lado a lado (`fotos.map`), sem carrossel e sem tela cheia ([CatalogoDetalhe.tsx](../Frontend/src/pages/Catalogo/CatalogoDetalhe.tsx), [EventoDetalhe.tsx](../Frontend/src/pages/Organizador/EventoDetalhe.tsx)) |
| Detalhe do carro | Lista com **26 linhas** de especificação à esquerda e um carrossel de no máximo 320px à direita (no celular, o carrossel fica embaixo, **depois** das 26 linhas) ([CarroDetalhe.tsx](../Frontend/src/pages/Garagem/CarroDetalhe.tsx)) |
| Componente [CarrosselFotos.tsx](../Frontend/src/components/CarrosselFotos.tsx) | Só setas e pontos. **Não desliza com o dedo**, não tem miniaturas, não tem tela cheia, não usa `loading="lazy"` e não responde às setas do teclado. |

### 2.3 Fluxo de chamado (lado do colecionador, serviço mecânico)

| # | Interação hoje | Onde |
|---|---|---|
| 1 | Menu "Meus serviços" | [NavBar.tsx](../Frontend/src/components/NavBar.tsx) |
| 2 | "+ Serviço mecânico" (há **rotas separadas** para mecânico e guincho) | [ServicosColecionador.tsx](../Frontend/src/pages/Servicos/ServicosColecionador.tsx) |
| 3 | Selecionar o veículo (só vem pré-selecionado se a garagem tiver 1 carro) | [NovoServico.tsx](../Frontend/src/pages/Servicos/NovoServico.tsx) |
| 4 | Marcar os serviços | idem |
| 5 | Ler o bloco de ajuda técnico ("geocodificação bloqueante", "o backend usa…") e entender os campos **Latitude/Longitude** numéricos | idem |
| 6 | "Usar meu GPS" (o botão fica **no fim** do formulário, ao lado de "Solicitar") | idem |
| 7 | "Solicitar serviço" | idem |
| 8 | **Recarregar a página manualmente** para ver se chegou uma proposta. Não há atualização automática (nenhum `setInterval` no projeto, exceto o contador regressivo). | [ServicoDetalhe.tsx](../Frontend/src/pages/Servicos/ServicoDetalhe.tsx) |
| 9 | "Aceitar proposta" | idem |
| 10 | Enviar a foto inicial | [SecaoFotoInicial.tsx](../Frontend/src/components/servicos/SecaoFotoInicial.tsx) |
| 11 | "Confirmar chegada" (outra seção, outro botão) | [SecaoChegada.tsx](../Frontend/src/components/servicos/SecaoChegada.tsx) |
| 12 | Enviar a foto final | [SecaoFotoFinal.tsx](../Frontend/src/components/servicos/SecaoFotoFinal.tsx) |
| 13 | Escolher as estrelas e enviar a avaliação | [SecaoAvaliacao.tsx](../Frontend/src/components/servicos/SecaoAvaliacao.tsx) |
| 14 | "Confirmar conclusão" | [SecaoConclusao.tsx](../Frontend/src/components/servicos/SecaoConclusao.tsx) |

São **cerca de 14 interações** e um recarregamento manual. Além disso:
- Todas as seções do detalhe aparecem **empilhadas ao mesmo tempo** (informações, fotos, chegada, avaliação, conclusão, cancelamento). O usuário precisa descobrir qual é a próxima ação.
- As etapas 12 a 14 são **sequenciais e obrigatórias** no backend ("Envie a sua foto final antes de avaliar", "precisa avaliar a outra parte antes de confirmar a conclusão"). São três toques separados para o que, para o usuário, é um único momento: "terminou".
- As etapas 10 e 11 também são consecutivas: a foto inicial é pré-requisito da confirmação de chegada.
- O lado do prestador ([PainelDisponiveis.tsx](../Frontend/src/pages/Prestador/PainelDisponiveis.tsx), [ServicoDetalhePrestador.tsx](../Frontend/src/pages/Prestador/ServicoDetalhePrestador.tsx)) repete as etapas de chegada, foto final, avaliação e conclusão.

**Importante:** dá para reduzir os passos **só no frontend**. As regras do backend (ordem, pré-requisitos, dupla confirmação) continuam as mesmas. O frontend agrupa num único gesto chamadas que hoje são separadas.

---

## 3. Objetivo e critério de sucesso

- **Padrão único:** **todas** as telas (inventário na seção 4, P3) usam os mesmos tokens, componentes e modelos de página. Uma pessoa que aprende a usar uma lista, um detalhe ou um formulário sabe usar todos os outros.
- **Referência de tela:** celular de **360 × 800** (o Android mais comum) como base; computador a partir de **1024px**.
- **Navegação no celular:** todas as áreas principais do perfil ficam acessíveis com **um toque** numa barra inferior fixa, sem rolar a página.
- **Sem zoom indesejado no iPhone** e **sem rolagem horizontal** em nenhuma tela a 360px.
- **Área de toque** de pelo menos 44 × 44px em botões, abas e links de navegação.
- **Galeria:** desliza com o dedo, abre em tela cheia, mostra miniaturas no computador e é a **primeira coisa** que aparece no detalhe do carro e do evento.
- **Chamado:** **solicitar** um serviço em **até 3 toques** para mecânico e **2 toques** para guincho. O fluxo completo do colecionador cai de **cerca de 14 para cerca de 7 interações**, sem nenhum recarregamento manual.
- **Lighthouse (mobile):** Acessibilidade ≥ 90 e Boas práticas ≥ 90 nas telas de catálogo, detalhe do carro e chamado.

---

## 4. Ações — Padrão único de UI/UX (aplicação toda)

Legenda de esforço: **P** = até meio dia · **M** = 1 a 2 dias · **G** = 3 dias ou mais.

Hoje cada tela monta a própria estrutura: títulos, cards, botões de link com `style` inline, listas de dados e ações em posições diferentes. O padrão único nasce de **três camadas**, nesta ordem: tokens → componentes → modelos de página.

### P1. Tokens visuais — **P**
- Centralizar em `:root` de [global.css](../Frontend/src/styles/global.css):
  - cores (já existem);
  - **espaçamento** (`--espaco-1..6`);
  - **tipografia** (`--fonte-xs..xl`, com 16px no corpo do celular);
  - **raios**, **sombras** e **área de toque** (`--toque-min: 44px`);
  - **pontos de quebra** (768px e 1024px, documentados no topo do arquivo, já que o CSS não aceita variáveis em media queries).
- Regra: **nenhum valor solto** de cor, espaço ou fonte nas classes novas. Tudo vem dos tokens.

### P2. Biblioteca de componentes compartilhados — **M**
Componentes que **todas** as telas usam. Os que já existem são padronizados; os novos são criados:

| Componente | Situação | Uso |
|---|---|---|
| `Botao` / `BotaoLink` | Existe [Botao.tsx](../Frontend/src/components/Botao.tsx); **criar** `BotaoLink` (`<Link>` com aparência de botão) | Substitui os vários `<Link className="botao…" style={{ textDecoration: 'none' }}>` |
| `CabecalhoPagina` | **Novo** (substitui `.titulo-pagina` montado à mão) | Título + subtítulo + ação principal + menu "⋯" com as ações secundárias |
| `CardComCapa` | **Novo** | Item de lista com foto de capa: eventos, carros da garagem, eventos do organizador e, no futuro, a vitrine |
| `CardResumo` | **Novo** (generaliza o `.card` atual) | Item de lista sem foto: inscrições, chamados, histórico, fila do prestador |
| `Galeria` | Evolução do [CarrosselFotos.tsx](../Frontend/src/components/CarrosselFotos.tsx) (G1) | Todo detalhe com fotos |
| `ListaDados` | **Novo** (substitui `.linha-dados` e `<dl className="linha-tempo">`) | Pares rótulo/valor: rótulo acima do valor no celular, ao lado no computador |
| `SecaoRecolhivel` | **Novo** | Especificações do carro, detalhes do serviço, blocos longos de formulário |
| `Pilula` / `Badge` | Existe [BadgeStatus.tsx](../Frontend/src/components/BadgeStatus.tsx) | Destaques (motor, câmbio), status, vagas, compatibilidade |
| `BarraAcaoFixa` | **Novo** | Ação principal fixa no rodapé no celular, inline no computador |
| `Modal` / bottom sheet | Existe [Modal.tsx](../Frontend/src/components/Modal.tsx), ganha variante (L3) | Confirmações e gestos agrupados (C4) |
| `Abas` | **Novo** (substitui `.tabs` montado à mão em Perfil e no detalhe do serviço) | Rolagem horizontal no celular |
| `EstadoVazio`, `Loading`, `Alerta`, `Paginacao`, `FormField` | Existem | Mantidos, ajustados aos tokens e à área de toque |

### P3. Modelos de página e inventário da aplicação — **M**
Cada tela passa a seguir **um de cinco modelos**. As "adaptações pertinentes" acontecem **dentro** do modelo (conteúdo, ações), sem inventar uma estrutura nova:

| Modelo | Estrutura no celular | Estrutura no computador |
|---|---|---|
| **Lista** | `CabecalhoPagina` → filtros em chips roláveis → cards em 1 coluna → `Paginacao` → `EstadoVazio` quando vazia | Mesma ordem, cards em grade de 2 ou 3 colunas |
| **Detalhe com galeria** | `Galeria` → título e pílulas → ações rápidas → `SecaoRecolhivel` → ação principal em `BarraAcaoFixa` | 2 colunas: galeria (cerca de 60%) e informações + ações |
| **Detalhe sem galeria** | `CabecalhoPagina` → cartão de destaque (status ou próxima ação) → `SecaoRecolhivel` | 2 colunas: cartão de destaque e detalhes |
| **Formulário** | Seções recolhíveis ou etapas → `BarraAcaoFixa` com a ação principal | Seções abertas em grade de 2 ou 3 colunas, ação no fim |
| **Página pública / autenticação** | Coluna única centralizada, com a marca e a proposta de valor no topo | Duas colunas: proposta de valor e formulário |

**Inventário (todas as telas):**

| Tela | Modelo |
|---|---|
| [Login](../Frontend/src/pages/Login/Login.tsx), [Cadastro](../Frontend/src/pages/Cadastro/Cadastro.tsx), [ConfirmarCodigo](../Frontend/src/pages/ConfirmarCodigo/ConfirmarCodigo.tsx), [EsqueciSenha](../Frontend/src/pages/RecuperarSenha/EsqueciSenha.tsx), [RedefinirSenha](../Frontend/src/pages/RecuperarSenha/RedefinirSenha.tsx) | Pública / autenticação |
| [Catalogo](../Frontend/src/pages/Catalogo/Catalogo.tsx), [EventosOrganizador](../Frontend/src/pages/Organizador/EventosOrganizador.tsx), [Garagem](../Frontend/src/pages/Garagem/Garagem.tsx) | Lista com `CardComCapa` |
| [Inscricoes](../Frontend/src/pages/Inscricoes/Inscricoes.tsx), [ServicosColecionador](../Frontend/src/pages/Servicos/ServicosColecionador.tsx), [CarroHistorico](../Frontend/src/pages/Garagem/CarroHistorico.tsx), [PainelDisponiveis](../Frontend/src/pages/Prestador/PainelDisponiveis.tsx), [FilaTrabalho](../Frontend/src/pages/Prestador/FilaTrabalho.tsx) | Lista com `CardResumo` |
| [CatalogoDetalhe](../Frontend/src/pages/Catalogo/CatalogoDetalhe.tsx), [EventoDetalhe](../Frontend/src/pages/Organizador/EventoDetalhe.tsx), [CarroDetalhe](../Frontend/src/pages/Garagem/CarroDetalhe.tsx) | **Detalhe com galeria** |
| [ServicoDetalhe](../Frontend/src/pages/Servicos/ServicoDetalhe.tsx), [ServicoDetalhePrestador](../Frontend/src/pages/Prestador/ServicoDetalhePrestador.tsx), [Dashboard](../Frontend/src/pages/Organizador/Dashboard.tsx) | Detalhe sem galeria (cartão de destaque) |
| [CarroForm](../Frontend/src/pages/Garagem/CarroForm.tsx), [EventoForm](../Frontend/src/pages/Organizador/EventoForm.tsx), [Perfil](../Frontend/src/pages/Perfil/Perfil.tsx), [NovoServico](../Frontend/src/pages/Servicos/NovoServico.tsx) (pedir ajuda, C1) | Formulário |
| [NotFound](../Frontend/src/pages/NotFound.tsx) | `EstadoVazio` com uma ação de volta |
| Futuras: página inicial e painel (plano de densidade de valor), vitrine de carros (plano próprio) | Já nascem nos modelos acima |

### P4. Guia do padrão — **P**
- Criar `docs/padrao-ui.md`, curto: tokens, quando usar cada componente, os cinco modelos (com um esboço ASCII de cada) e as regras ("ação principal sempre na `BarraAcaoFixa` no celular", "ações destrutivas sempre no menu ⋯ ou no fim da página", "nenhum `style={{}}` novo").
- Serve de referência para as telas novas (vitrine, painel) e de evidência para a banca de que o padrão é intencional.

---

## 5. Ações — Fundamentos do layout (mobile e computador)

### L1. CSS mobile-first com pontos de quebra definidos — **M**
- **Reescrever a base** de [global.css](../Frontend/src/styles/global.css) para o celular e **ampliar** para telas maiores com `min-width`:
  ```css
  /* base = celular (< 768px) */
  @media (min-width: 768px)  { /* tablet */ }
  @media (min-width: 1024px) { /* computador */ }
  ```
- Remover a `@media (max-width: 640px)` atual e incorporar as regras dela na base.
- Aplicar os **tokens do P1** em todas as classes existentes. `--largura-conteudo: 1080px` vale só a partir de 1024px.
- **Tipografia:** `.campo-input`, `select` e `textarea` com `font-size: 16px` no celular (corrige o zoom do iPhone). `.botao`, `.tab` e `.nav-link` com `min-height: var(--toque-min)`.
- **Grades:** a base é uma coluna. `.form-grid` com 2 colunas a partir de 768px e 3 a partir de 1024px. As listas de cards (catálogo, garagem, eventos do organizador) seguem o mesmo padrão: 1 / 2 / 3 colunas.
- **Migrar os 45 estilos inline** para classes, principalmente imagens com `maxHeight`, `width: '100%'` e `textDecoration: 'none'` em links com aparência de botão (criar `.botao` aplicável a `<Link>`).
- Em [index.html](../Frontend/index.html): `viewport-fit=cover` e `<meta name="theme-color">`. Usar `env(safe-area-inset-bottom)` na barra inferior (L2).

### L2. Dois layouts de navegação — **M**
A banca pede "um layout para mobile e outro para computadores". A proposta é **um layout por tipo de tela**, com as mesmas páginas e a mesma lógica:

| | Celular (< 1024px) | Computador (≥ 1024px) |
|---|---|---|
| Topo | Barra compacta: marca à esquerda, avatar à direita (abre um menu com Perfil e Sair) | Navbar horizontal completa (como hoje, mas sem quebrar linha) |
| Navegação principal | **Barra inferior fixa** com 4 ou 5 ícones e rótulos por perfil (ver tabela abaixo) | Links na navbar |
| Ação principal da tela | **Botão fixo no rodapé da tela**, acima da barra inferior ("Salvar", "Solicitar", "Inscrever-se") | Botão no fim do formulário ou no cabeçalho da página |
| Modais | **Bottom sheet** (sobe de baixo, ocupa a largura, com alça para arrastar) | Caixa centralizada (como hoje) |
| Detalhes (carro, evento, serviço) | Uma coluna: galeria → destaques → seções recolhíveis | Duas colunas: galeria grande à esquerda, dados à direita |

**Itens da barra inferior por perfil** (a mesma lógica de visibilidade da [NavBar.tsx](../Frontend/src/components/NavBar.tsx)):

| Perfil | Itens |
|---|---|
| Convidado | Início · Eventos · Entrar |
| Entusiasta | Início · Eventos · Garagem · Inscrições · Serviços (com explicação de desbloqueio, ver o plano de densidade de valor, A5) |
| Colecionador | Início · Eventos · Garagem · **Pedir ajuda** (botão central em destaque) · Serviços |
| Organizador | Dashboard · Meus eventos · **Criar evento** (central) · Eventos |
| Mecânico / Guincho | Chamados · Minha fila · Perfil |

**Implementação:**
- Novos componentes `BarraInferior.tsx` e `BarraSuperiorCompacta.tsx` em `components/`. O [Layout.tsx](../Frontend/src/components/Layout.tsx) renderiza os dois conjuntos, e o **CSS** mostra ou esconde cada um por ponto de quebra (não há "pisca" no carregamento nem estado de redimensionamento para controlar).
- Um hook `useEhMobile()` (`matchMedia('(max-width: 1023px)')`) **só** onde a estrutura muda de verdade: Modal ou bottom sheet, galeria com ou sem miniaturas.
- A lista de itens por perfil vira uma **configuração única** (`utils/navegacao.ts`), usada pela navbar e pela barra inferior. Isso elimina a duplicação de regras.
- Ícones: SVG inline (sem dependência nova) ou `lucide-react` (pequena e com *tree-shaking*).

### L3. Componentes adaptados ao toque — **P**
- [Modal.tsx](../Frontend/src/components/Modal.tsx): variante bottom sheet no celular, com o foco preso dentro dela e fechamento com Esc ou toque fora.
- `.tabs`: rolagem horizontal (`overflow-x: auto; scroll-snap-type: x`) em vez de `flex-wrap`, para as abas não quebrarem linha (Perfil e detalhe do serviço).
- `.linha-dados`: no celular, o rótulo fica **acima** do valor (hoje `min-width: 150px/110px` aperta o valor).
- Formulários longos ([CarroForm.tsx](../Frontend/src/pages/Garagem/CarroForm.tsx)): agrupar em seções recolhíveis (Identificação · Motor · Transmissão · Chassi · Interior e conforto · Fotos) e deixar a ação principal fixa no rodapé.

---

## 6. Ações — Galeria (eventos e detalhe do carro)

A galeria segue o modelo **Detalhe com galeria** (P3). As telas de evento e de carro ficam com a mesma estrutura e diferem só no conteúdo.

### G1. Componente `Galeria` (evolução do `CarrosselFotos`) — **M**
| Recurso | Celular | Computador |
|---|---|---|
| Navegação | **Deslizar com o dedo** usando `scroll-snap` do CSS (sem biblioteca), com contador "2/5" | Setas visíveis ao passar o mouse + **faixa de miniaturas** clicáveis abaixo da foto principal + setas ← → do teclado |
| Tela cheia | Toque na foto → **visualizador em tela cheia** (fundo escuro, deslizar, fechar com o "×" ou arrastando para baixo) | Clique → mesmo visualizador, fechando com Esc |
| Proporção | `aspect-ratio: 4 / 3` fixo, com `object-fit: cover` (sem a página "pular" enquanto a foto carrega) | Mesma proporção, foto principal maior |
| Desempenho | `loading="lazy"` e `decoding="async"` em todas as fotos, exceto a primeira (a capa) | Idem |
| Sem fotos | Bloco com ícone e "Sem fotos" no mesmo tamanho da galeria, para o layout não mudar | Idem |
| Acessibilidade | `aria-roledescription="carrossel"`, `aria-label` em cada foto ("foto 2 de 5"), foco visível | Idem |

- Substitui o [CarrosselFotos.tsx](../Frontend/src/components/CarrosselFotos.tsx) em todos os usos.
- A primeira foto é a **capa** definida no [plano-acao-fotos.md](plano-acao-fotos.md) (B2).

### G2. Detalhe do carro com galeria em primeiro plano — **M**
Nova organização de [CarroDetalhe.tsx](../Frontend/src/pages/Garagem/CarroDetalhe.tsx):

**Celular**
1. **Galeria** de largura total no topo.
2. **Título** (marca e modelo) + placa + ano.
3. **Destaques** em 4 "pílulas": Motor (ex.: "V8 5.0"), Câmbio, Combustível, Tração.
4. **Ações rápidas:** Pedir serviço · Histórico · Editar (as ações do plano de densidade de valor, A4).
5. **Especificações** em seções recolhíveis (fechadas por padrão): Motor · Transmissão · Chassi e rodas · Interior · Conforto. Hoje são 26 linhas soltas.
6. "Excluir" no fim, separado das outras ações.

**Computador:** duas colunas, com a galeria (cerca de 60%, foto grande + miniaturas) à esquerda e título, destaques, ações e especificações à direita.

### G3. Eventos com galeria (catálogo, detalhe e telas do organizador) — **M**
- **Lista ([Catalogo.tsx](../Frontend/src/pages/Catalogo/Catalogo.tsx)):**
  - Celular: uma coluna, com a **capa em largura total** (16:9) e um **selo de data** (dia e mês) sobre a foto. Abaixo: nome, cidade/UF, preço e vagas. A **descrição completa sai do card** (vai para o detalhe) para a lista ficar compacta.
  - Computador: grade com 2 ou 3 colunas no mesmo formato.
  - Um ícone indica quando o evento tem mais de uma foto ("1/4").
- **Detalhe ([CatalogoDetalhe.tsx](../Frontend/src/pages/Catalogo/CatalogoDetalhe.tsx) e [EventoDetalhe.tsx](../Frontend/src/pages/Organizador/EventoDetalhe.tsx)):**
  - `Galeria` no topo, no lugar das fotos lado a lado.
  - No celular, o botão **"Inscrever-se"** fica fixo no rodapé, com o preço ao lado.
  - Endereço com o botão "Abrir no mapa" (o link do Google Maps já existe).
- **Telas do organizador:** [EventosOrganizador.tsx](../Frontend/src/pages/Organizador/EventosOrganizador.tsx) usa o mesmo `CardComCapa` do catálogo, acrescentando status e ações de edição. [EventoDetalhe.tsx](../Frontend/src/pages/Organizador/EventoDetalhe.tsx) usa a mesma `Galeria` e a mesma estrutura do detalhe público, com as ações do organizador (editar, alterar status) no lugar de "Inscrever-se".
- A garagem ([Garagem.tsx](../Frontend/src/pages/Garagem/Garagem.tsx)) segue o mesmo padrão de card com capa (usando a `fotoCapa` do plano de fotos, B4).
- A futura **vitrine de carros** reutiliza `CardComCapa` + `Galeria` + o modelo de detalhe, sem componente novo.

---

## 7. Ações — Chamado com menos passos (foco no colecionador)

O foco é o **colecionador solicitar** mecânico ou guincho com o menor esforço possível. Por isso a prioridade é:

1. **C1 (pedir ajuda)**, onde está a dificuldade de solicitar;
2. **C2 (atualização automática)**, para o colecionador não precisar recarregar para saber da proposta;
3. **C3 e C4**, que deixam o acompanhamento até a conclusão mais simples.

As mudanças do lado do prestador são **consequência do padrão único** (mesma estrutura de tela), não o objetivo principal.

### C1. Uma entrada única: "Pedir ajuda" — **M**
- Unificar `/servicos/novo-mecanico` e `/servicos/novo-guincho` em **`/servicos/novo`** com seletor no topo: **[ Mecânico | Guincho ]**. As rotas antigas redirecionam para `?tipo=mecanico|guincho`.
- Acesso por **um toque**: o botão central "Pedir ajuda" da barra inferior (L2) e o atalho no detalhe do carro (`?idCarro=`).
- **Padrões inteligentes**, para que o caso comum precise só de confirmação:
  - **Veículo:** pré-selecionado por `?idCarro=` → último carro usado num chamado (guardado no `localStorage`) → único carro da garagem. No celular, a escolha usa **cards com a capa do carro**, não um `<select>`.
  - **Localização:** ao abrir a tela, **pede o GPS automaticamente** e mostra "📍 Usando sua localização atual" com a opção "Alterar". Se o GPS for negado, mostra "Usaremos o endereço do seu perfil" (o backend já usa o perfil quando nada é enviado) e a opção "Digitar endereço".
  - **Latitude/Longitude** numéricos saem da tela: só são preenchidos pelo GPS.
  - O **bloco de ajuda técnico** é trocado por uma frase simples. O efeito colateral real (o endereço digitado **atualiza a localização do perfil**) passa a ser dito ao usuário: "Este endereço também será salvo no seu perfil."
- **Serviços (mecânico):** chips grandes, de toque fácil, com os mais comuns primeiro.
- **Guincho:** sem serviços para escolher, o caso comum vira **abrir → confirmar** (2 toques).
- No celular, o botão **"Solicitar"** fica fixo no rodapé e mostra um resumo ("Mecânico · Fusca 1972 · Localização atual").

### C2. Acompanhamento sem recarregar a página — **P**
- No [ServicoDetalhe.tsx](../Frontend/src/pages/Servicos/ServicoDetalhe.tsx) e no [ServicoDetalhePrestador.tsx](../Frontend/src/pages/Prestador/ServicoDetalhePrestador.tsx): **atualização automática a cada 15 segundos** enquanto o status for `SOLICITADO`, `PROPOSTA`, `PENDENTE` ou `CHEGOU`.
  - Pausa com a aba oculta (`document.visibilitychange`) e atualiza imediatamente quando a aba volta.
  - Para em `FINALIZADO`.
  - Fica num hook reutilizável `useAtualizacaoPeriodica(funcao, intervalo, ativo)`.
- Quando o status muda, mostra um aviso ("Você recebeu uma proposta!") e faz o celular vibrar (`navigator.vibrate`, onde houver suporte).
- **Backend:** nenhum. Os endpoints de detalhe já existem, e o rate limit atual precisa ser conferido para essa frequência (1 requisição a cada 15s por tela aberta). Notificação em tempo real (WebSocket, SSE ou push) fica fora do escopo (seção 9).

### C3. Detalhe do serviço guiado pela próxima ação — **M**
Reorganizar o [ServicoDetalhe.tsx](../Frontend/src/pages/Servicos/ServicoDetalhe.tsx) em três blocos:

1. **Linha do tempo** compacta no topo: Solicitado → Proposta → A caminho → No local → Concluído, com a etapa atual destacada (o plano de densidade de valor, A7, usa o mesmo componente).
2. **Cartão "Próxima ação"**: **um único** bloco grande que muda conforme o status. Só ele tem botão principal:

| Status | Cartão "Próxima ação" |
|---|---|
| `SOLICITADO` | "Procurando prestadores… (3 compatíveis por perto)" + link para o ranking. Sem botão: aguarda a atualização automática (C2). |
| `PROPOSTA` | Prestador (nome, compatibilidade, distância, nota) + valor → **[Aceitar]** e [Recusar] |
| `PENDENTE` | "João está a caminho" + telefone com **link `tel:`** (ligação com um toque) → **[Prestador chegou]** (C4) |
| `CHEGOU` | "Serviço em andamento" → **[Finalizar serviço]** (C4) |
| `FINALIZADO` | Resumo + "Ver no histórico do carro" |

3. **Detalhes** (informações, datas, fotos, ranking completo) em seções **recolhíveis**, abaixo do cartão.

"Cancelar solicitação" passa a ser uma ação secundária (link no menu "⋯" do cabeçalho), e não uma seção inteira.

### C4. Agrupar ações sequenciais num gesto só — **M**
O backend continua exigindo a mesma ordem. O frontend apenas **encadeia as chamadas** depois de um único envio do usuário:

| Gesto único | Chamadas encadeadas (endpoints existentes) | Substitui |
|---|---|---|
| **"Prestador chegou"** → uma folha com a câmera aberta para a **foto inicial** → [Confirmar chegada] | `POST /foto-inicial` → `PATCH /confirmar-chegada` | Etapas 10 e 11 |
| **"Finalizar serviço"** → uma folha com **foto final** + **estrelas** (+ comentário opcional) → [Concluir] | `POST /foto-final` → `POST /avaliacao` → `PATCH /confirmar-conclusao` | Etapas 12, 13 e 14 |

- Se a foto inicial já tiver sido enviada antes (regra de imutabilidade, 422), a folha pula essa etapa e só confirma.
- **Falha no meio da cadeia** (por exemplo, a foto sobe mas a avaliação falha): a folha mostra o que já foi salvo com ✓, mantém o restante preenchido e o botão vira "Tentar novamente", recomeçando do ponto que falhou. Cada passo já é idempotente no backend ("Você já enviou a foto final", "Você já avaliou"), então repetir não duplica nada. Mesmo assim, o frontend guarda o progresso para não refazer chamadas.
- A câmera e a compressão vêm do [plano-acao-fotos.md](plano-acao-fotos.md) (F0 e F5).
- Pelo padrão único, o **mesmo agrupamento** é aplicado depois ao prestador em [ServicoDetalhePrestador.tsx](../Frontend/src/pages/Prestador/ServicoDetalhePrestador.tsx) (chegada; foto final + avaliação + conclusão), reaproveitando as mesmas folhas.
- **Backend:** nenhum. Um endpoint agregador (`/finalizar`) seria possível, mas não é necessário e criaria outra forma de fazer a mesma coisa.

### C5. Contagem de passos (antes × depois) — colecionador, serviço mecânico

| Momento | Antes | Depois |
|---|---|---|
| Abrir o pedido | 2 (menu → "+ Serviço mecânico") | 1 ("Pedir ajuda") |
| Veículo e localização | 2 ou 3 (selecionar carro, ler a ajuda, "Usar meu GPS") | 0 (pré-selecionados; GPS automático) |
| Serviços | 1 ou mais | 1 ou mais |
| Enviar | 1 | 1 |
| Saber da proposta | 1 (recarregar a página) | 0 (atualização automática) |
| Aceitar | 1 | 1 |
| Chegada | 2 (foto inicial; confirmar) | 1 gesto (foto + confirmar) |
| Conclusão | 3 (foto final; avaliação; confirmar) | 1 gesto (foto + estrelas + concluir) |
| **Total** | **cerca de 14** | **cerca de 7** |

Para o **guincho**, o pedido cai de cerca de 6 para **2 toques** (abrir e confirmar).

---

## 8. Ordem de execução sugerida

| Etapa | Ações | Resultado |
|---|---|---|
| 1 | **P1 + L1** (tokens, CSS mobile-first, toque, fonte de 16px) | A base deixa de "quebrar" no celular |
| 2 | **P2 + L2 + L3** (componentes compartilhados, barra inferior, topo compacto, bottom sheet) | Os blocos do padrão e os dois layouts existem |
| 3 | **G1 → G2 → G3** (galeria, detalhe do carro, eventos) | Galeria em eventos e no detalhe do carro, já no modelo de detalhe |
| 4 | **C1 + C2** (pedir ajuda, atualização automática) | O colecionador solicita em 2 ou 3 toques e vê a proposta chegar sozinha |
| 5 | **C3 → C4** (próxima ação, gestos agrupados) | Chamado completo com cerca de 7 interações |
| 6 | **P3** nas telas restantes do inventário + **P4** (guia) | Aplicação inteira no mesmo padrão |

As etapas 3 e 4 já usam os componentes do P2. Assim, as telas que a banca citou são as primeiras a adotar o padrão, e as demais (etapa 6) só aplicam o que já está pronto.

**Dependências com os outros planos:**
- G1 e G2 usam a **capa** e a `fotoCapa` do plano de fotos (B2 e B4).
- C4 usa a **câmera** e a **compressão** do plano de fotos (F0 e F5).
- A linha do tempo de C3 é o mesmo componente do plano de densidade de valor (A7). Fazer uma única vez.
- O botão "Pedir ajuda" e o atalho no detalhe do carro cobrem o A4 do plano de densidade de valor.

---

## 9. Fora do escopo e cuidados

- **Vitrine pública de carros:** terá um **plano próprio** (outro parágrafo da banca). Este plano só garante que `Galeria`, `CardComCapa` e os modelos de lista e detalhe sirvam para ela sem adaptação.
- **Notificação em tempo real** (WebSocket, SSE ou push): a atualização a cada 15s (C2) resolve para a apresentação. O tempo real fica como trabalho futuro.
- **Aplicativo nativo:** não é necessário. Opcionalmente, um **PWA mínimo** (`manifest.webmanifest` + ícones + `display: standalone`), sem service worker nem modo offline, permite "Adicionar à tela inicial" e reforça o argumento de que o público usa o celular. Esforço **P**.
- **Regras de negócio:** nenhuma muda. A ordem foto → avaliação → conclusão e a dupla confirmação continuam garantidas pelo backend. O frontend só reduz os toques.
- **Rate limit:** confirmar que o [RateLimitFilter.java](../src/main/java/carshub/com/br/backend/config/RateLimitFilter.java) suporta a atualização automática (C2) com várias abas abertas. Ajustar o intervalo se necessário.
- **Permissão de GPS:** pedir no momento em que o usuário abre "Pedir ajuda" é aceitável porque a intenção é clara. Nunca pedir no carregamento do site.

---

## 10. Como testar

- **Chrome DevTools, modo dispositivo:** 360 × 800 (Android), 390 × 844 (iPhone 12–15), 768 × 1024 (tablet) e 1280 × 800 (computador).
- **Aparelhos reais**, pelo menos um Android e um iPhone, acessando o Vite pela rede local (`npm run dev -- --host`). O GPS e a câmera exigem HTTPS ou `localhost` em alguns navegadores. Usar um túnel (ngrok ou similar) se precisar.
- **Lighthouse no modo mobile** nas telas: catálogo, detalhe do evento, detalhe do carro, pedir ajuda e detalhe do serviço.
- **Roteiro manual cronometrado** do chamado completo (antes × depois), registrando o número de toques para levar à banca.

---

## 11. Roteiro de demonstração para a banca (cerca de 60 segundos)

| Tempo | Tela | O que mostrar |
|---|---|---|
| 0–10 s | Lado a lado: celular e computador | O mesmo catálogo em 1 coluna com barra inferior × grade de 3 colunas com navbar |
| 10–25 s | Celular: detalhe do carro | Galeria no topo, deslizar, tela cheia, destaques em pílulas, especificações recolhíveis |
| 25–40 s | Celular: "Pedir ajuda" → Guincho | Carro e GPS já preenchidos → 2 toques → a proposta aparece sozinha |
| 40–60 s | Celular: detalhe do serviço | Cartão "Próxima ação" → "Prestador chegou" (câmera + confirmar) → "Finalizar" (foto + estrelas + concluir) |

---

## 12. Checklist de aceite

**Padrão único**
- [ ] Tokens de cor, espaço, tipografia, raio, sombra e toque centralizados em `:root`, sem valores soltos nas classes novas.
- [ ] Componentes compartilhados (`BotaoLink`, `CabecalhoPagina`, `CardComCapa`, `CardResumo`, `Galeria`, `ListaDados`, `SecaoRecolhivel`, `BarraAcaoFixa`, `Abas`) criados e em uso.
- [ ] **Todas** as telas do inventário (P3) seguem um dos cinco modelos de página.
- [ ] Nenhum `style={{}}` restante nas páginas.
- [ ] `docs/padrao-ui.md` publicado com os tokens, componentes, modelos e regras.

**Layout**
- [ ] O CSS é mobile-first (`min-width`), sem a antiga `@media (max-width: 640px)`.
- [ ] Nenhuma tela tem rolagem horizontal a 360px.
- [ ] Os campos têm 16px ou mais no celular, sem zoom ao focar no iPhone.
- [ ] Botões, abas e links de navegação têm pelo menos 44px de altura.
- [ ] No celular: barra superior compacta + barra inferior fixa por perfil, respeitando a área segura do iPhone.
- [ ] No computador: navbar horizontal numa linha só, conteúdo limitado a 1080px.
- [ ] Os modais viram bottom sheet no celular.
- [ ] A ação principal fica fixa no rodapé nos formulários longos (celular).
- [ ] Os estilos inline foram migrados para classes.

**Galeria**
- [ ] O componente `Galeria` desliza com o dedo, tem tela cheia, miniaturas (computador), teclado e `loading="lazy"`.
- [ ] O detalhe do carro mostra a galeria primeiro, destaques e especificações recolhíveis.
- [ ] O catálogo mostra cards com capa e selo de data (1 coluna no celular, grade no computador).
- [ ] Os detalhes do evento (público e do organizador) usam a `Galeria` e a mesma estrutura do detalhe do carro.
- [ ] "Meus eventos" (organizador) usa o mesmo `CardComCapa` do catálogo.

**Chamado**
- [ ] Existe uma entrada única, "Pedir ajuda", com o seletor Mecânico/Guincho, acessível em 1 toque.
- [ ] Veículo pré-selecionado e GPS automático, sem campos de latitude e longitude visíveis.
- [ ] O detalhe do serviço se atualiza sozinho (15s, pausando com a aba oculta) e avisa quando o status muda.
- [ ] O cartão "Próxima ação" mostra só a ação do status atual.
- [ ] "Prestador chegou" e "Finalizar serviço" são gestos únicos, que se recuperam de falha no meio da cadeia.
- [ ] O mesmo agrupamento existe no lado do prestador.
- [ ] Contagem cronometrada: o colecionador **solicita** um mecânico em até 3 toques e um guincho em 2; o fluxo completo tem cerca de 7 interações.

**Qualidade**
- [ ] Lighthouse mobile: Acessibilidade ≥ 90 e Boas práticas ≥ 90 nas telas principais.
- [ ] Testado em pelo menos um Android e um iPhone reais.
