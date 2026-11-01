# Guia: Configurar o CarsHub numa máquina nova (Windows 11)

> Feito para leigos em Docker. Siga os passos na ordem.
> O projeto já vem com `Dockerfile` e `docker-compose.yaml`, então **não é preciso instalar Java, Maven nem configurar nada manualmente** — o Docker cuida de tudo.

---

## 0. Como a aplicação funciona

O projeto usa **2 containers** (aplicações isoladas que o Docker gerencia):

| Container      | O que é                                   | Porta no seu PC |
|----------------|-------------------------------------------|-----------------|
| `carshub-db`   | Banco de dados MySQL 8                    | **3307**        |
| `carshub-backend` | A API do CarsHub (Spring Boot/Java 21) | **8080**        |

- O banco fica em `localhost:3307` (usuário `root`, senha `root`, banco `carshub`).
- A API responde em `http://localhost:8080`.
- Os dados do banco ficam guardados num "volume" chamado `mysql_data` — mesmo se o container for apagado, os dados continuam.
- As fotos enviadas pela aplicação ficam na pasta `uploads` dentro da pasta do projeto.

### Os dois jeitos de rodar (leia para não se confundir)

O arquivo `docker-compose.yaml` define 2 serviços, mas dá para usar só parte deles:

- **Modo escolhido para a máquina nova — tudo no Docker:** `docker compose up -d` sobe os **dois** containers (banco + aplicação). Não precisa de Java nem Maven instalados. É o modo seguido neste guia.
- **Modo alternativo (como o computador antigo rodava):** `docker compose up -d db` sobe **só o banco**, e a aplicação roda pelo IntelliJ conectando em `localhost:3307`. Para esse modo é preciso ter um JDK 21 configurado no IntelliJ.

> Se no computador antigo você via apenas 1 container no Docker, era porque ele usava o modo alternativo (a aplicação subia pelo IntelliJ, fora do Docker). Na máquina nova, com o modo escolhido, os 2 containers ficarão visíveis no Docker Desktop.

---

## 1. Verificações antes de começar

### 1.1 Virtualização
O Docker Desktop precisa de virtualização habilitada na BIOS. Em notebooks Dell (como o Vostro 3520) normalmente **já vem habilitada de fábrica**. Se o Docker Desktop reclamar, entre na BIOS (F2 ao ligar) e procure **Virtualization Technology (VT-x)** → Enabled.

### 1.2 Docker Desktop instalado com WSL2
- Abra o **Docker Desktop** (ícone da baleia).
- Se aparecer a tela de boas-vindas, aceite os termos e escolha **"Use WSL 2 based engine"** (opção recomendada).
- Se der erro dizendo que o WSL precisa de atualização, abra o PowerShell **como administrador** e rode:
  ```powershell
  wsl --update
  wsl --install
  ```
  Depois **reinicie o PC**.
- Espere o Docker Desktop ficar com status verde (rodando). Dica: na barra de tarefas, o ícone da baleia deve estar firme, sem animação.

### 1.3 Verifique se o Docker está respondendo
Abra o PowerShell e digite:
```powershell
docker --version
docker compose version
```
Se aparecerem números de versão, está tudo certo.

---

## 2. Importante sobre o MySQL instalado na máquina

Você tem um MySQL instalado no Windows. **Não precisamos dele** — a aplicação usa o MySQL do Docker. Recomendo **desativar o serviço** para liberar memória (importante em máquina com 8 GB) e evitar confusão:

1. Pressione `Win + R`, digite `services.msc` e Enter.
2. Procure o serviço **MySQL80** (ou nome parecido).
3. Clique com o botão direito → **Parar**.
4. Clique de novo com o botão direito → **Propriedades** → em "Tipo de inicialização" escolha **Manual** → OK.

> O **MySQL Workbench** pode continuar instalado — ele é útil para olhar o banco. Para conectar: host `127.0.0.1`, porta **3307**, usuário `root`, senha `root`.

---

## 3. Passo a passo para rodar a aplicação

### Passo 1 — Extraia o zip num lugar simples
Extraia o projeto para uma pasta **fora do OneDrive** (OneDrive pode atrapalhar o Docker), por exemplo:

```
C:\CarsHub
```

A pasta deve conter: `Dockerfile`, `docker-compose.yaml`, `pom.xml`, `src`, etc.

> Se a pasta `uploads` não existir dentro dela, crie uma pasta vazia chamada `uploads`.

### Passo 2 — Abra o PowerShell na pasta do projeto
- Abra o Explorador de Arquivos, entre em `C:\CarsHub`.
- Clique na **barra de endereço**, digite `powershell` e pressione Enter. O terminal já abre na pasta certa.

### Passo 3 — Suba os containers (primeira vez)
```powershell
docker compose up -d --build
```
O que isso faz:
- `--build` = compila a aplicação (baixa Maven, Java e as dependências — **na primeira vez pode demorar de 10 a 30 minutos**, dependendo da internet);
- `-d` = roda em segundo plano (você pode fechar o terminal depois).

> **Dica de memória:** na primeira build, feche o IntelliJ e o navegador se a máquina travar. Depois de pronto, pode abrir tudo de novo.

