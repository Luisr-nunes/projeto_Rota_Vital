# Story map do Rota Vital — Desafio de Infra (E2 · TDD)

Jira: PI2-187 (subtarefa da PI2-36) · Responsável: Luis Felipe Farias Nunes

O story map organiza as histórias de usuário do Rota Vital pela jornada de uma bolsa de sangue,
da doação até a entrega no hospital. A primeira linha é o **backbone** (as atividades do usuário);
abaixo dela ficam as histórias, separadas por release. As histórias completas, com cenários BDD,
estão em [`doc/historias_de_usuario.md`](../historias_de_usuario.md).

## 1. Backbone e releases

| Atividade (backbone) | Doar e estocar | Consultar estoque | Requisitar | Alocar e rotear | Entregar | Acompanhar |
|---|---|---|---|---|---|---|
| **Usuário principal** | Atendente do hemocentro | Profissional do hemocentro | Profissional do hospital | Profissional do hemocentro / logística | Motorista / hospital | Gestor da rede |
| **Release 1 (MVP)** | HU01 Registrar doação e cadastrar bolsa | HU02 Consultar estoque por tipo, componente e validade | HU03 Registrar requisição hospitalar | HU04/HU08 Alocar bolsa compatível (ABO/Rh) por FEFO · HU05 Calcular rota de menor custo | HU06 Confirmar entrega | HU07 Painel gerencial |
| **Release 2** | — | Alerta de estoque mínimo por tipo | Prioridade clínica (URGENTE antes de NORMAL) | Rota exibida no mapa da tela | Telemetria de temperatura do veículo em trânsito | Risco de desabastecimento e taxa de descarte · Painel de rede |

A Release 1 é o caminho mínimo que entrega valor: uma bolsa entra no estoque, um hospital pede,
o sistema escolhe a bolsa certa e a rota, e a entrega é confirmada.

## 2. Três histórias da Release 1 quebradas em tarefas

Cada tarefa de regra de negócio tem um teste unitário. Cada história tem ao menos um teste de
integração (com o banco) e um teste de aceitação. A coluna **Situação** mostra o estado real em 19/10/2026.

### HU08 — Alocar bolsa compatível (ABO/Rh) com menor validade (FEFO)

| # | Tarefa | Teste | Nível | Situação |
|---|---|---|---|---|
| 1 | Regra de compatibilidade ABO/Rh (receptor × doador) | `TipoSanguineo.compativel` exercitada pelos cenários de incompatibilidade | Unitário | Regra na main; teste unitário dedicado da matriz ABO/Rh vem na PI2-165 |
| 2 | Escolher a bolsa compatível que vence primeiro (FEFO) | `AEDCasosDeBordaTest` (FilaFEFO) e `RequisicaoFluxoIntegrationTest.deveAprovarRequisicaoComFEFO` | Unitário / Integração | Na main |
| 3 | Reverter a alocação inteira quando falta estoque para um item | `AlocacaoServiceTest` | Unitário | Teste ainda na branch `luis/pi2-145-alocacao`; regra na main (commit d61cda5) |
| 4 | Não alocar bolsa incompatível ou vencida, gravando os status no banco | `RequisicaoFluxoIntegrationTest` (Spring Boot + H2) | Integração | Na main |
| 5 | Cenários de aceitação da alocação | [`alocar_bolsa.feature`](../../rotavital/src/test/resources/features/alocar_bolsa.feature) (Cucumber + MockMvc) | Aceitação | 6 cenários executáveis (PI2-188) |

### HU01 — Registrar doação e cadastrar bolsa no estoque

| # | Tarefa | Teste | Nível | Situação |
|---|---|---|---|---|
| 1 | Recusar doador inativo (422) | `DoacaoControllerIntegrationTest.deveRejeitarDoadorInativo` | Integração | Na main; falta o teste unitário no `DoacaoServiceTest` |
| 2 | Respeitar intervalo mínimo de 60 dias entre doações | `DoacaoServiceTest` (59, 60 dias, mesmo dia, data anterior) | Unitário | Na main |
| 3 | Validar o corpo da requisição (coleta não futura, validade futura) | `DoacaoControllerIntegrationTest` (400) | Integração | Na main |
| 4 | `POST /api/doacoes` salvando doação e bolsa `DISPONIVEL` na mesma transação | `DoacaoControllerIntegrationTest` (201 + bolsa no banco) | Integração | Na main |
| 5 | Cenários BDD da HU01 (doador ativo e inativo) | `DoacaoControllerIntegrationTest` cobre os dois cenários | Aceitação | Cobertos por MockMvc; feature Gherkin própria ainda não criada |

### HU06 — Confirmar a entrega das bolsas ao hospital

| # | Tarefa | Teste | Nível | Situação |
|---|---|---|---|---|
| 1 | Transição de status: requisição `ALOCADA` → `ENTREGUE`, bolsas `RESERVADA` → `UTILIZADA` | Teste unitário do service de requisição | Unitário | Pendente (PI2-181, sprint 14e15) |
| 2 | Impedir entrega de requisição não alocada (409) | Teste unitário do service de requisição | Unitário | Pendente (PI2-181) |
| 3 | `PATCH /api/v1/requisicoes/{id}/entrega` gravando os status | Teste de integração com o banco | Integração | Pendente (PI2-181) |
| 4 | Cenários de aceitação da entrega | [`confirmar_entrega.feature`](../../rotavital/src/test/resources/features/confirmar_entrega.feature) | Aceitação | Escrito antes do código (marcado `@pendente_PI2_181`) |

A HU06 é o exemplo mais puro de TDD do projeto: os cenários de aceitação foram escritos **antes**
de existir o endpoint e hoje estão vermelhos de propósito. Eles passam a contar como aprovados
quando a PI2-181 entrar na main.

## 3. Quais regras ganharam teste primeiro e por quê

| Regra | Por que primeiro (risco) |
|---|---|
| Compatibilidade ABO/Rh | Um erro aqui entrega sangue incompatível e põe um paciente em risco. É a regra de maior impacto do sistema. |
| FEFO (menor validade primeiro) | Errar a ordem aumenta o descarte de bolsas por vencimento, que é um dos problemas centrais do projeto. |
| Rollback da alocação parcial | Reservar só parte de um pedido deixa bolsas presas e o hospital sem atendimento completo. |
| Doador inativo e intervalo entre doações | Protegem a saúde do doador e a qualidade da bolsa que entra no estoque. |

## 4. O que ficou sem teste (e por quê)

- **Telas HTML/JavaScript** (`index.html`, `indicadores.html`): validadas manualmente e nos screencasts; não há teste automatizado de interface.
- **Integração com PostgreSQL real**: os testes rápidos (`mvn test`) usam H2 em memória; a PI2-191 adicionou os testes `*IT` com Postgres via Testcontainers (`mvn verify`), que cobrem o fluxo de requisição e alocação e a carga sintética. Ficam de fora da cobertura com Postgres os demais endpoints (doação, estoque, rotas, indicadores), que continuam só no H2.
- **HU06**: endpoint ainda não implementado (cenários de aceitação já escritos).
- **Deploy no Render**: verificado pelo health check `GET /actuator/health` após cada publicação, não por teste automatizado.
