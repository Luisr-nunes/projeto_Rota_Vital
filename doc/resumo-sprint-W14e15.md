# Resumo da Sprint — Week 14e15 (Entrega Final U2)

**Sprint:** Week 14e15-Entrega Final U2
**Período:** 02/11/2026 – 13/11/2026
**Meta:** publicar a Entrega 04 de POO (09/11) com HU02, HU06 e HU08, apresentar o projeto (09–13/11) e fechar a documentação da U2 de AED, EST, SO e RSD.
**Situação em 12/11 (véspera do fechamento):** 9 de 18 subtarefas entregues na main; 2 prontas aguardando PR (N1 e N2); 7 sem entrega na main.
**Verificação:** a main foi conferida no git em 12/11; 304 testes verdes no `mvn test`.

> Regra de "concluído" (Definition of Done da U2): PR mergeado na main, CI verde e link no comentário da subtarefa no Jira. Por isso a tabela abaixo segue o git, e não só o board.

---

## Entregas por história

| Issue | História | Responsável | Situação em 12/11 | Evidência |
|---|---|---|---|---|
| [PI2-33](https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-33) | POO — Entrega 04: HU02, HU06 e HU08 | Mariana | **Entregue** (5/5 subtarefas) | `GET /api/v1/estoques/bolsas` com filtros (PR #67), tela de estoque, README da Entrega 04 com post-its e print do bug tracker (PRs #69, #72, #75), screencasts do sistema e do código (PRs #71, #73, #74) |
| [PI2-127](https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-127) | HU06 — Confirmar entrega | Matheus | **Entregue** | `PATCH /api/v1/requisicoes/{id}/entrega` e botão Confirmar entrega (PR #64) |
| [PI2-160](https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-160) | P3 — Apresentação final e gestão da sprint | Luis Felipe | K1 **entregue**; K2 em andamento | Roteiro (PR #69), [slides](apresentacao/slides-apresentacao-final.pdf) e este resumo; roteiro da demo (PR #65), vídeo curto pendente |
| [PI2-222](https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-222) | SO — Arquitetura em 3 cenários e demonstração concorrente | Lucas | J2 **entregue**; J4 pendente | [`so-u2-arquitetura-cenarios.md`](so-u2-arquitetura-cenarios.md) com diagramas, portas e orçamento |
| [PI2-37](https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-37) | RSD — Painel de rede com telemetria real e benchmarking final | Micaella | N1 e N2 **prontos, aguardando PR**; N3 pendente | Métricas da telemetria no painel e benchmarking dos 3 cenários contra o Render com perda e jitter |
| [PI2-35](https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-35) | EST — Previsão, relatório final e painel consolidado | Luis Lucena | M3 **entregue**; M1 em andamento; M2 pendente | Painel consolidado U1 + U2 (PR #57) |
| [PI2-34](https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-34) | AED — Relatório U2 de complexidade e cenários | João Pedro | L1 e L2 pendentes | — |

---

## Burndown (subtarefas restantes, pela data do merge na main)

| Data | Entregues no dia | Restantes |
|---|---|---|
| 02/11 (início) | — (H3 já entregue em 28/10) | 17 |
| 03/11 | 1 (M3) | 16 |
| 04/11 – 05/11 | 0 | 16 |
| 06/11 | 1 (H1) | 15 |
| 07/11 | 0 | 15 |
| 08/11 | 4 (H2, H4, H5, H6) | 11 |
| 09/11 – 11/11 | 0 | 11 |
| 12/11 | 2 (J2, K1) | 9 |
| 13/11 (meta) | N1, N2 e o que mais entrar até o fim do dia | 0 |

O padrão da W12e13 se repetiu: um platô de 09/11 a 11/11, logo depois da Entrega 04. A Entrega 04 de POO saiu no prazo porque concentrou o time em 08/11, mas as entregas de documentação da U2 (AED, EST e RSD) ficaram para os dois últimos dias.

---

## Pendências em 12/11 (prazo 13/11)

| Subtarefa | Responsável | O que falta |
|---|---|---|
| N1 · Painel de rede com telemetria real (PI2-198) | Micaella | Abrir o PR (código e testes prontos) |
| N2 · Benchmarking com perda e jitter (PI2-199) | Micaella | Abrir o PR (medição feita em 12/11) |
| M2 · Relatório final de EST (PI2-196) | Micaella | Relatório `doc/est-u2-relatorio.md` |
| N3 · Plano de implantação (PI2-200) | Matheus | `doc/rsd-plano-implantacao.md` |
| K2 · Vídeo curto da demo (PI2-202) | Matheus | Exportar a versão de até 2 min |
| M1 · Previsão simples de demanda (PI2-195) | Luis Lucena | `GET /api/v1/indicadores/previsao-demanda` |
| L1 e L2 · Relatório de AED e cenários (PI2-193, PI2-194) | João Pedro | Relatório de complexidade e cenários em Gherkin |
| J4 · Demonstração concorrente nos 3 cenários (PI2-192) | Lucas | Rodar o teste de corrida com a carga de cada cenário |

O que não entrar até 13/11 vai para a Sprint 16e18 como dívida, com o motivo registrado no Jira, como foi feito com a C4 na W12e13.

---

## Lições para a 16e18

- A entrega de uma disciplina não pode esperar a da outra acabar: as docs da U2 deveriam ter começado junto com a Entrega 04, não depois.
- Benchmark e orçamento dependem um do outro: rodar o k6 primeiro deu os números que justificaram o dimensionamento de SO.
- Atualizar o Jira no mesmo dia do merge. Em 12/11, quatro subtarefas já estavam na main desde 08/11 com o card ainda em "Tarefas pendentes".

## Próxima sprint

**Week 16e18 (16/11 – 04/12):** demonstrações finais por disciplina (PI2-38 a PI2-42), versão v2.0 com tag na main, apresentação final do PI, retrospectiva e dossiê de evidências de gestão (PI2-203).
