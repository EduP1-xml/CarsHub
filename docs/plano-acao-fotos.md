# Plano de ação — Fotos (limite, lote, câmera, capa e reordenação)

> **Feedback da banca:** "Fotos: subir o limite para 5 fotos, upload em lote por arrastar-e-soltar (para computadores) e upload tirando foto com a câmera do dispositivo ou em lote (com limite sendo de 1 ao máximo de fotos permitidas pela plataforma) acessando a galeria do dispositivo, capa/reordenação. É o ajuste mais barato com maior sinal de escuta."

---

## 1. Leitura do feedback

A banca pede quatro coisas para as fotos de **carros** e **eventos**:

1. **Limite de 5 fotos** (hoje são 2 por carro e 3 por evento).
2. **Arrastar e soltar** vários arquivos de uma vez no computador.
3. **No celular**, tirar a foto com a câmera **ou** escolher de 1 até o máximo permitido na galeria.
4. **Capa e reordenação**: o usuário escolhe a foto principal e a ordem das demais.

A expressão "sinal de escuta" indica o que a banca quer ver: uma mudança pequena, visível e diretamente ligada ao que ela comentou. O plano mantém o escopo enxuto e concentra o esforço no componente de upload, que é reaproveitado em todas as telas.

---

## 2. Diagnóstico no código

### 2.1 Limites fixos espalhados

| Regra | Onde está fixa |
|---|---|
| Máximo de 2 fotos por carro | [CarroService.java](../src/main/java/carshub/com/br/backend/services/CarroService.java) — 3 ocorrências da mensagem "no máximo 2 fotos" (POST, PUT e PATCH) |
| Máximo de 3 fotos por evento | [EventoService.java](../src/main/java/carshub/com/br/backend/services/EventoService.java) — 3 ocorrências da mensagem "no máximo 3 fotos" |
| `maximo={2}` e o título "Fotos (máx. 2…)" | [CarroForm.tsx](../Frontend/src/pages/Garagem/CarroForm.tsx) |
| `maximo={3}` e o título "Fotos (máx. 3…)" | [EventoForm.tsx](../Frontend/src/pages/Organizador/EventoForm.tsx) |
| Comentários "máx 2" e "máx 3" | [api/carros.ts](../Frontend/src/api/carros.ts), [api/eventos.ts](../Frontend/src/api/eventos.ts) |

Não existe uma constante única. Subir para 5 hoje exige editar oito ou mais pontos e manter backend e frontend sincronizados à mão.

### 2.2 Componente de upload atual ([UploadImagens.tsx](../Frontend/src/components/UploadImagens.tsx))

- ✅ Já aceita várias fotos (`multiple`) e valida formato e tamanho no próprio navegador.
- ❌ **Não tem arrastar e soltar.** O único caminho é o botão "Adicionar foto".
- ❌ **Não tem câmera.** Falta o atributo `capture`, então no celular não aparece a opção direta "Tirar foto".
- ❌ **Rejeita o lote inteiro** quando passa do limite. Se o usuário escolhe 4 fotos e só restam 3 vagas, nenhuma é aceita. A banca pediu "de 1 até o máximo".
- ❌ **Não tem capa nem reordenação.** As miniaturas aparecem na ordem de seleção e só podem ser removidas.
- ❌ **Vazamento de memória.** `URL.createObjectURL(arquivo)` é chamado a cada renderização e nunca é liberado com `revokeObjectURL`.
- ❌ **Sem compressão.** A foto do celular (3 a 8 MB) sobe no tamanho original. Com 5 fotos, isso pesa na rede móvel e se aproxima do limite da requisição (ver 2.4).

### 2.3 Edição de fotos (o ponto mais complexo)

| Entidade | Como a edição funciona hoje | Problema |
|---|---|---|
| Carro, PATCH | Partes `foto1` e `foto2`, trocadas por posição ([CarroService.java](../src/main/java/carshub/com/br/backend/services/CarroService.java) `aplicarFotoNoSlot`) | Não escala para 5 posições. Não permite remover uma foto, reordenar nem escolher a capa. Pode gravar `null` no meio da lista. |
| Carro, PUT | `arquivos` substitui todas as fotos; sem arquivos, vale `urlFotoCarro` do JSON | Para trocar uma foto, o usuário precisa reenviar todas. |
| Evento, PUT e PATCH | `arquivos` substitui todas; sem arquivos, vale `urlFotoEvento` do JSON | Mesmo problema. |
| Tela do carro ([CarroForm.tsx](../Frontend/src/pages/Garagem/CarroForm.tsx)) | Explica "PATCH… foto1 = posição 1… PUT (substituir tudo)" ao usuário final | Linguagem de API exposta para o usuário. |

