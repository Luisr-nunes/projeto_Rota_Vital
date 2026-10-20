# Pipeline de CI/CD e Deploy — Rota Vital

**Disciplina:** Infraestrutura de Software (SO) / RSD
**Arquivos envolvidos:** [`.github/workflows/ci.yml`](../.github/workflows/ci.yml), [`rotavital/Dockerfile`](../rotavital/Dockerfile), [`rotavital/src/main/resources/application-prod.yml`](../rotavital/src/main/resources/application-prod.yml)

Este documento descreve o caminho do código desde o `git push` até a aplicação no ar no **Render**: o workflow do GitHub Actions, o fluxo de deploy, as variáveis de ambiente de produção e como reverter um deploy com problema.

---

## 1. Visão geral

```text
 pull request para a main
      │
      ▼
 GitHub Actions — job build-and-test (Java 17, mvn package + mvn test)
      │  vermelho: o PR fica marcado como falho (ex.: run #58)
      │  verde: pode ser mesclado (ex.: run #59)
      ▼
 merge / push na main
      ├──> GitHub Actions roda de novo (badge do README)
      └──> Render — auto-deploy a cada commit (Docker)
             clona o repositório → docker build (rotavital/Dockerfile, roda mvn -B verify)
               │  teste falhou: build falha, a versão anterior continua no ar
               ▼  passou
             nova instância sobe → tráfego vai para a nova versão
      │
      ▼
 Conferência: GET /actuator/health → {"status":"UP"}
```

---

## 2. Workflow do GitHub Actions

