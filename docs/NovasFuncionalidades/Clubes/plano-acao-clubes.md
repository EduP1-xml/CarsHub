# Clubes de entusiastas + filtros de carro em eventos

> **Diretriz do usuário:** nenhuma linha de código é implementada sem autorização explícita, nem depois de o plano ser aprovado. Aguardar o comando "pode implementar" (ou equivalente).

## Contexto
Hoje o CarsHub tem Entusiasta/Colecionador (garagem de `Carro`) e Organizador (CRUD de `Evento`), mas não há comunidade entre entusiastas nem forma de direcionar um evento a um "tipo de carro". A feature cria **Clubes**: um entusiasta cria o clube (vira **administrador**) e define o "gosto" do clube marcando, por checkbox, as características da entidade `Carro` que importam. **O que ele não marca é irrelevante: qualquer valor passa nesse campo.** Membros colecionadores **vinculam ao clube os carros da garagem que atendem ao gosto do clube**, e a aba Carros mostra esses carros vinculados. O clube tem também uma **aba Eventos** que lista só eventos ATIVOS compatíveis com o gosto. O organizador marca as características do evento da mesma forma, e a mesma regra vale: o que ele não marca, qualquer valor passa.

Decisões já tomadas com o usuário:
- **Compatibilidade clube × evento**: tudo o que o clube exige deve ser aceito pelo evento; o evento pode aceitar coisas a mais que o clube. **O conjunto do clube está contido no do evento.**
- **Compatibilidade clube × carro**: o carro só pode ser vinculado se atender a **todas** as características marcadas pelo clube (detalhe na seção de regras).
- **Entrada no clube**: livre (clicou "Entrar", virou membro). Admin pode remover membros. Ao entrar, o colecionador pode já escolher os carros compatíveis a vincular, e pode vincular ou desvincular depois.
- **Aba Carros**: mostra **só os carros vinculados**; o resto da garagem do membro continua privado.
- **Carro vinculado que deixa de ser compatível** (admin muda o filtro do clube ou o dono edita o carro): **desvinculado automaticamente** na mesma transação.
- **Administrador**: exatamente 1 por clube; pode **transferir a administração** para outro membro.
- **Data de entrada** do membro é guardada.
- **Formato dos filtros** (mesmo modelo para clube e evento):
  - Numéricos (`ano`, `quantidadeCilindros`, `aro`, `numeroPortas`, `numeroAssentos` → Integer; `litragemMotor` → BigDecimal): **faixa mín/máx** (pontas opcionais).
  - Enums (carroceria, motor, injeção, alimentação, combustível, câmbio, tração, suspensão, freio, direção, roda, revestimento): **checkbox dos valores aceitos**.
  - Strings (`marca`, `modelo`, `cor`): **lista de valores aceitos**, um por linha, botão **+** para adicionar (e × para remover).
- Fora do filtro: `id` (pedido), `placa` (identificador único — mesma natureza do id), `colecionador`, `fotosCarro` e os booleanos `vidrosEletricos`, `arCondicionado`, `tetoSolar`.
- **Único booleano no filtro: `som`**, como **checkbox** "Exige som" (clubes com foco em som automotivo). Marcado = `true`; desmarcado = não é critério (`null`).
- **Membros e administrador não ganham id novo**: são o próprio `Entusiasta` (ou Colecionador, que compartilha o mesmo id via `@MapsId`) já existente. O membro é identificado pelo par (id do clube, id do usuário).

## Regras de compatibilidade

### Clube × Evento
Avaliada **característica por característica**; o evento é compatível com o clube se **todas** passam.

| Situação da característica | Resultado |
|---|---|
| Não marcada no clube **ou** não marcada no evento | **passa** (campo irrelevante) |
| Enum (Set), marcada nos dois | passa se **conjunto do clube ⊆ conjunto do evento** |
| String (lista), marcada nos dois | passa se **lista do clube ⊆ lista do evento** (trim + case-insensitive) |
| Faixa numérica, marcada nos dois | passa se a **faixa do clube está dentro da do evento** |
| `som`, marcado nos dois | passa |