⚠️ **Risco já existente:** no PUT, as listas `urlFotoCarro` e `urlFotoEvento` do JSON são gravadas **sem verificar se as URLs pertencem à entidade**. Qualquer URL externa pode virar foto do carro ou do evento. O novo contrato (seção 4, B3) corrige isso.

### 2.4 Persistência e limites de tamanho

- `fotosCarro` ([Carro.java](../src/main/java/carshub/com/br/backend/models/entities/Carro.java)) e `urlFotoEvento` ([Evento.java](../src/main/java/carshub/com/br/backend/models/entities/Evento.java)) são `@ElementCollection List<String>` **sem `@OrderColumn`**. O banco não guarda a ordem: ela depende da ordem de inserção e **não é garantida**. Sem uma coluna de ordem, não existe "capa" confiável.
- Em [application.yaml](../src/main/resources/application.yaml), `max-file-size` e `max-request-size` valem **100MB**. Com até 5 arquivos por requisição, o limite por requisição é atingido antes do limite por arquivo.
- `ddl-auto: update` adiciona a coluna nova automaticamente, mas **não preenche** a ordem das fotos que já existem. É preciso um script SQL.

### 2.5 Exibição

- [Garagem.tsx](../Frontend/src/pages/Garagem/Garagem.tsx) faz **uma chamada de detalhe por carro** só para buscar as fotos, porque a projeção da listagem não traz nenhuma ([CarroGaragemProjecaoDTO.java](../src/main/java/carshub/com/br/backend/models/dtos/carros/CarroGaragemProjecaoDTO.java)). Com a capa definida, basta trazer **uma URL** na projeção.
- [Catalogo.tsx](../Frontend/src/pages/Catalogo/Catalogo.tsx) já usa `fotos[0]` como capa. Com a ordem persistida, isso passa a ser a escolha do organizador.
- [CatalogoDetalhe.tsx](../Frontend/src/pages/Catalogo/CatalogoDetalhe.tsx) e [EventoDetalhe.tsx](../Frontend/src/pages/Organizador/EventoDetalhe.tsx) mostram as fotos lado a lado, enquanto [CarroDetalhe.tsx](../Frontend/src/pages/Garagem/CarroDetalhe.tsx) já usa o [CarrosselFotos.tsx](../Frontend/src/components/CarrosselFotos.tsx). Com 5 fotos, o carrossel funciona melhor.

### 2.6 Fotos únicas (fora do limite de 5, mas ganham com a câmera)

- Foto inicial e foto final do serviço ([SecaoFotoInicial.tsx](../Frontend/src/components/servicos/SecaoFotoInicial.tsx), [SecaoFotoFinal.tsx](../Frontend/src/components/servicos/SecaoFotoFinal.tsx)): são **o caso mais natural para a câmera**. O colecionador e o mecânico estão ao lado do carro, no celular.
- Foto de perfil ([AbaFoto.tsx](../Frontend/src/pages/Perfil/AbaFoto.tsx)): pode oferecer a câmera frontal (selfie).

---

## 3. Objetivo e critério de sucesso

- No **computador**, o usuário arrasta até 5 imagens para o formulário de carro ou de evento, reordena arrastando e marca uma como capa.
- No **celular**, o usuário escolhe "Tirar foto" (abre a câmera traseira) ou "Galeria" (seleciona de 1 até as vagas restantes), reordena e marca a capa.
- A **capa** aparece na garagem, no catálogo e como primeira foto do carrossel.
- A **edição** permite remover, adicionar, reordenar e trocar a capa sem reenviar as fotos que não mudaram.
- O limite **5** fica definido em **um único lugar** no backend e em **um único lugar** no frontend.

---

## 4. Ações no backend

Legenda de esforço: **P** = até meio dia · **M** = 1 a 2 dias · **G** = 3 dias ou mais.

