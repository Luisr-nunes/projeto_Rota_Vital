# Evidências do pipeline — Desafio de Infra (E3)

Jira: PI2-223 · Responsável: Micaella Maria Barbosa Cabral

O enunciado pede quatro evidências. Cada uma fica nesta pasta, como print, e com o link da execução no GitHub Actions.

| # | Evidência | Arquivo | Link da execução | Situação |
|---|---|---|---|---|
| 1 | Execução **vermelha** que barrou o merge e o deploy (teste falhando em um pull request) | `pipeline-vermelho.png` | _link do run no Actions_ | Aguardando o pipeline com gates (PI2-189) |
| 2 | Execução **verde** que publicou no Render (merge na `main`) | `pipeline-verde.png` | _link do run no Actions_ | Aguardando o pipeline com gates (PI2-189) |
| 3 | Sistema no ar | `health-producao.png` | https://rota-vital-jnmx.onrender.com/actuator/health | Pronto para captura |
| 4 | Rollback para a versão anterior | `rollback-render.png` | Render → Events → Rollback | Procedimento abaixo |

## Como gerar as evidências 1 e 2

1. Criar uma branch curta a partir da `main` e quebrar um teste de propósito (por exemplo, trocar o status esperado em um cenário de `alocar_bolsa.feature`).
2. Abrir o pull request: o job de testes fica vermelho e o botão de merge fica bloqueado pela proteção da `main`. Print da tela do PR com o check falho → `pipeline-vermelho.png`.
3. Corrigir o teste no mesmo PR, esperar o check ficar verde e fazer o merge. O Render publica só depois dos checks (Auto-Deploy "After CI Checks Pass"). Print do run verde e do deploy "Live" no Render → `pipeline-verde.png`.

## Como fazer o rollback

**Pelo Render (mais rápido):** serviço Rota Vital → **Events** → no último deploy que funcionava, **Rollback** → confirmar → conferir `GET /actuator/health` → `{"status":"UP"}`. Depois de um rollback o Render pode desligar o Auto-Deploy; religar em **Settings → Auto-Deploy** quando a correção estiver na `main`.

**Pelo Git (corrige a `main`):**

```bash
git checkout main
git pull
git revert <hash-do-commit-com-problema>
git push origin main
```

O `revert` cria um commit novo que desfaz o problema, passa pelo pipeline e o Render publica a versão corrigida. Detalhes em [`doc/pipeline-deploy.md`](../pipeline-deploy.md).