Pontas abertas na faixa: ponta nula = infinita. Uma ponta aberta do clube só cabe no evento se a mesma ponta do evento também for aberta.

Exemplos (clube "Fuscas": marca {Volkswagen}, modelo {Fusca}, carroceria {SEDAN}, ano 1960–1985):
- Evento marca {Volkswagen, Ford}, ano 1950–1990 → **aparece** (modelo/carroceria não marcados no evento = qualquer um).
- Evento carroceria {SEDAN, COUPE} → **aparece**.
- Evento ano 1970–1980 → **não aparece** (faixa do clube sai da do evento).
- Evento modelo {Gol} → **não aparece**.
- Evento sem nenhum filtro (aberto a todos) → **aparece** em todos os clubes.

Característica "marcada" = set/lista não vazia, min ou max não nulo, `som == true`.

### Clube × Carro
A mesma ideia de "o clube cabe no outro lado" não funciona aqui ao pé da letra: o carro tem **um valor** por característica, enquanto o clube aceita um conjunto ou uma faixa. Por isso o sentido se inverte: **o carro precisa caber no gosto do clube**. Todos os campos do carro estão sempre preenchidos (são `@NotNull` na entidade), então só o lado do clube pode ter característica "não marcada".

| Característica do clube | Carro passa se |
|---|---|
| Não marcada | sempre |
| Enum (Set) | o valor do carro **está no conjunto** do clube |
| String (lista) | o valor do carro **está na lista** do clube (trim + case-insensitive) |
| Faixa numérica | o valor do carro **está dentro da faixa** (ponta nula = aberta) |
| `som` marcado | `carro.som == true` |

Exemplo (clube "Fuscas"): Fusca 1972 sedan Volkswagen → **pode vincular**; Fusca 1990 → **não** (ano fora); Gol 1980 → **não** (modelo).

## Backend

### Entidades (`models/entities/`, `ddl-auto: update` cria as tabelas)
- **`FiltroCarro`** (`@Entity`, UUID binário como as demais): campos tipados
  - `Set<String> marcas, modelos, cores` — `@ElementCollection` (`FiltroCarro_Marcas` etc.)
  - 12 `Set<TipoX>` com `@ElementCollection @Enumerated(STRING)` (`FiltroCarro_Carrocerias`, …) reutilizando os enums de `models/enums/carros/`
  - `Integer anoMin/anoMax, quantidadeCilindrosMin/Max, aroMin/Max, numeroPortasMin/Max, numeroAssentosMin/Max`; `BigDecimal litragemMotorMin/Max (precision 5, scale 3)`
  - `Boolean som` (`true` ou `null`)
  - Um único modelo compartilhado por Clube e Evento (por isso entidade própria, e não `@Embeddable` — evita conflito de nome das collection tables).
  - **Decisão: tabelas tipadas** — uma tabela filha por característica (3 de String + 12 de enum = 15), cada uma com o tipo da entidade `Carro` (`String` ou o enum correspondente). `Set` (sem duplicados) em todas.
- **`Clube`**:
  - `id`, `nome` (50, único), `descricao` (500), `dataCriacao`, `@Version versao` (mesmo padrão do `Evento`).
  - `administrador`: `@ManyToOne Entusiasta` → FK `idAdministrador_Clube` para o `id_Usuario` já existente (sem id novo). Um único admin por clube; a transferência só troca essa FK.
  - `membros`: `@OneToMany(mappedBy = "clube", cascade = ALL, orphanRemoval = true) Set<MembroClube>`.
  - `filtroCarro`: `@OneToOne(cascade = ALL, orphanRemoval = true)`, obrigatório.
  - `fotosClube`: `@ElementCollection @CollectionTable(name = "FotosClube_Clube") List<String>`, igual ao `urlFotoEvento` do Evento, de **0 a 5 fotos**. **Validação igual à do Evento**: sem anotação na entidade nem no DTO; o `ClubeService` confere `arquivos.size() > 5` antes do upload e o tamanho da lista de URLs do JSON no PUT, lançando `IllegalArgumentException("É permitido enviar no máximo 5 fotos por clube.")` → 400. Mesmo formato de `criarEvento` / `substituirFotosSeHouverArquivos`.