### B1. Limite configurável e centralizado — **P**
- Criar `app.fotos.maximo: 5` em [application.yaml](../src/main/resources/application.yaml), com override por `.env` (`FOTOS_MAXIMO`), e ler o valor num único lugar (`@Value` ou `@ConfigurationProperties`).
- Trocar as 6 verificações fixas de [CarroService.java](../src/main/java/carshub/com/br/backend/services/CarroService.java) e [EventoService.java](../src/main/java/carshub/com/br/backend/services/EventoService.java) por um validador comum, por exemplo `ValidadorFotos.validarQuantidade(total)`, com a mensagem "É permitido enviar no máximo {n} fotos."

### B2. Ordem persistida (capa = posição 0) — **P**
- Adicionar `@OrderColumn(name = "ordem_Foto")` em `Carro.fotosCarro` e em `Evento.urlFotoEvento`.
- Criar o script `db/ordem-fotos.sql` (no mesmo padrão de [db/servico-localizacao-espacial.sql](../db/servico-localizacao-espacial.sql)) para:
  1. apagar as linhas com URL `null` (que podem ter sido criadas pela troca por posição);
  2. preencher `ordem_Foto` com `ROW_NUMBER() OVER (PARTITION BY id… ) - 1` nas fotos existentes.
- **Convenção:** a capa é sempre a **primeira** foto da lista. Não há campo separado de capa, então não há como a capa ficar dessincronizada da lista.

### B3. Contrato único de edição com lista ordenada — **M**
Substituir a troca por posição (`foto1`/`foto2`) e a regra "arquivos substituem tudo" por **uma lista final ordenada** enviada pelo cliente.

```
PATCH /colecionador/carros/{id}         (o mesmo vale para PUT e para /organizadores/eventos/{id})
multipart:
  dados    = { ...campos..., "fotos": ["https://.../a.jpg", "novo:0", "https://.../c.jpg", "novo:1"] }
  arquivos = [arquivo0, arquivo1]
```

Regras de validação (422 com mensagem clara):
- `fotos` ausente (`null`) → as fotos **não mudam** no PATCH. Se `arquivos` vier sozinho, os arquivos são **adicionados ao final** (compatível com o comportamento simples).
- Cada URL citada precisa **já pertencer à entidade**. Isso fecha o risco descrito em 2.3.
- Cada `novo:i` precisa apontar para um arquivo existente, e cada arquivo enviado precisa ser usado **exatamente uma vez**.
- Itens repetidos são rejeitados.
- O total não pode passar de `app.fotos.maximo`.
- A posição 0 é a capa.

Para o POST de criação nada muda: as fotos chegam em `arquivos` na ordem escolhida e a primeira é a capa. Só o limite sobe para 5.

Depois disso, remover `foto1`/`foto2` do [CarroController.java](../src/main/java/carshub/com/br/backend/controllers/CarroController.java), o método `aplicarFotoNoSlot` e o campo `urlFotoCarro`/`urlFotoEvento` como **origem livre** de URLs. O único cliente da API é o frontend do projeto, então não há consumidores externos para manter compatíveis.

### B4. Capa na listagem da garagem — **P**
- Incluir `fotoCapa` (`String`, pode ser `null`) em [CarroGaragemProjecaoDTO.java](../src/main/java/carshub/com/br/backend/models/dtos/carros/CarroGaragemProjecaoDTO.java), buscada por subconsulta da foto com `ordem_Foto = 0` na query de [CarroRepository.java](../src/main/java/carshub/com/br/backend/repositories/CarroRepository.java).
- Com isso, a garagem deixa de fazer uma chamada por carro (ver 2.5).

### B5. Tamanho de upload coerente com 5 fotos — **P**
- **Limites definidos:**
  - **20MB por foto**;
  - **100MB para o conjunto de fotos** de um envio;
  - os **demais dados** (a parte JSON `dados` e as requisições JSON comuns) **não entram nessa conta**. Eles trafegam conforme a necessidade de uso normal e têm apenas um **teto separado contra abuso** (tráfego excessivo ou tentativa de ataque).

- **Por que não basta configurar `max-request-size: 100MB`:** no Spring, `max-request-size` mede a requisição multipart **inteira** (fotos + JSON). Se ficasse em 100MB, o JSON consumiria parte da cota das fotos. A solução separa as camadas:

| Camada | Configuração | O que limita |
|---|---|---|
| Por foto | `spring.servlet.multipart.max-file-size: 20MB` | Cada parte do multipart |
| Soma das fotos | Validação na aplicação: `ValidadorFotos.validarTamanhoTotal(arquivos)` soma `MultipartFile.getSize()` e rejeita acima de **100MB** com **413** | **Só** as fotos |
| Dados do formulário | Parte `dados` com teto de **1MB**, verificado no mesmo validador. Os DTOs já limitam cada campo com `@Size` (a descrição tem no máximo 1000 caracteres), então um uso normal fica em poucos KB. | Só a parte JSON do multipart |
| Requisição multipart inteira | `spring.servlet.multipart.max-request-size: 101MB` (100MB de fotos + 1MB de dados) | Última barreira do Tomcat, só para abuso |
| Requisições JSON comuns (sem foto) | Novo filtro `LimiteCorpoRequisicaoFilter` em `config/`: rejeita com **413** um corpo `application/json` acima de **1MB**, pelo `Content-Length` ou lendo até o limite quando o cabeçalho não vier | Hoje não existe teto para JSON no Spring MVC. Fecha o envio de corpos gigantes. |

  - Todos os valores ficam em [application.yaml](../src/main/resources/application.yaml) com override por `.env`: `UPLOAD_MAX_FOTO=20MB`, `UPLOAD_MAX_TOTAL_FOTOS=100MB`, `LIMITE_DADOS_REQUISICAO=1MB`. O `max-request-size` é derivado: total de fotos + limite de dados.
  - O filtro de tamanho complementa o [RateLimitFilter.java](../src/main/java/carshub/com/br/backend/config/RateLimitFilter.java), que já limita a **frequência** de requisições. Juntos, os dois cobrem o volume (tamanho) e a repetição (quantidade).
  - Mensagens de 413 diferentes em [GlobalException.java](../src/main/java/carshub/com/br/backend/exceptions/GlobalException.java):
    - foto acima de 20MB → "A foto excede o limite de 20MB.";
    - soma das fotos acima de 100MB → "O conjunto de fotos excede o limite de 100MB por envio.";
    - dados acima do teto → "A requisição excede o tamanho permitido." (mensagem genérica, sem detalhar o limite para quem está abusando).
  - Atualizar [validarImagemLocal](../Frontend/src/api/client.ts) e o texto de ajuda do componente ("até 20MB por foto · até 100MB no total").
- **Na prática**, a compressão obrigatória (F0) deixa cada foto em cerca de 0,5 a 2 MB, então 5 fotos somam menos de 10MB. Mesmo assim, o frontend confere **depois da compressão** se cada foto tem até 20MB e se **a soma das fotos** fica em até 100MB, e avisa o usuário antes de enviar.
- Os limites do servidor continuam valendo como última barreira, inclusive para clientes que não passem pela compressão (por exemplo, testes de API ou chamadas diretas).
- **Testes (incluir no B6):**
  - 5 fotos somando pouco menos de 100MB + `dados` normal → aceito;
  - fotos somando mais de 100MB → 413;
  - parte `dados` acima de 1MB → 413;
  - JSON comum acima de 1MB em qualquer endpoint → 413;
  - JSON comum de tamanho normal → sem impacto.

### B6. Testes — **P**
- Atualizar [CarroIntegrationTest.java](../src/test/java/carshub/com/br/backend/controllers/CarroIntegrationTest.java). Os testes "PATCH troca as fotos slot a slot e valida o limite de 2" passam a testar o novo contrato.
- Casos novos para carro e evento:
  - aceita 5 fotos e rejeita 6;
  - reordena sem enviar arquivo;
  - troca a capa;
  - remove uma foto do meio;
  - mistura URLs existentes com `novo:i`;
  - rejeita URL que não pertence à entidade;
  - rejeita `novo:i` fora do intervalo ou não utilizado;
  - a ordem persiste depois de recarregar a entidade.

---

## 5. Ações no frontend

### F0. Compressão de imagens no navegador (regra do projeto) — **P**
A compressão no navegador é **obrigatória em todo upload de imagem da aplicação**. O objetivo é diminuir o uso de rede, principalmente em dados móveis.