**Arquivo:** `.github/workflows/ci.yml` · **Gatilhos:** `pull_request` para a `main` e `push` na `main` (desde o PR #36, 20/10/2026) · **Runner:** `ubuntu-latest` · **Job:** `build-and-test`

| # | Etapa | O que faz |
|---|-------|-----------|
| 1 | **Checkout do código** (`actions/checkout@v4`) | Baixa o repositório na versão do commit que disparou o workflow. |
| 2 | **Configurar Java 17** (`actions/setup-java@v4`, distribuição `temurin`, `cache: maven`) | Instala o JDK 17 (mesma versão do `pom.xml` e do Dockerfile) e guarda em cache o repositório local do Maven (`~/.m2`), deixando os próximos builds mais rápidos. |
| 3 | **Compilar e testar** (diretório `./rotavital`) | Compila o projeto e executa os testes unitários e de integração (JUnit 5 + Spring Boot Test, banco H2 em memória). Se algum teste falhar, o job falha e o commit fica marcado com ❌. |
| 4 | **Badge de status** | O README exibe o badge do workflow (`ci.yml`, branch `main`). Ele fica verde quando o último run passou e vermelho quando falhou — é o "semáforo" que a equipe olha antes de considerar uma entrega pronta. |

Badge usado no README:

```markdown
![CI/CD](https://img.shields.io/github/actions/workflow/status/Luisr-nunes/projeto_Rota_Vital/ci.yml?branch=main&style=for-the-badge&logo=github&label=CI%2FCD)
```

### 2.1 Comando de build: `mvnw verify`

A etapa 3 deve rodar o **Maven Wrapper** com a fase `verify`:

```bash
cd rotavital
./mvnw -B verify
```

- `./mvnw` garante que o CI usa a mesma versão do Maven que a equipe (3.9.16, definida em `rotavital/.mvn/wrapper/maven-wrapper.properties`), sem depender do Maven instalado no runner.
- `verify` executa, em um único comando, compilação → testes → empacotamento do `.jar` → verificações finais. Um único passo verde significa "compila, passa nos testes e gera o artefato".
- `-B` (batch mode) deixa o log limpo, sem barras de progresso.

> **Situação atual do `ci.yml`:** hoje o workflow ainda usa duas etapas com o Maven do runner — `mvn package -DskipTests` e depois `mvn test`. O resultado é equivalente, mas o alvo é trocar as duas pelo passo único abaixo. O `mvnw` precisa estar com permissão de execução no Git (`git update-index --chmod=+x rotavital/mvnw`).
>
> ```yaml
>       - name: Build e testes (mvnw verify)
>         working-directory: ./rotavital
>         run: ./mvnw -B verify
> ```

---

## 3. Fluxo de deploy

| Passo | Onde | O que acontece |
|-------|------|----------------|
| 1. **Push na `main`** | GitHub | Merge do PR (ou push direto) na `main`. Isso dispara o workflow e notifica o Render pelo webhook do repositório. |
| 2. **CI** | GitHub Actions | O job `build-and-test` roda no PR e de novo no push na `main`. O resultado aparece no PR e no badge. Hoje ele **não** bloqueia o Render sozinho (ver a configuração abaixo). |
| 3. **Auto-deploy no Render** | Render | O Auto-Deploy está em **"On Commit"**: todo commit na `main` inicia um deploy. Ele clona o repositório e faz o `docker build` do `rotavital/Dockerfile` (multi-stage: Maven + JDK 17 roda `mvn -B verify`, que compila e executa os testes → imagem final só com JRE 17). Se um teste falhar, o build falha e a versão anterior continua no ar. |
| 4. **Subida da nova versão** | Render | O container sobe com `SPRING_PROFILES_ACTIVE=prod` (definido no Dockerfile), lê as variáveis de ambiente do banco e escuta na porta 8080. |
| 5. **Health check** | Render / equipe | O campo **Health Check Path** do serviço ainda está vazio, então o Render só confere se a porta abriu. A equipe confere `GET /actuator/health` → `{"status":"UP"}` depois de cada deploy. Com o campo preenchido, o Render passa a só trocar o tráfego quando essa resposta for 200. |

**Configuração do Web Service no Render**

| Campo | Hoje (conferido na API do Render em 20/10/2026) | Alvo |
|-------|-------|-------|
| Runtime | Docker | Docker |
| Branch | `main` | `main` |
| Docker Context | `rotavital` | `rotavital` |
| Dockerfile Path | `./rotavital/Dockerfile` | `./rotavital/Dockerfile` |
| Auto-Deploy | On Commit | After CI Checks Pass |
| Health Check Path | (vazio) | `/actuator/health` |

Os dois ajustes da coluna **Alvo** e a proteção da `main` exigindo o check `build-and-test` só podem ser feitos por quem administra o repositório e o serviço no Render.

Para conferir manualmente depois do deploy:

```bash
curl -s https://<url-do-servico>.onrender.com/actuator/health
# {"status":"UP"}
```

---

## 4. Variáveis de ambiente (produção)

Configuradas em **Render → serviço → Environment**. Os valores **não** ficam no repositório.

| Variável | Obrigatória | Onde é usada | Exemplo de formato (sem valor real) |
|----------|-------------|--------------|-------------------------------------|
| `SPRING_PROFILES_ACTIVE` | Sim (já vem do Dockerfile) | Ativa `application-prod.yml` | `prod` |
| `SPRING_DATASOURCE_URL` | Sim | URL JDBC do PostgreSQL | `jdbc:postgresql://<host-interno>:5432/<banco>` |
| `SPRING_DATASOURCE_USERNAME` | Sim | Usuário do banco | `<usuario>` |
| `SPRING_DATASOURCE_PASSWORD` | Sim (**secreta**) | Senha do banco | `<senha>` |
| `HEMOREDE_SEED_ENABLED` | Não | Liga/desliga a carga de dados sintéticos (`hemorede.seed.enabled`) | `true` ou `false` |

Cuidados:

- O Render mostra a URL do Postgres no formato `postgres://usuario:senha@host/banco`. O Spring precisa do formato **JDBC**: `jdbc:postgresql://host:5432/banco`, com usuário e senha nas variáveis separadas.
- Use a **Internal Database URL** (rede privada do Render), nunca a externa — o banco não deve ficar acessível pela internet.
- Nenhuma credencial vai para o `application-prod.yml`, para o README ou para o histórico do Git. Se uma senha vazar, troque-a no Render e atualize a variável.

---

## 5. Como reverter um deploy

**Opção A — Rollback pelo Render (mais rápida, sem mexer no código)**

1. Render → serviço Rota Vital → aba **Events** (ou **Deploys**).
2. Localize o último deploy que estava funcionando.
3. Clique em **Rollback** e confirme.
4. Confira o health check: `curl .../actuator/health` → `{"status":"UP"}`.
5. Atenção: após um rollback o Render pode desativar o Auto-Deploy, para que o próximo push não publique o código com problema de novo. Reative em **Settings → Auto-Deploy** depois que a correção estiver na `main`.

**Opção B — Reverter o commit no Git (corrige a `main`)**

```bash
git checkout main
git pull
git revert <hash-do-commit-com-problema>
git push origin main
```

O `git revert` cria um novo commit que desfaz a mudança. O push dispara o CI de novo e, com ele verde, o Render publica automaticamente a versão corrigida. Não use `git push --force` na `main`.

**Quando usar cada uma:** a opção A tira o problema do ar em segundos; a opção B deixa o histórico da `main` correto. Em um incidente, faça A primeiro e B logo em seguida.

---

## 6. Checklist rápido

- [ ] CI verde no último commit da `main` (badge verde no README)
- [ ] Deploy concluído no Render (status **Live**)
- [ ] `GET /actuator/health` retorna `{"status":"UP"}`
- [ ] Variáveis de ambiente presentes no Render, nenhuma no repositório