- **`MembroClube`** (tabela `Membro_Clube`). Volta a ser entidade porque precisa guardar a data de entrada e os carros vinculados, que uma tabela de junção simples não comporta. Continua **sem id próprio**:
  - `@EmbeddedId MembroClubeId id` → `@Embeddable` com `UUID idClube` + `UUID idEntusiasta` (chave composta).
  - `@ManyToOne @MapsId("idClube") Clube clube` e `@ManyToOne @MapsId("idEntusiasta") Entusiasta entusiasta`: as duas colunas da chave são as FKs para os registros já existentes.
  - `LocalDateTime dataEntrada` (preenchida ao entrar; o admin entra na criação).
  - `@ManyToMany Set<Carro> carrosVinculados` → `@JoinTable(name = "CarrosVinculados_Membro_Clube")` com as colunas `idClube`, `idEntusiasta` e `idCarro`. A chave dessa tabela é o trio, e ela também não tem id novo.
- **`Evento`** ([Evento.java](src/main/java/carshub/com/br/backend/models/entities/Evento.java)): novo `@OneToOne(cascade = ALL, orphanRemoval = true) FiltroCarro filtroCarro` (nullable = evento aberto a todos).

### Lógica pura
- **`services/clubes/CompatibilidadeFiltro.java`** — utilitário estático e puro, no estilo de [CalculadoraMatch.java](src/main/java/carshub/com/br/backend/services/servicos/CalculadoraMatch.java):
  - `boolean compativel(FiltroCarro clube, FiltroCarro evento)` (evento com filtro `null` → compatível)
  - `boolean carroAtende(FiltroCarro clube, Carro carro)` (regra Clube × Carro)
  - `boolean possuiAlgumCriterio(FiltroCarro)`

### DTOs (`models/dtos/clubes/`)
- `FiltroCarroDTO` (record, todos opcionais): listas de String com `@Size` por elemento igual ao `Carro` (marca/modelo 30, cor 20) e no máx. 20 itens; sets de enum (Jackson já devolve 400 para valor inválido); faixas com os mesmos `@Min/@Max/@DecimalMin/@DecimalMax` do [Carro.java](src/main/java/carshub/com/br/backend/models/entities/Carro.java); `Boolean som` (`false` normalizado para `null`).
- `ClubeRegistroDTO` (nome, descricao, `@NotNull @Valid FiltroCarroDTO filtro`, `List<String> fotosClube` sem anotação, como o `urlFotoEvento` do `EventoRegistroDTO`, para o PUT manter as atuais).
- `ClubeRespostaDTO` (id, nome, descricao, nomeAdministrador, idAdministrador, quantidadeMembros, quantidadeCarrosVinculados, souMembro, souAdministrador, filtro, fotosClube, dataCriacao).
- `MembroClubeRespostaDTO`: `idEntusiasta`, `nome`, `urlFotoPerfil`, `administrador`, `dataEntrada` e **`carrosVinculados: List<CarroVinculadoResumoDTO>`**. Para o entusiasta sem garagem a lista vem **vazia**; para o colecionador, vem com os carros que ele vinculou ao clube.
- `CarroVinculadoResumoDTO`: `idCarro`, `marca`, `modelo`, `ano`, `cor`, `fotoCapa` (sem placa).
- `EntradaClubeDTO` (corpo **opcional** do "Entrar"): `Set<UUID> idsCarros` a vincular já na entrada.
- Eventos: adicionar `@Valid FiltroCarroDTO filtroCarro` a `EventoRegistroDTO`, `EventoPutDTO`, `EventoPatchDTO` e `EventoRespostaDTO`.
- Um `FiltroCarroMapper` (componente/estático) converte DTO ↔ entidade, normalizando strings (trim, remove vazias e duplicadas case-insensitive) e validando `min ≤ max` → `RegraDeNegocioException` (422). DTO sem nenhum critério → `null` no evento; no clube → 422 ("o clube precisa de pelo menos uma característica").

