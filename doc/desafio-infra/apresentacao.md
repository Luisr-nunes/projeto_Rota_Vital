# Desafio de Infra — entrega e apresentação (20/10/2026)

Jira: PI2-221 (subtarefa da PI2-158) · Responsável: Luis Felipe Farias Nunes

## 1. Checklist da entrega no Classroom

Uma entrega por grupo, com os quatro itens pedidos no enunciado:

| # | Item | Onde está | Situação |
|---|---|---|---|
| 1 | Link do repositório (público) | https://github.com/Luisr-nunes/projeto_Rota_Vital | Pronto |
| 2 | URL pública do sistema no ar | https://rota-vital-jnmx.onrender.com (health check em `/actuator/health`) | Pronto |
| 3 | README com as seções Integrantes · Como rodar · Testes · Pipeline e deploy · Decisões · Uso de IA | [`README.md`](../../README.md), seção "Desafio de Infra" | Testes, Pipeline e deploy, Decisões e Uso de IA: PI2-223 · Como rodar com Docker: PI2-172 |
| 4 | Evidências do pipeline (execução vermelha, execução verde, URL e rollback) | [`doc/evidencias/`](../evidencias/README.md) | Pronto: PR #36 (run #58 vermelho, #59 e #60 verdes, deploy `b14e58c`) |

Antes de enviar, conferir:

- [ ] `docker compose up -d --build` sobe API e Postgres do zero (PI2-171)
- [x] Pipeline roda em pull request e push (PR #36)
- [ ] Merge bloqueado com teste vermelho: falta a proteção da `main` (Luis)
- [x] Prints das duas execuções (vermelha e verde) em `doc/evidencias/`
- [x] `curl https://rota-vital-jnmx.onrender.com/actuator/health` responde `{"status":"UP"}` (20/10, 13:33)
- [ ] Print da entrega confirmada no Classroom anexado na PI2-221

## 2. Roteiro da apresentação (cerca de 5 minutos)

Tema pedido: **a jornada do TDD e as lições aprendidas**. Cada bloco tem um responsável, para todos participarem.

| Tempo | Bloco | O que mostrar | Quem |
|---|---|---|---|
| 0:00–0:40 | Contexto | O Rota Vital em uma frase: componente certo, compatível e dentro da validade, no hospital certo. A pergunta do desafio: um colega consegue clonar, rodar, testar e publicar sem pedir ajuda? | Luis Felipe |
| 0:40–1:30 | E1 · Container | `docker compose up -d --build`, `docker compose ps` com os dois serviços saudáveis e a API respondendo. Por que multi-stage e por que volume nomeado. | Matheus e Mariana |
| 1:30–2:50 | E2 · TDD | O story map (backbone e Release 1). A regra escolhida para testar primeiro: compatibilidade ABO/Rh e FEFO, pelo risco ao paciente e ao descarte. Mostrar um cenário Gherkin de `alocar_bolsa.feature` passando e o `confirmar_entrega.feature` escrito antes do código. | Luis Lucena e João Pedro |
| 2:50–4:10 | E3 · Deploy automatizado | Um PR com teste vermelho barrado; um merge verde que publica no Render; onde ficam os segredos; como fazer o rollback. | Lucas e Micaella |
| 4:10–5:00 | Lições aprendidas | Ver a seção 3. | Luis Felipe |

## 3. Lições aprendidas (para o último bloco)

1. **Testar primeiro onde o erro custa mais.** Começamos pelas regras de compatibilidade e de validade porque um erro nelas põe um paciente em risco ou aumenta o descarte de bolsas. As telas ficaram por último.
2. **Teste de aceitação antes do código vira contrato.** O cenário da HU06 foi escrito antes do endpoint: ele já diz ao backend o que precisa ser entregue e quando a história está pronta.
3. **Branch longa atrasa a integração.** A alocação compatível e o cálculo de risco ficaram prontos em branches separadas e demoraram a chegar na main. Com PRs curtos e pipeline obrigatório, a integração fica contínua.
4. **O pipeline só protege se for obrigatório.** Antes, o CI rodava só depois do push na main. Com testes no pull request e a main protegida, teste vermelho não chega à produção.
5. **Configuração fora do código desde o início.** Senha do banco no cofre (GitHub Secrets e variáveis do Render) e `.env.example` no repositório evitam vazamento e deixam qualquer um subir o projeto.