- Criar `utils/comprimirImagem.ts`, uma função única `comprimirImagem(arquivo: File): Promise<File>`:
  - redimensiona para no máximo **1920px** no maior lado, mantendo a proporção e sem ampliar imagens menores;
  - gera **JPEG com qualidade 0,85** via `createImageBitmap(arquivo, { imageOrientation: 'from-image' })` + `canvas.toBlob`;
  - devolve um `File` com o nome trocado para `.jpg`;
  - se a versão comprimida ficar **maior** que a original (imagem pequena ou já otimizada), envia a original.
- Todos os pontos de upload usam essa função antes de validar e de enviar:
  - `EditorFotos` (carro e evento, F1);
  - [SecaoFotoInicial.tsx](../Frontend/src/components/servicos/SecaoFotoInicial.tsx) e [SecaoFotoFinal.tsx](../Frontend/src/components/servicos/SecaoFotoFinal.tsx) (F5);
  - [AbaFoto.tsx](../Frontend/src/pages/Perfil/AbaFoto.tsx) / [UploadImagens.tsx](../Frontend/src/components/UploadImagens.tsx) (foto de perfil, F5).
- A validação local ([validarImagemLocal](../Frontend/src/api/client.ts)) roda **sobre o arquivo já comprimido**: até 20MB por foto e até 100MB somando **apenas as fotos** do envio (B5). Os demais dados do formulário não entram nessa conta.
- Durante a compressão, mostrar o estado "Preparando fotos…", porque com 5 fotos de celular ela pode levar 1 ou 2 segundos.
- Registrar a regra no comentário do helper e no [leiame.txt](../leiame.txt) ("toda imagem é comprimida no navegador antes do envio").

### F1. Novo componente `EditorFotos` (substitui o `UploadImagens` nos formulários de carro e evento) — **M**

Estado interno: uma lista ordenada de itens `{ tipo: 'existente', url } | { tipo: 'novo', arquivo, previa }`. Essa lista vira o campo `fotos` + `arquivos` do contrato B3.

| Recurso | Como fazer | Detalhe |
|---|---|---|
| **Arrastar e soltar (computador)** | Área com `onDragOver`/`onDrop` lendo `dataTransfer.files`, com destaque visual ao passar o arquivo por cima | Mostrar a área só quando `matchMedia('(pointer: fine)')` for verdadeiro. Também aceitar colar com Ctrl+V (opcional, custo baixo). |
| **Galeria em lote (celular e computador)** | Botão "Escolher da galeria" → `<input type="file" multiple accept="image/jpeg,image/png">` | Lote de 1 até as vagas restantes. |
| **Câmera (celular)** | Botão "Tirar foto" → `<input type="file" accept="image/jpeg,image/png" capture="environment">` | Mostrar só em dispositivos de toque (`pointer: coarse`). Cada foto tirada entra na lista, e o botão segue disponível até o limite. |
| **Lote acima do limite** | **Aceitar as primeiras vagas restantes** e avisar: "3 de 4 fotos adicionadas — limite de 5 atingido." | Hoje o lote inteiro é rejeitado (2.2). |
| **Reordenar** | No computador: arrastar as miniaturas (Drag and Drop nativo do HTML5). Em qualquer dispositivo: botões ◀ ▶ em cada miniatura. | O arrastar nativo não funciona bem no toque (iOS). Os botões resolvem com custo zero e são acessíveis pelo teclado. `@dnd-kit/sortable` fica como opção se sobrar tempo. |
| **Capa** | Botão "★ Definir como capa", que move a foto para a posição 0. A primeira miniatura mostra o selo "Capa". | Segue a convenção do B2. |
| **Remover** | Botão × em cada miniatura (fotos existentes e novas) | Remover uma foto existente equivale a não incluí-la em `fotos`. |
| **Contador** | "3/5 fotos", com os botões desabilitados quando a lista está cheia | O limite vem de uma constante única, `MAX_FOTOS = 5`, em `utils/` (espelho de `app.fotos.maximo`). |
| **Compressão (obrigatória)** | Toda imagem é comprimida no navegador **antes** de entrar na lista: no máximo 1920px no maior lado e JPEG com qualidade 0,85 (`createImageBitmap` + `canvas.toBlob`) | Objetivo: **reduzir o uso de rede da aplicação**. Reduz 3 a 8 MB para cerca de 0,5 a 1,5 MB. Converte para JPEG e corrige a orientação EXIF. O nome do arquivo precisa terminar em `.jpg`, porque o backend valida pela extensão. Ver F0. |
| **Progresso** | `onUploadProgress` do axios nos envios multipart, com uma barra "Enviando fotos… 60%" | Importante no celular com 5 fotos. |
| **Memória** | Criar a prévia (`createObjectURL`) **uma vez** por arquivo e chamar `revokeObjectURL` ao remover o item ou desmontar o componente | Corrige o vazamento descrito em 2.2. |
| **Acessibilidade** | `aria-label` nos botões, área de soltar também clicável, `aria-live` para os avisos de limite | — |