### Repositórios
- `ClubeRepository`: listagem paginada com busca por nome (`LIKE`); `existsByNomeIgnoreCase`; página de clubes do entusiasta (via `MembroClube`).
- **`MembroClubeRepository`** (`JpaRepository<MembroClube, MembroClubeId>`):
  - `existsById(new MembroClubeId(idClube, idEntusiasta))`
  - página de membros do clube ordenada por `dataEntrada`
  - contagem de membros por clube via GROUP BY, para a página de clubes (padrão do `contarConfirmadasPorEventos`)
  - `buscarCarrosVinculadosDosMembros(idClube, idsEntusiastas)` em lote, para montar os `carrosVinculados` da página de membros sem N+1
  - `buscarVinculosDoCarro(idCarro)`: todos os vínculos de um carro, usado na edição e na exclusão do carro
- `CarroRepository` ([CarroRepository.java](src/main/java/carshub/com/br/backend/repositories/CarroRepository.java)):
  - `projetarCarrosVinculadosDoClube(idClube, pageable)` reutilizando `CarroGaragemProjecaoDTO` com `FROM MembroClube m JOIN m.carrosVinculados c WHERE m.id.idClube = :idClube`
  - `buscarFotosDosCarros(ids)` (mesmo padrão do `buscarFotosDosEventos`)
- `EventoRepository` ([EventoRepository.java](src/main/java/carshub/com/br/backend/repositories/EventoRepository.java)):
  - `listarCandidatosAtivos(ATIVO)`: projeção leve `(e.id, f.id)` com `LEFT JOIN e.filtroCarro f`, ordenada por `dataHoraInicio ASC`. Não instancia entidades e não dispara coleções lazy.
  - `projetarCatalogoPorIds(ids, pageable)`: mesma projeção do `projetarCatalogoAtivos`, com `e.id IN :ids`.
- **`FiltroCarroRepository`** (carregamento **em lote à mão**, padrão do `buscarFotosDosEventos`):
  - `buscarEscalares(ids)`: as colunas de faixa + `som` de vários filtros em 1 consulta.
  - Uma consulta por coleção, `SELECT f.id, v FROM FiltroCarro f JOIN f.marcas v WHERE f.id IN :ids` (e o mesmo para cada uma das outras 14), devolvendo `List<Object[]>`.
  - Total fixo de **16 consultas**, qualquer que seja o número de filtros.