### Passo 4 — Verificar se funcionou
```powershell
docker compose ps
```
Você deve ver os dois containers com status `Up` ou `running`:

```
NAME               STATUS
carshub-db         Up
carshub-backend    Up
```

Veja o log da API para confirmar que iniciou:
```powershell
docker compose logs backend
```
Procure uma linha parecida com `Started BackendApplication`. Isso significa sucesso!

### Passo 5 — Testar
- Abra o navegador em: **http://localhost:8080**
- Para ver o banco, abra o MySQL Workbench e conecte em `127.0.0.1:3307`, usuário `root`, senha `root`.

---

## 4. Comandos do dia a dia (guarde este resumo)

| Quero...                         | Comando                                             |
|----------------------------------|-----------------------------------------------------|
| Subir a aplicação                | `docker compose up -d`                              |
| Subir e recompilar o código      | `docker compose up -d --build`                      |
| Parar a aplicação (dados mantidos) | `docker compose down`                             |
| Ver os containers                | `docker compose ps`                                 |
| Ver os logs do backend           | `docker compose logs -f backend`                    |
| Ver os logs do banco             | `docker compose logs -f db`                         |
| Reiniciar só o backend           | `docker compose restart backend`                    |
| Apagar TUDO (inclusive dados!)   | `docker compose down -v`  ⚠️ só se quiser zerar    |

- **Ao editar o código** no IntelliJ: rode `docker compose up -d --build` de novo para recompilar. (Alternativa: rode a aplicação direto pelo IntelliJ apontando para `localhost:3307` — mas o caminho do Docker é o mais simples.)
- **Todo dia ao ligar o PC**: abra o Docker Desktop (ele pode subir sozinho) e rode `docker compose up -d` na pasta do projeto. Os containers têm `restart: always/on-failure`, então costumam voltar sozinhos.

---

## 5. Migrar os dados do banco antigo (opcional)

O banco da máquina nova nasce vazio — as tabelas são criadas automaticamente pela aplicação, mas **sem os dados**. Se quiser levar os dados:

### Opção A — MySQL Workbench (mais fácil)
1. Na máquina **antiga**: conecte no `127.0.0.1:3307` → menu **Server → Data Export** → exporte o schema `carshub` (arquivos `.sql`).
2. Copie os arquivos para a máquina nova (pen drive/Google Drive).
3. Na máquina **nova**: conecte no `127.0.0.1:3307` → **Server → Data Import** → importe os arquivos.

### Opção B — Comandos
Na máquina **antiga**, dentro da pasta do projeto, rode (repare no `cmd /c`, ele evita um problema de codificação do PowerShell):
```powershell
cmd /c "docker exec carshub-db mysqldump -uroot -proot carshub > backup.sql"
```
Copie o `backup.sql` para a máquina nova e, dentro da pasta do projeto lá, rode:
```powershell
cmd /c "type backup.sql | docker exec -i carshub-db mysql -uroot -proot carshub"
```
> Importante: o container `carshub-db` precisa estar rodando na máquina nova antes do import.

---

## 6. Problemas comuns e soluções

| Sintoma | Causa provável | Solução |
|---|---|---|
| `error during connect... docker API` | Docker Desktop não está aberto | Abra o Docker Desktop e espere ficar verde |
| `port is already allocated` (porta 3307 ou 8080) | Algum programa já usa a porta | Rode `netstat -ano | findstr :3307` (e `:8080`) para achar o processo; se for o MySQL do Windows, desative o serviço (seção 2) |
| `carshub-backend` fica reiniciando no início | O banco ainda não estava pronto quando o backend subiu | Espere ~30 s e rode `docker compose restart backend` |
| Erro de WSL / "WSL 2 installation is incomplete" | WSL desatualizado | PowerShell como admin: `wsl --update` e reinicie o PC |
| Primeira build muito lenta ou travando | 8 GB de RAM é apertado para baixar+compilar | Feche IntelliJ/navegador durante a build; nas configurações do Docker Desktop (Resources) limite a memória do WSL para ~3 GB se necessário |
| Erro de rede baixando as dependências do Maven | Internet instável | Rode `docker compose build --no-cache` para tentar de novo |
| `mysql_data` ocupa muito espaço | Normal — são os dados do banco | `docker compose down -v` apaga (⚠️ perde os dados) |

---

## 7. Observações finais

- **Este zip é só o backend.** Se a aplicação tiver um frontend (o projeto espera um em `http://localhost:5173`), ele está em outro repositório — configure-o separadamente.
- **Google Maps:** a chave da API de geocodificação é opcional (a aplicação roda sem ela; só a busca de endereço/coordenadas fica limitada). Se um dia precisar, adicione `GOOGLE_MAPS_API_KEY` no arquivo `docker-compose.yaml`, na lista `environment` do serviço `backend`.
- **E-mails:** já estão configurados no código (Gmail), não precisa mexer.
- **Nada de instalar Java/Maven:** o `Dockerfile` compila e roda tudo dentro do container. IntelliJ fica só para editar o código.
