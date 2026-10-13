# Pipeline de CI/CD e Deploy — Rota Vital

**Disciplina:** Infraestrutura de Software (SO) / RSD
**Arquivos envolvidos:** [`.github/workflows/ci.yml`](../.github/workflows/ci.yml), [`rotavital/Dockerfile`](../rotavital/Dockerfile), [`rotavital/src/main/resources/application-prod.yml`](../rotavital/src/main/resources/application-prod.yml)

Este documento descreve o caminho do código desde o `git push` até a aplicação no ar no **Render**: o workflow do GitHub Actions, o fluxo de deploy, as variáveis de ambiente de produção e como reverter um deploy com problema.

---

## 1. Visão geral

```text
 git push (main)
      │
      ▼
 GitHub Actions — workflow "CI/CD - Rota Vital"
   checkout → Java 17 → build/testes Maven → status do commit (badge)
      │  CI verde
      ▼
 Render — auto-deploy (Docker)
   clona o repositório → docker build (rotavital/Dockerfile) → sobe nova instância
      │
      ▼
 Health check  GET /actuator/health → {"status":"UP"}
      │  passou
      ▼
 Tráfego vai para a nova versão (a anterior é desligada)
```

---

## 2. Workflow do GitHub Actions

**Arquivo:** `.github/workflows/ci.yml` · **Gatilho:** `push` na branch `main` · **Runner:** `ubuntu-latest` · **Job:** `build-and-test`

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
| 2. **CI verde** | GitHub Actions | O job `build-and-test` precisa terminar com sucesso. Se falhar, nada é publicado. |
| 3. **Auto-deploy no Render** | Render | Com o Auto-Deploy configurado como **"After CI Checks Pass"**, o Render só inicia o deploy depois que os checks do commit passam. Ele clona o repositório e faz o `docker build` do `rotavital/Dockerfile` (multi-stage: Maven + JDK 17 para gerar o `.jar` → imagem final só com JRE 17). |
| 4. **Subida da nova versão** | Render | O container sobe com `SPRING_PROFILES_ACTIVE=prod` (definido no Dockerfile), lê as variáveis de ambiente do banco e escuta na porta 8080. |
| 5. **Health check** | Render | O Render chama `GET /actuator/health` (campo **Health Check Path** do serviço). Só quando a resposta é `200 {"status":"UP"}` o tráfego passa para a nova versão; a versão anterior é desligada depois disso (deploy sem downtime). Se o health check não passar, o deploy é marcado como falho e a versão anterior continua no ar. |

**Configuração do Web Service no Render**

| Campo | Valor |
|-------|-------|
| Runtime | Docker |
| Branch | `main` |
| Root Directory | `rotavital` |
| Dockerfile Path | `./Dockerfile` |
| Auto-Deploy | After CI Checks Pass |
| Health Check Path | `/actuator/health` |

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