### Services
- **`ClubeService`**:
  - **Criar** (multipart): admin = logado, já inserido como `MembroClube` com `dataEntrada = agora`. As fotos (0–5) vão por `FileStorageService.salvarArquivo(arquivo, "entusiasta", idAdmin)` depois da validação.
  - **Listar** todos (com `souMembro`), **listar meus** e **detalhar**.
  - **Editar** (só admin): PUT multipart de nome, descrição, filtro e fotos. Com arquivos, substitui todas as fotos; sem arquivos, vale a lista `fotosClube` do JSON, como no `substituirFotosSeHouverArquivos` do Evento. **Se o filtro mudou**, reavalia com `carroAtende` todos os carros vinculados do clube e **desvincula os incompatíveis** na mesma transação. O `ClubeRespostaDTO` informa `carrosDesvinculados` (quantidade).
  - **Excluir** (só admin): a cascata leva membros, vínculos, filtro e fotos.
  - **Entrar**: recebe `EntradaClubeDTO` opcional. 409 se já for membro (`SolicitacaoDuplicadaException`). Grava `dataEntrada`. Cada carro informado passa pela validação de vínculo abaixo; se algum falhar, a entrada inteira é recusada, sem membro pela metade.
  - **Sair**: remove o `MembroClube`, e os vínculos dos carros dele saem junto. **O admin não pode sair** sem antes transferir a administração ou excluir o clube → 422 ("Transfira a administração para outro membro ou exclua o clube").
  - **Remover membro** (só admin; não pode remover a si mesmo): remove o membro e os vínculos dele.
  - **Transferir administração** (só o admin atual): o destino precisa ser membro (422 se não for) e não pode ser o próprio admin (422). Troca `clube.administrador` com o `@Version` do clube protegendo contra duas transferências simultâneas → 409. O admin anterior continua membro.
  - **Vincular carro** (só o próprio membro): o carro precisa ser do usuário logado (403, mesma checagem do `verificarPropriedadeDoCarro`), atender ao filtro (`carroAtende` → 422 "Este carro não atende ao gosto do clube") e não estar vinculado já (409).
  - **Desvincular carro** (o próprio membro).
  - **Meus carros compatíveis**: lista os carros da garagem do logado que passam em `carroAtende`, com a marcação de quais já estão vinculados. Alimenta a seleção na tela de entrar/vincular.
  - **Listar membros / carros vinculados / eventos**: **só membros** → `PerfilIncompativelException` 403 caso contrário.
  - Carros vinculados: projeção paginada + fotos em lote; **placa omitida (null)** por privacidade. Para isso tornar `CarroService.mapearProjecaoParaResponse` reutilizável (package-private/estático público).
  - Eventos do clube (**decisão: opção A + lote**, compatibilidade toda em código):
    1. `listarCandidatosAtivos` → pares (idEvento, idFiltro). Evento com `idFiltro == null` (aberto a todos) **já é compatível** e não entra no lote.
    2. `CarregadorFiltroCarro` (componente novo em `services/clubes/`) recebe os ids dos filtros dos candidatos **mais o id do filtro do clube** e, com as 16 consultas do `FiltroCarroRepository`, monta um `Map<UUID, FiltroCarro>` em memória (objetos transitórios, sem lazy loading).
    3. `CompatibilidadeFiltro.compativel(filtroClube, filtroEvento)` para cada candidato; os ids compatíveis seguem a ordem do passo 1.
    4. `projetarCatalogoPorIds(idsCompativeis, pageable)` + `CatalogoEventoService.enriquecerPagina` (vagas, inscrito, fotos).
    - Custo esperado: ~22 consultas por abertura da aba, **constante**; o volume lido cresce linearmente com os eventos ativos que têm filtro. Lista de ids muito grande (milhares) → dividir o `IN` em blocos dentro do `CarregadorFiltroCarro`.
    - O mesmo `CarregadorFiltroCarro` serve para montar o `filtro` do `ClubeRespostaDTO` / `EventoRespostaDTO` em listagens (1 lote por página).
- **`CarroService`** ([CarroService.java](src/main/java/carshub/com/br/backend/services/CarroService.java)):
  - `substituirCarro` e `atualizarCarroCondicional`: depois de aplicar a edição, reavaliar os vínculos do carro (`buscarVinculosDoCarro` + `CarregadorFiltroCarro` para os filtros dos clubes, em lote) e **desvincular** dos clubes cujo filtro ele não atende mais.
  - `removerCarro`: apagar os vínculos do carro **antes** do `delete`, senão a FK da tabela `CarrosVinculados_Membro_Clube` barra a exclusão.
- **`CatalogoEventoService`** ([CatalogoEventoService.java](src/main/java/carshub/com/br/backend/services/CatalogoEventoService.java)): extrair o trecho de enriquecimento (vagas, inscrito, fotos) de `listarEventosAtivos` para um método reutilizável `enriquecerPagina(Page<CatalogoEventoProjecaoDTO>, idEntusiasta)` chamado também pelo `ClubeService`.
- **`EventoService`** ([EventoService.java](src/main/java/carshub/com/br/backend/services/EventoService.java)): mapear `filtroCarro` em `mapearRegistroDtoParaEntidade`, `mapearPutDtoParaEntidade` (PUT substitui; null limpa), `atualizarEventoCondicional` (PATCH: presente substitui; ausente mantém; filtro vazio limpa) e `mapearEntidadeParaResponse`.

