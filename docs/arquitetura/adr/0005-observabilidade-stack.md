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
`prod`), num `docker-compose.yml` dedicado em `observabilidade/`. Zero mudança no Render e no
fluxo de deploy existente (`doc/pipeline-deploy.md`).

**Onde esse ambiente roda (atualizado em 26/11/2026):** a ideia inicial era uma VM gratuita
(Oracle Cloud Always Free). Na prática, a região `sa-saopaulo-1` esgotou a capacidade gratuita
tanto do shape ARM (`VM.Standard.A1.Flex`) quanto do x86 (`VM.Standard.E2.1.Micro`) — erro `Out
of capacity` repetido em várias tentativas, em horários diferentes, para os dois shapes
elegíveis ao Always Free. Trocar de região exigiria assinar uma região adicional na tenancy, sem
garantia de que teria capacidade e sem tempo hábil para a entrega. Decisão: rodar o ambiente na
própria máquina da equipe (já com Docker instalado), expondo só o Grafana pela internet via
**Cloudflare Tunnel** (binário `cloudflared`, sem instalação, sem precisar abrir porta no
roteador). Ver seção "Acesso remoto" abaixo, atualizada para refletir essa troca.

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

**Acesso remoto: Cloudflare Tunnel, a partir da máquina que já roda o ambiente.** Com a
capacidade gratuita de VM esgotada (ver acima), o `cloudflared` conecta a própria máquina da
equipe à internet sem abrir porta nenhuma localmente — Grafana continua sendo o único serviço
alcançável de fora, exatamente como seria numa VM.

## Consequências

**Positivas**

- Produção (Render) sem nenhuma mudança de risco: perfil `prod` continua só com `health`.
- Node Exporter e cAdvisor monitoram infraestrutura real (host da VM), não uma VM genérica do
  Docker Desktop.
- `/actuator/prometheus` nunca fica exposto na internet (nem na VM, nem no Render) — API e banco
  sem porta publicada no Compose da VM.

**Negativas e riscos aceitos**

- Dois lugares onde a aplicação roda (Render e a máquina da observabilidade) durante a correção;
  esse ambiente não reflete uso real de produção, só a demonstração.
- A máquina que roda o ambiente precisa ficar ligada (com o `cloudflared` ativo) durante toda a
  janela de correção — igual precisaria de uma VM ligada, só que sem o custo/disponibilidade de
  nuvem garantidos por um provedor.
- O appender do Loki é assíncrono: se o Loki cair durante a correção, logs daquele intervalo se
  perdem (aceitável para uma demonstração; não seria para produção real).
- Dashboards (import + export) não são 100% reproduzíveis por script — documentado passo a passo
  em `observabilidade/README.md`.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| VM gratuita em nuvem (Oracle Cloud Always Free) | Era a escolha original; abandonada depois de esgotar a capacidade Always Free (ARM e x86) na região disponível da tenancy, sem tempo hábil para resolver via assinatura de outra região. |
| Promtail + socket do Docker | Mais um contêiner com acesso sensível, sem necessidade real (só uma aplicação gera log). |
| ngrok | Cloudflare Tunnel preferido por não ter limite de conexões do plano gratuito nem trocar a URL a cada reinício. |
| Rodar contra o Render de produção | `application-prod.yml` já restringe o Actuator; reabrir isso pra observabilidade colocaria produção em risco por uma entrega de curso. |
| Alternativa de coleta (VictoriaMetrics, Mimir, etc.) | Nenhum ganho concreto para o escopo desta entrega; Prometheus já era a escolha existente. |