> Por que `accept="image/jpeg,image/png"` e não `image/*`: no iPhone, quando o `accept` lista JPEG, o Safari converte HEIC para JPEG automaticamente. A compressão também gera JPEG, mas com isso o backend nunca recebe HEIC.

### F2. Integração no formulário de carro — **P**
- Em [CarroForm.tsx](../Frontend/src/pages/Garagem/CarroForm.tsx): remover o bloco de troca por posição (`foto1`/`foto2`, `setFoto1`/`setFoto2`) e o texto "PATCH… PUT (substituir tudo)". Usar um único `<EditorFotos>` em criação e edição.
- Em [api/carros.ts](../Frontend/src/api/carros.ts): `atualizarCarroParcial` e `substituirCarro` passam a receber `fotos` (a lista ordenada) + `arquivos`.
- Em [types/api.ts](../Frontend/src/types/api.ts): adicionar `fotos?: string[]` aos DTOs de PUT e PATCH e `fotoCapa` ao item da garagem.

### F3. Integração no formulário de evento — **P**
- Em [EventoForm.tsx](../Frontend/src/pages/Organizador/EventoForm.tsx): trocar `UploadImagens` por `EditorFotos` e remover o aviso "As fotos selecionadas substituirão todas as atuais".
- Em [api/eventos.ts](../Frontend/src/api/eventos.ts): mesmo ajuste de assinatura do F2.

### F4. Exibição com capa — **P**
- [Garagem.tsx](../Frontend/src/pages/Garagem/Garagem.tsx): mostrar `fotoCapa` no card e **remover o `useEffect` que busca o detalhe de cada carro**. O carrossel completo continua no detalhe.
- [CatalogoDetalhe.tsx](../Frontend/src/pages/Catalogo/CatalogoDetalhe.tsx) e [EventoDetalhe.tsx](../Frontend/src/pages/Organizador/EventoDetalhe.tsx): trocar a galeria lado a lado pelo [CarrosselFotos.tsx](../Frontend/src/components/CarrosselFotos.tsx) com `abrirEmNovaAba`.
- [Catalogo.tsx](../Frontend/src/pages/Catalogo/Catalogo.tsx): nenhuma mudança de código (`fotos[0]` já é a capa), apenas conferir o resultado.

### F5. Câmera nas fotos únicas — **P**
- [SecaoFotoInicial.tsx](../Frontend/src/components/servicos/SecaoFotoInicial.tsx) e [SecaoFotoFinal.tsx](../Frontend/src/components/servicos/SecaoFotoFinal.tsx): no celular, mostrar dois botões ("Tirar foto" com `capture="environment"` e "Galeria"), usando a mesma compressão do F0.
- [AbaFoto.tsx](../Frontend/src/pages/Perfil/AbaFoto.tsx) via [UploadImagens.tsx](../Frontend/src/components/UploadImagens.tsx) no modo `unico`: botão "Tirar selfie" com `capture="user"`.
- As regras de negócio não mudam: continua 1 foto, e a foto inicial continua imutável (422).

---

## 6. Ordem de execução sugerida

| Etapa | Ações | Resultado |
|---|---|---|
| 1 | **B1 + B2 + B5** | Limite 5 centralizado, ordem persistida e tamanhos coerentes |
| 2 | **B3 + B6** | Contrato de edição seguro e testado |
| 3 | **F0 + F1** | Compressão obrigatória e componente pronto (arrastar e soltar, galeria, câmera, capa, reordenação) |
| 4 | **F2 + F3** | Carro e evento usando o novo componente |
| 5 | **B4 + F4** | Capa visível na garagem e no catálogo, sem as chamadas por carro |
| 6 | **F5** | Câmera também nas fotos de serviço e de perfil |