### Controller + segurança
- **`ClubeController`** `@RequestMapping("/api/carshub/clubes")`, `@PreAuthorize("hasAuthority('ENTUSIASTA')")`, `@UsuarioAutenticado UUID`, paginação `pagina/tamanho` + `PaginaRespostaDTO.de(...)`:
  - `POST /` e `PUT /{id}` em multipart (`dados` + `arquivos` opcionais, máx. 5), como o `EventoController`
  - `GET /?busca=` · `GET /meus` · `GET /{id}` · `DELETE /{id}`
  - `POST /{id}/membros` (entrar, corpo `EntradaClubeDTO` opcional) · `DELETE /{id}/membros/eu` (sair) · `DELETE /{id}/membros/{idEntusiasta}` (admin remove) · `GET /{id}/membros`
  - `PATCH /{id}/administrador/{idEntusiasta}` (transferir administração)
  - `GET /{id}/meus-carros-compativeis` · `POST /{id}/carros/{idCarro}` (vincular) · `DELETE /{id}/carros/{idCarro}` (desvincular)
  - `GET /{id}/carros` (vinculados) · `GET /{id}/eventos`
- [SecurityConfigurations.java](src/main/java/carshub/com/br/backend/config/SecurityConfigurations.java): `.requestMatchers("/api/carshub/clubes/**").hasAuthority("ENTUSIASTA")` (checklist (a) do próprio arquivo).

## Frontend (`Frontend/src`)
- `types/api.ts`: `FiltroCarroDTO`, `ClubeRegistroDTO`, `ClubeRespostaDTO`, `MembroClubeRespostaDTO`, `CarroVinculadoResumoDTO`, `EntradaClubeDTO`; `filtroCarro?: FiltroCarroDTO | null` nos DTOs de evento.
- `api/clubes.ts`: funções para todos os endpoints (padrão de [api/eventos.ts](Frontend/src/api/eventos.ts), com `multipartComDados` no POST/PUT).
- **`components/FiltroCarroForm.tsx`** (usado por clube e evento): cada característica tem um checkbox "Usar este critério"; desmarcado = "qualquer valor". Ao marcar abre:
  - enum → grade de checkboxes com os `LABEL_*` de [utils/enums.ts](Frontend/src/utils/enums.ts);
  - string → lista de inputs, botão **+** adiciona linha, **×** remove;
  - numérico → inputs Mín / Máx (`type=number`, `step=0.001` para litragem);
  - `som` → um único checkbox "Exige som" (sem checkbox extra de "usar critério").
  Agrupado em seções (Identificação, Motor, Transmissão/Chassi, Interior/Conforto) para não ficar um paredão.
- **`components/FiltroCarroResumo.tsx`**: exibe os critérios como chips (detalhe do clube e do evento do organizador).
- **`components/SeletorCarrosCompativeis.tsx`**: lista os carros de `meus-carros-compativeis` com checkbox. Usado no modal de "Entrar" (só aparece para colecionador) e na gestão "Meus carros neste clube".
- Páginas `pages/Clubes/`:
  - `Clubes.tsx` — abas "Todos" (busca + Entrar) e "Meus clubes"; botão "Criar clube"; card com a 1ª foto do clube.
  - `ClubeForm.tsx` — criar/editar (nome, descrição, `UploadImagens maximo={5}`, `FiltroCarroForm`). Na edição, avisa quando a resposta trouxer `carrosDesvinculados > 0`.
  - `ClubeDetalhe.tsx` — cabeçalho (`CarrosselFotos` com as fotos do clube, admin, nº de membros, Entrar/Sair/Editar/Excluir) + abas:
    - **Sobre**: resumo do filtro + membros com data de entrada e os carros vinculados de cada um. Para o admin, botões "Remover" e "Tornar administrador" (com confirmação via `Modal`).
    - **Carros**: cards dos carros vinculados, com dono e fotos. Para o membro colecionador, botão "Gerenciar meus carros" que abre o `SeletorCarrosCompativeis`.
    - **Eventos**: cards dos eventos compatíveis.
    - As abas de membros só aparecem para quem é membro.
- Exportar `CardEvento` de [Catalogo.tsx](Frontend/src/pages/Catalogo/Catalogo.tsx) para reusar na aba Eventos.
- [App.tsx](Frontend/src/App.tsx): rotas `/clubes`, `/clubes/novo`, `/clubes/:idClube`, `/clubes/:idClube/editar` com `entusiastaOuColecionador`.
- [NavBar.tsx](Frontend/src/components/NavBar.tsx): link "Clubes" para entusiasta/colecionador.
- [EventoForm.tsx](Frontend/src/pages/Organizador/EventoForm.tsx): nova seção "Carros aceitos no evento (opcional)" com `FiltroCarroForm`; incluir em POST/PUT e no `comparar('filtroCarro', …)` do PATCH. `EventoDetalhe.tsx` mostra `FiltroCarroResumo`.
- [CarroForm.tsx](Frontend/src/pages/Garagem/CarroForm.tsx): na edição, se a resposta indicar que o carro foi desvinculado de algum clube, mostrar um aviso (toast).

