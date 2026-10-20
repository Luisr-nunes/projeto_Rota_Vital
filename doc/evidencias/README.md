# Evidências do pipeline — Desafio de Infra (E3)

Jira: PI2-223 · Responsável: Micaella Maria Barbosa Cabral · Gerado em 20/10/2026 no PR [#36](https://github.com/Luisr-nunes/projeto_Rota_Vital/pull/36)

| # | Evidência | Arquivo | Link |
|---|---|---|---|
| 1 | Execução **vermelha**: teste falhando em um pull request | [`pipeline-vermelho.png`](pipeline-vermelho.png) | [run #58](https://github.com/Luisr-nunes/projeto_Rota_Vital/actions/runs/39456503024) |
| 2 | Execução **verde** no mesmo PR e no merge na `main`, seguida do deploy no Render | [`pipeline-verde.png`](pipeline-verde.png) | [run #59](https://github.com/Luisr-nunes/projeto_Rota_Vital/actions/runs/39457625160) · [run #60](https://github.com/Luisr-nunes/projeto_Rota_Vital/actions/runs/39457885505) |
| 3 | Sistema no ar | [`health-producao.png`](health-producao.png) | https://rota-vital-jnmx.onrender.com/actuator/health |
| 4 | Rollback: versões anteriores disponíveis e procedimento | [`rollback-render.png`](rollback-render.png) | Render → Events → Rollback |

## O que aconteceu no PR #36

| Commit | O que fez | CI |
|---|---|---|
| `b3ba90a` | `ci.yml` passa a rodar em `pull_request` para a `main`, além do `push` na `main` | — |
| `a5a97d1` | Erro proposital: `<` trocado por `<=` na validação do intervalo mínimo de doação (`DoacaoService`) | **Vermelho** (run #58). `DoacaoServiceTest.exatamenteNoIntervalo` falhou: "Faltam 0 dia(s)". 139 testes, 1 erro, BUILD FAILURE |
| `71cba44` | Regra restaurada | **Verde** (run #59) |
| `b14e58c` | Merge na `main` | **Verde** (run #60) e deploy automático no Render |

O teste unitário escrito antes pegou um erro de "um a mais" que deixaria um doador doar no dia errado. É o tipo de regressão que o pipeline existe para barrar.

## O que barra uma versão quebrada hoje

1. **No pull request:** o check `build-and-test` fica vermelho e o PR mostra a falha para quem for revisar.
2. **No Render:** o `rotavital/Dockerfile` roda `mvn -B verify` no estágio de build. Se um teste falhar, a imagem não é gerada, o deploy falha e a versão anterior continua no ar.

**Gates ligados depois das evidências (20/10, 19:07, PI2-189):**

- Proteção da `main` exigindo o check `build-and-test`. No momento do print 1 ela ainda não existia, por isso o GitHub mostrava "merging is still available"; agora um PR vermelho não pode ser mesclado.
- No Render, Auto-Deploy em "After CI Checks Pass" (antes "On Commit") e Health Check Path em `/actuator/health` (antes vazio).

## Como fazer o rollback

**Pelo Render (mais rápido):** serviço `rota-vital` → **Events** → no último deploy que funcionava, **Rollback** → confirmar → conferir `GET /actuator/health` → `{"status":"UP"}`. Depois de um rollback o Render desliga o Auto-Deploy; religar em **Settings → Auto-Deploy** quando a correção estiver na `main`.

**Pelo Git (corrige a `main`):**

```bash
git checkout main
git pull
git revert <hash-do-commit-com-problema>
git push origin main
```

O `revert` cria um commit novo que desfaz o problema, passa pelo pipeline e o Render publica a versão corrigida. Foi o que fizemos no dia 06/10: o commit `3fa67cd` quebrou o CI (run #44) e o revert `3bc8126` deixou verde de novo (run #45). Detalhes em [`doc/pipeline-deploy.md`](../pipeline-deploy.md).
