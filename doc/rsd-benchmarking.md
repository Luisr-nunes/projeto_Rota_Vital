# Benchmarking piloto de rede — Rota Vital

**Task:** PI2-174 — E2 · Benchmarking piloto com k6 contra o Render
**História:** PI2-31 — RSD: métricas de rede, benchmarking piloto e painel de rede
**Responsável:** Micaella Cabral
**Disciplina:** Infraestrutura de Comunicação (RSD)
**Sprint:** W12e13

## 1. Objetivo

Medir como a API do Rota Vital se comporta na rede real (cliente → borda HTTPS do Render → aplicação), em três níveis de carga, registrando **latência**, **vazão** e **erros**. O piloto da Sprint 12e13 roda o cenário **baixo** contra produção. Os cenários moderado e alto ficam prontos para a U2 (Sprint 14e15), alinhados aos três cenários de uso de SO.

## 2. Ferramenta e alvo

| Item | Valor |
|---|---|
| Ferramenta | [k6](https://k6.io) (Grafana Labs), script em JavaScript |
| Script | [`rsd/k6/benchmark.js`](../rsd/k6/benchmark.js) |
| Alvo do piloto | `https://rota-vital-jnmx.onrender.com` (HTTPS/443, TLS terminado no Render) |
| Caminho de rede | Cliente k6 → Internet → borda Render (HTTPS/443) → container Spring Boot (HTTP/8080) → PostgreSQL Supabase (JDBC/TCP 5432 com TLS) |
| Rotas exercitadas | Só leitura (GET), escolhidas ao acaso a cada iteração, para não gravar dados em produção |

Rotas do teste:

| Rota | História |
|---|---|
| `GET /api/hospitais` | HU03 |
| `GET /api/estoques/bolsas?tipo=O_POS&componente=HEMACIAS` | HU02 (consulta FEFO) |
| `GET /api/indicadores` | HU07 |
| `GET /api/v1/indicadores/risco-desabastecimento?dias=7` | EST U2 |
| `GET /api/telemetria/temperatura?veiculoId=1` | Telemetria |

Cada usuário virtual (VU) faz uma requisição e espera 1 s antes da próxima (tempo de "pensar" entre cliques).

## 3. Cenários de carga

| Cenário | VUs | Duração | Situação que representa | Quando rodar |
|---|---:|---:|---|---|
| **baixo** | 10 | 1 min | Uso normal: hemocentro e poucos hospitais | Piloto (Sprint 12e13) |
| **moderado** | 50 | 2 min | Plantão: vários hospitais pedindo bolsas ao mesmo tempo | Sprint 14e15 |
| **alto** | 100 | 2 min | Pico de emergência | Sprint 14e15, com a equipe avisada (plano gratuito do Render) |

Vazão esperada pelo desenho do teste: cerca de VUs ÷ (tempo de resposta + 1 s). Para 10 VUs, um pouco menos de 10 requisições/s.

## 4. Métricas e critérios

| Métrica | De onde vem | Critério do piloto |
|---|---|---|
| Latência p50 / p95 / máx. | `http_req_duration` do k6 (tempo total da requisição, inclui rede e TLS) | p95 < 800 ms |
| Vazão | `http_reqs` do k6 (requisições/s) | Registro, sem limite |
| Taxa de erro | `http_req_failed` (status ≥ 400 ou falha de conexão) | < 1% |
| Handshake TLS | `http_req_tls_handshaking` | Registro: custo da borda HTTPS/443 |
| Tempo no servidor | `GET /api/v1/metricas/rede` (p50/p95 medidos dentro da aplicação, PI2-173) | Comparar com o k6 |

Os limites de p95 e de erro estão no próprio script (`thresholds`): se forem violados, o k6 termina com código de saída diferente de zero.

**Leitura conjunta:** a diferença entre o p95 do k6 (cliente) e o p95 de `/api/v1/metricas/rede` (servidor) estima quanto do tempo é **rede + borda** e quanto é **processamento** na aplicação.

## 5. Como executar

1. Instalar o k6: `winget install k6` (Windows) ou ver [grafana.com/docs/k6](https://grafana.com/docs/k6/latest/set-up/install-k6/).
2. **Acordar o serviço** (plano gratuito hiberna após ~15 min): abrir `https://rota-vital-jnmx.onrender.com/actuator/health` e esperar `{"status":"UP"}`. Sem isso, o *cold start* de ~1 min entra na medição.
3. Anotar `GET https://rota-vital-jnmx.onrender.com/api/v1/metricas/rede` antes do teste.
4. Rodar o piloto:

   ```bash
   k6 run -e BASE_URL=https://rota-vital-jnmx.onrender.com -e CENARIO=baixo rsd/k6/benchmark.js
   ```

5. Consultar `/api/v1/metricas/rede` de novo e salvar o resumo do k6 e um print em `doc/evidencias/`.

Para outro cenário, trocar `CENARIO=moderado` ou `CENARIO=alto`. Sem `BASE_URL`, o script usa `http://localhost:8080`.

## 6. Validação do script (execução local)

Antes do piloto, o cenário baixo foi executado contra a aplicação rodando localmente (`java -jar`, perfil padrão com H2, sem rede externa), em 28/10/2026. **Estes números não medem a rede**: servem só para provar que o script funciona e como referência de tempo de processamento sem a borda.

| Métrica | k6 (cliente) | `/api/v1/metricas/rede` (servidor) |
|---|---:|---:|
| Requisições | 600 | 600 |
| Erros | 0 (0,00%) | 0 × 4xx, 0 × 5xx |
| Vazão | 9,87 req/s | 8,82 req/s (média desde o início do processo) |
| Latência p50 | 6,88 ms | ≤ 10 ms (balde) |
| Latência p95 | 19,92 ms | ≤ 25 ms (balde) |
| Latência máx. | 178,15 ms | — |

O p95 do k6 (19,92 ms) cai dentro do balde de 25 ms reportado pela aplicação, o que confirma que as duas medições são coerentes. Os dois limites do script (p95 < 800 ms e erro < 1%) passaram.

## 7. Resultados do piloto contra o Render

*A preencher na execução do piloto (planejada para 29/10), seguindo a seção 5.*

| Métrica | Resultado |
|---|---|
| Data/hora da execução | |
| Requisições / vazão | |
| Latência p50 / p95 / máx. (k6) | |
| p50 / p95 no servidor (`/api/v1/metricas/rede`) | |
| Taxa de erro | |
| Handshake TLS médio | |
| Limites do script | passou / falhou |

## 8. Fora do escopo do piloto

- **Perda de pacotes e jitter:** o k6 mede na camada HTTP. Perda e variação de atraso exigem medição na camada de rede (por exemplo, `ping`/`mtr` para a borda) e ficam como próximo passo (30/10).
- **Escrita no banco:** o teste só lê. Cenários com `POST` (requisições, doações) precisam de um ambiente que não seja a produção.
- **Cold start:** fica fora da medição (passo 2 da seção 5), mas deve ser citado na interpretação, porque afeta o primeiro usuário após a hibernação.