## Testes
- `CompatibilidadeFiltroTest` (unitário, puro):
  - Clube × Evento: característica não marcada em qualquer lado passa; enum e string do clube ⊆ evento passam e o inverso falha; strings case-insensitive; faixa do clube dentro da do evento passa e parcialmente fora falha; pontas abertas; `som`; evento sem filtro passa; os exemplos do clube "Fuscas".
  - Clube × Carro: valor no conjunto ou na lista passa e fora falha; valor dentro, fora e nos limites da faixa; ponta aberta; `som` exigido com carro sem som falha; característica não marcada passa.
- `ClubeIntegrationTest` (padrão de `EventoInscricaoIntegrationTest`):
  - Criação: com 0 e com 5 fotos; 6 fotos → 400 (no POST e no PUT, inclusive 6 URLs no JSON); o criador já aparece como membro, com o próprio id de usuário e `dataEntrada`.
  - Entrada e saída: entrar sem carros; colecionador entrar já vinculando carros compatíveis; carro incompatível na entrada → 422 e nenhum membro criado; carro de outro usuário → 403; 409 ao entrar de novo.
  - Vínculos: vincular e desvincular depois; vincular de novo → 409.
  - Desvínculo automático: o admin muda o filtro → os vínculos incompatíveis somem e `carrosDesvinculados` vem correto; o dono edita o carro para ficar incompatível → o vínculo some.
  - Exclusão: excluir carro vinculado funciona e o vínculo some.
  - Administração: admin não sai (422); transferir para membro funciona e o antigo admin continua membro; transferir para não-membro → 422; não-admin transferir → 403.
  - Acesso: não-membro em `/carros` e `/eventos` → 403; não-admin edita → 403.
  - Conteúdo das abas: aba eventos retorna só os compatíveis; aba carros mostra só os vinculados, com placa nula; admin remove membro e os vínculos dele somem.
- Evento com filtro: POST/PUT/PATCH persistem e devolvem `filtroCarro`; `min > max` → 422.
- `CarregadorFiltroCarro` (integração): monta corretamente vários filtros de uma vez (todas as 15 coleções + faixas + `som`) e ignora ids inexistentes.
- Segurança (checklist (d)): `/clubes` sem token → 401, organizador/prestador → 403 (em `SegurancaAutorizacaoIntegrationTest`).

## Verificação
1. `docker compose up -d db` e `./mvnw test` (todos os testes, incluindo os novos).
2. Subir o back + `npm run dev` no `Frontend`; logar como entusiasta A → criar o clube "Fuscas" com 2 fotos (marca Volkswagen, modelo Fusca, carroceria SEDAN, ano 1960–1985).
3. Colecionador B, com um Fusca 1972 e um Gol na garagem, entra no clube: o seletor oferece só o Fusca; ele vincula. A aba **Carros** mostra o Fusca sem placa, e a aba **Sobre** mostra B com a data de entrada e o Fusca.
4. B edita o Fusca para ano 1990 → recebe o aviso e o carro some do clube.
5. Logar como organizador → criar os eventos: marca {Volkswagen, Ford} + ano 1950–1990; ano 1970–1980; modelo {Gol}; sem filtros. A aba **Eventos** mostra o "Volkswagen/Ford 1950–1990" e o "sem filtros", e esconde os outros dois.
6. A transfere a administração para B; agora A pode sair, e B vê os botões de admin.
7. B remove A; A perde acesso às abas (403 tratado na tela).

## Fora de escopo
- Restringir inscrição em evento a carros compatíveis com o filtro.
- Múltiplos administradores simultâneos.
- Mostrar o filtro no catálogo público de eventos.
