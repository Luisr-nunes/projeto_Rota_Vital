# ADR 0005 — Observabilidade: Prometheus/Grafana, Micrometer Tracing e logs via push ao Loki

**Status:** Aceito
**Data:** 26/11/2026
**Responsável:** Luis Felipe Farias Nunes
**Relacionados:** entrega de Infraestrutura de Comunicação (Observabilidade com Actuator,
Prometheus e Grafana); `observabilidade/` (stack completo); `doc/observabilidade.md`

## Contexto

A disciplina de Infraestrutura de Comunicação pede métricas de aplicação (Actuator/Prometheus),
métricas de infraestrutura (Node Exporter/cAdvisor), logs centralizados (Loki) e traces (Tempo),
tudo visível no Grafana, com acesso remoto para correção.

O Rota Vital já roda em produção no Render (`application-prod.yml` restringe o Actuator só a
`health`, sem tracing nem envio de logs — ver ADR 0003 sobre a decisão de monolito modular). Esse
ambiente de produção real não é o lugar certo para o experimento de observabilidade: o Render
free tier não dá acesso ao host (sem Node Exporter/cAdvisor de verdade) nem permite rodar um
Docker Compose de vários serviços ao lado da API.

## Decisão

**Ambiente próprio, separado da produção.** A aplicação sobe de novo, com o perfil padrão (não
`prod`), num `docker-compose.yml` dedicado em `observabilidade/`, numa VM gratuita (Oracle Cloud
Always Free) só durante a janela de correção. Zero mudança no Render e no fluxo de deploy
existente (`doc/pipeline-deploy.md`).

**Stack de coleta: Prometheus + Grafana, sem trocar.** O `pom.xml` já trazia
`micrometer-registry-prometheus`; nenhuma das alternativas da Seção 3 do enunciado
(VictoriaMetrics, InfluxDB, Mimir, Thanos, Graphite, Zabbix) resolve um problema que o Prometheus
não resolva aqui, e trocar custaria tempo sem ganho para uma entrega de curso.

**Traces: Micrometer Tracing + bridge OTel, exportando via OTLP.** É o sucessor oficial do Spring
Cloud Sleuth (descontinuado) e já vem gerenciado pelo BOM do Spring Boot 3.3 — sem fixar versão
na mão. O Tempo recebe os traces direto via OTLP (push), sem precisar de um Collector
intermediário.

**Logs: push direto da aplicação pro Loki (Loki4j/Logback), sem Promtail.** A alternativa mais
comum é Promtail lendo os logs dos contêineres via `docker_sd_configs` (socket do Docker montado
no Promtail). Descartada porque: (a) mais um contêiner com acesso ao socket do Docker é superfície
extra sem necessidade — só uma aplicação gera log aqui; (b) o push direto funciona igual rodando
local ou na VM, sem depender de onde o Docker guarda os logs do contêiner.

**Acesso remoto: VM com porta do Grafana liberada no firewall, não Cloudflare Tunnel/ngrok.**
Com o ambiente já rodando numa VM (necessário pelo Node Exporter/cAdvisor monitorarem um host
real), expor a porta do Grafana no firewall da própria VM é mais direto que adicionar mais um
serviço (`cloudflared`) só para encaminhar tráfego para o mesmo host.

## Consequências

**Positivas**

- Produção (Render) sem nenhuma mudança de risco: perfil `prod` continua só com `health`.
- Node Exporter e cAdvisor monitoram infraestrutura real (host da VM), não uma VM genérica do
  Docker Desktop.
- `/actuator/prometheus` nunca fica exposto na internet (nem na VM, nem no Render) — API e banco
  sem porta publicada no Compose da VM.

**Negativas e riscos aceitos**

- Dois lugares onde a aplicação roda (Render e VM) durante a correção; a VM não reflete uso real
  de produção, só a demonstração.
- O appender do Loki é assíncrono: se o Loki cair durante a correção, logs daquele intervalo se
  perdem (aceitável para uma demonstração; não seria para produção real).
- Setup manual da VM (Oracle Cloud) e dos dashboards (import + export) não é 100% reproduzível
  por script — documentado passo a passo em `observabilidade/README.md`.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Promtail + socket do Docker | Mais um contêiner com acesso sensível, sem necessidade real (só uma aplicação gera log). |
| Cloudflare Tunnel / ngrok | Resolvido mais simples liberando a porta do Grafana na própria VM, já que o Node Exporter/cAdvisor exigem uma VM de qualquer forma. |
| Rodar contra o Render de produção | `application-prod.yml` já restringe o Actuator; reabrir isso pra observabilidade colocaria produção em risco por uma entrega de curso. |
| Alternativa de coleta (VictoriaMetrics, Mimir, etc.) | Nenhum ganho concreto para o escopo desta entrega; Prometheus já era a escolha existente. |