**Atalho para apresentar logo:** as etapas 1, 3 e 4 sem o B3, enviando sempre a lista completa via PUT, já mostram o limite de 5, o arrastar e soltar, a câmera e a capa no **cadastro**. O B3 completa a **edição** sem reenvio de fotos.

---

## 7. Roteiro de demonstração (cerca de 30 segundos)

| Tempo | Dispositivo | O que mostrar |
|---|---|---|
| 0–10 s | Computador | Novo carro → arrastar 6 fotos da pasta → 5 entram e aparece o aviso de limite → arrastar a 3ª para o início → selo "Capa" |
| 10–20 s | Celular | Editar o carro → "Tirar foto" (câmera abre) → "Galeria" com 2 selecionadas → ◀ ▶ para reordenar |
| 20–30 s | Computador | Garagem com a nova capa no card → detalhe com carrossel de 5 fotos → catálogo com a capa do evento |

> Para testar a câmera no celular durante o desenvolvimento, é preciso HTTPS ou um túnel (por exemplo `vite --host` com certificado local, ou ngrok). O `capture` funciona com HTTP numa rede local, mas alguns navegadores restringem recursos fora de um contexto seguro.

---

## 8. Fora do escopo e cuidados

- **Apagar arquivos órfãos do storage** (fotos removidas ou substituídas): o [FileStorageService.java](../src/main/java/carshub/com/br/backend/services/file/FileStorageService.java) não tem operação de exclusão. Registrar como melhoria futura; não bloqueia a entrega.
- **Recorte e edição de imagem** (crop, rotação manual): fora do escopo.
- **Formatos novos** (HEIC, WEBP) no backend: desnecessários com a compressão para JPEG no navegador.
- **Foto inicial imutável** do serviço: regra mantida (422). O F5 muda apenas a forma de capturar a foto.
- **Migração:** rodar `db/ordem-fotos.sql` **antes** de subir o backend com `@OrderColumn`. Sem isso, as linhas antigas ficam com `ordem_Foto` nula, o que gera erro ao carregar a coleção.

---

## 9. Checklist de aceite

- [ ] O limite 5 vem de `app.fotos.maximo` no backend e de `MAX_FOTOS` no frontend, sem outros números fixos.
- [ ] Carro e evento aceitam 5 fotos e rejeitam 6, com mensagem clara (frontend e backend).
- [ ] No computador, arrastar vários arquivos adiciona as fotos, e um lote acima do limite aceita as vagas restantes e avisa.
- [ ] No celular, "Tirar foto" abre a câmera traseira e "Galeria" permite selecionar várias fotos.
- [ ] **Toda** imagem enviada pela aplicação (carro, evento, serviço e perfil) passa por `comprimirImagem` antes do envio, e fotos típicas de celular ficam abaixo de 2 MB.
- [ ] O backend limita cada foto a 20MB (`max-file-size`) e **a soma das fotos** a 100MB (validação na aplicação). Os dados do formulário não consomem essa cota.
- [ ] A parte `dados` do multipart e as requisições JSON comuns têm um teto separado contra abuso (1MB, configurável), com resposta 413. O `max-request-size` é 101MB (fotos + dados).
- [ ] O frontend valida os limites de foto (20MB cada, 100MB no total) sobre os arquivos já comprimidos.
- [ ] Dá para reordenar arrastando (computador) e com ◀ ▶ (qualquer dispositivo).
- [ ] "Definir como capa" move a foto para a primeira posição, e o selo "Capa" aparece.
- [ ] Editar permite remover, adicionar, reordenar e trocar a capa sem reenviar as fotos que não mudaram.
- [ ] O backend rejeita URLs que não pertencem ao carro ou ao evento.
- [ ] A ordem persiste depois de recarregar a página.
- [ ] A garagem mostra a capa sem uma chamada de detalhe por carro.
- [ ] O detalhe do evento (público e do organizador) usa o carrossel.
- [ ] As fotos inicial e final do serviço e a foto de perfil oferecem a câmera no celular.
- [ ] Nenhuma tela mostra "PATCH", "PUT", "slot" ou "foto1/foto2" para o usuário.
- [ ] Testes de integração atualizados e passando.
