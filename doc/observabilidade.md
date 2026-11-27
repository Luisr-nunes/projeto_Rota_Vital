# Observabilidade — Rota Vital

**Disciplina:** Infraestrutura de Comunicação
**Stack:** Spring Boot Actuator + Prometheus + Grafana + Loki + Tempo
**Ambiente completo (Docker Compose, setup da VM, dashboards):** [`observabilidade/README.md`](../observabilidade/README.md)
**Decisões de arquitetura:** [ADR 0005](../docs/arquitetura/adr/0005-observabilidade-stack.md)

Este documento cobre a parte que fica no código da aplicação (`rotavital/`): o que o Actuator
expõe, as métricas de negócio e como rodar localmente. A infraestrutura (Prometheus, Grafana,
Loki, Tempo, exporters) está em `observabilidade/`.

## 1. Endpoints do Actuator

| Endpoint | Conteúdo |
|---|---|
| `GET /actuator/health` | Status da aplicação (público, sem autenticação — usado pelo healthcheck do Docker/Render) |
| `GET /actuator/info` | Nome e descrição da aplicação (`info.app.*` no `application.yml`) |
| `GET /actuator/metrics` | Lista e consulta métricas individuais do Micrometer |
| `GET /actuator/prometheus` | Métricas no formato Prometheus (HTTP, JVM, HikariCP, negócio) |

Todas as métricas saem com a tag `application=hemorede` (`management.metrics.tags.application`),
pra diferenciar a aplicação nas consultas do Grafana quando houver mais de um serviço no mesmo
Prometheus.

`/actuator/prometheus` não exige autenticação (`SegurancaConfig`) porque o scrape estático do
Prometheus não tem como mandar um JWT — ver a justificativa de segurança no ADR 0005 e no
comentário do próprio `SegurancaConfig.java`. Em produção (perfil `prod`, Render) o endpoint nem
existe: só `health` está no `management.endpoints.web.exposure.include` do
`application-prod.yml`.

## 2. Métricas de negócio

| Métrica | Tipo | O que mede | Onde é incrementada/lida |
|---|---|---|---|
| `hemorede_requisicoes_criadas_total` | Counter | Total de requisições hospitalares registradas | `RequisicaoService.registrar` |
| `hemorede_requisicoes_alocacao_seconds` | Timer | Tempo gasto reservando as bolsas compatíveis de uma requisição (medido mesmo quando falha por estoque insuficiente) | `RequisicaoService.aprovar` |
| `hemorede_estoque_bolsas_disponiveis` | Gauge | Bolsas com status `DISPONIVEL` em toda a rede | `EstoqueService` (via `BolsaRepository.countByStatus`), lido a cada scrape |

## 3. Traces (Tempo) e correlação com logs

- `management.tracing.sampling.probability: 1.0` — traceia 100% das requisições (demonstração;
  em produção real seria uma fração bem menor).
- `management.otlp.tracing.endpoint` manda os traces via OTLP para o Tempo
  (`http://tempo:4318/v1/traces` por padrão, configurável por `OTEL_EXPORTER_OTLP_ENDPOINT`).
- O `logback-spring.xml` imprime `traceId`/`spanId` no console e na mensagem enviada ao Loki
  (`traceId=... spanId=...`). O datasource do Loki no Grafana extrai o `traceId` por regex
  (`derivedField`) e linka pro trace correspondente no Tempo — navegação log → trace sem precisar
  de nenhum código extra na aplicação.

## 4. Logs (Loki)

O appender `Loki4jAppender` (biblioteca `loki-logback-appender`) envia os logs por push HTTP pro
Loki, com a label `application=hemorede`. A URL vem de `LOKI_URL` (variável de ambiente; sem
valor fixo). Como o envio é assíncrono, rodar sem o Loki no ar (`mvn test`, dev local sem o stack
de observabilidade) só gera uma tentativa de conexão falhando em segundo plano — não derruba a
aplicação nem os testes.

## 5. Como rodar

| Cenário | Comando | Loki/Tempo |
|---|---|---|
| `mvn test` / dev local sem Docker | `./mvnw spring-boot:run` | Tentativa de push falha em segundo plano (sem efeito) |
| Ambiente completo de observabilidade | `cd observabilidade && docker compose up -d --build` | Funcionando (ver `observabilidade/README.md`) |

## 6. Checklist da entrega (seção 9 do enunciado)

- [x] Actuator e `micrometer-registry-prometheus` na aplicação
- [x] `/actuator/prometheus` respondendo com a tag `application`
- [x] 3 métricas de negócio (counter, timer, gauge), com painel no Grafana (ver `observabilidade/README.md`)
- [x] Node Exporter e cAdvisor no `docker-compose.yml` de `observabilidade/`
- [x] `docker compose up -d` sobe todo o ambiente sem passos manuais
- [ ] Todos os *targets* UP no Prometheus — confirmar com o stack no ar
- [ ] Datasource do Grafana provisionado e dashboards de aplicação/infraestrutura com dados — dashboards montados manualmente (seção "Dashboards" do `observabilidade/README.md`)
- [ ] JSON dos dashboards versionado em `observabilidade/grafana/dashboards/`
- [x] Senha do Grafana via `.env` (fora do repositório, só `.env.example`)
- [x] README com passo a passo e URLs (`observabilidade/README.md`)
- [ ] PDF com descrição, diagrama e evidências — fora do repositório, rascunho à parte
- [ ] URL de acesso remoto e usuário Viewer informados ao professor
- [ ] Loki e Tempo rodando, com logs, traces e navegação log → trace evidenciados no PDF
