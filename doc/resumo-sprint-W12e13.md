# Resumo da Sprint — Week 12e13 (Telemetria U2)

**Sprint:** Week 12e13-Telemetria U2 (ID: 74)
**Período:** 19/10/2026 – 30/10/2026 (encerrada em 01/11/2026)
**Meta:** publicar a Entrega 03 de POO (19/10), integrar na main a alocação compatível ABO/Rh + FEFO + rota (HU08), colocar no ar a telemetria concorrente sincronizada (SO), as métricas e o benchmarking de rede (RSD), o risco de desabastecimento e a taxa de descarte no painel (EST), e subir o sistema com docker compose (Desafio Infra E1).
**Status:** concluída — 8/8 histórias e 27/27 subtarefas, com uma subtarefa repassada (C4)
**Verificação:** a main foi conferida no git em 01/11; 291 testes verdes e CI verde no PR #55.

---

## Entregas por disciplina

| Issue | História | Responsável | Evidência |
|---|---|---|---|
| [PI2-156](https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-156) | POO — Entrega 03: HU01 e HU03 documentadas e demonstradas | Luis Felipe | README (seção Entrega 03), screencasts de sistema e de código |
| [PI2-157](https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-157) | AED — HU08: matching ABO/Rh + FEFO + rota | João Pedro | `MatrizCompatibilidade` 8×8 em Java e C (PR #53), alocação devolvendo a rota Dijkstra, `doc/aed-u2-compatibilidade.md` (PR #46) |
| [PI2-158](https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-158) | SO — Desafio Infra E1: container + entrega | Lucas | Dockerfile multi-stage e `compose.yaml` (PR #34), `.env.example`, README Docker, apresentação em 20/10 |
| [PI2-36](https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-36) | SO — Desafio Infra E2/E3: TDD e deploy com gates | Lucas | Story map, Gherkin (`alocar_bolsa.feature`, `confirmar_entrega.feature`), `ci.yml` com gate em PR, Testcontainers (PR #38), evidências vermelho/verde/rollback |
| [PI2-31](https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-31) | RSD — métricas de rede, benchmarking e painel | Micaella | Actuator + Micrometer, `GET /api/v1/metricas/rede`, k6 em 3 cenários, `doc/rsd-benchmarking.md` (PR #48), `gestor/rede.html` (PR #52) |
| [PI2-32](https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-32) | EST — risco de desabastecimento e taxa de descarte | Luis Lucena | `GET /api/v1/indicadores/descarte` e `/risco-desabastecimento`, `doc/est-u2-probabilidade.md` (PRs #40, #42, #43), cards e gráfico (PR #51) |
| [PI2-30](https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-30) | SO — telemetria com ingestão concorrente | Matheus | Agregador sincronizado e teste de corrida (PR #44), `POST /api/v1/telemetria/leituras` com fila e consumidores e simulador de veículos (PRs #54 e #55) |
| [PI2-159](https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-159) | P3 — gestão da sprint | Mariana / Luis Felipe | Definition of Done da U2 (PR #43) e este resumo |

---

## Desvio e encaminhamento

- **C1 (PI2-168) e C3 (PI2-170), Lucas:** entregues parcialmente.
  - Entrou o commit `2337ba9`, com o POST, a fila e os consumidores. Porém as classes de produção ficaram em `src/test`, os testes do GET da PI2-121 foram apagados e o PR não foi aberto.
  - Entrou também o commit `187f5c4`, com o gerador do simulador (PR #54). Faltaram o runner, o envio para a fila e as docs.
- **C4 (PI2-229):** o restante foi repassado para a Micaella. Os commits `9d00a91`, `b3edf3b` e `f89be15` entraram pelo PR #55 em 01/11, com CI verde.

---

## Burndown (subtarefas restantes)

| Data | Concluídas no dia | Restantes |
|---|---|---|
| 19/10 | 5 | 22 |
| 20/10 | 7 | 15 |
| 21/10 | 1 | 14 |
| 22/10 – 26/10 | 0 | 14 |
| 27/10 | 4 | 10 |
| 28/10 | 3 | 7 |
| 29/10 | 2 | 5 |
| 30/10 | 1 | 4 |
| 01/11 | 4 | 0 |

Platô de 22/10 a 26/10: o trabalho se acumulou na segunda semana. Quatro subtarefas passaram do fim da sprint (30/10).

---

## Lições para a W14e15

- "Concluído" no Jira só com o PR mergeado na main e o link no comentário (Definition of Done da U2).
- Conferir o git, e não só o board, antes de fechar a sprint.
- Commits diários desde o início da sprint, para evitar o platô do meio.

## Próxima sprint

**Week 14e15-Entrega Final U2 (02/11 – 13/11):** Entrega 04 de POO (09/11) com HU02, HU06 e HU08, apresentação do projeto (09–13/11), PI2-222 / J2 (arquitetura em 3 cenários com orçamento) e fechamento da documentação de AED, EST, SO e RSD.
