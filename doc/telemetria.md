# Telemetria de Temperatura da Cadeia Fria — Rota Vital

**Task:** PI2-120 — Especificação do canal de telemetria (RSD)
**Responsável:** Lucas Henrique
**Disciplina:** Redes e Sistemas Distribuídos (RSD)
**Sprint:** 08e09
**Implementação do endpoint:** PI2-121 — implementado em `TelemetriaController` / `TelemetriaService` (testes: `TelemetriaControllerIntegrationTest`, `TelemetriaServiceTest`).
**Implementação da ingestão:** PI2-168 — implementada em `TelemetriaLeituraController` / `TelemetriaIngestaoService` / `ConsumidorTelemetria` (testes: `TelemetriaLeituraControllerIntegrationTest`, `TelemetriaIngestaoServiceTest`).

Hemocomponentes têm faixa de temperatura de transporte obrigatória (`TipoRefrigeracao`: hemácias refrigeradas a 2–6 °C, plasma congelado a −20 °C, plaquetas a 20–24 °C). A telemetria simula sensores nos veículos e permite detectar quebra da cadeia fria durante a entrega (HU05/HU06).

## 1. Payload da leitura

```json
{
  "veiculoId": 1,
  "temperaturaC": 4.2,
  "timestamp": "2026-09-29T09:15:30-03:00",
  "status": "OK"
}
```

| Campo | Tipo | Descrição |
|-------|------|-----------|
| `veiculoId` | inteiro | Identificador do veículo (`Veiculo.id`) |
| `temperaturaC` | decimal | Temperatura em graus Celsius |
| `timestamp` | texto ISO-8601 | Instante da leitura, com fuso horário |
| `status` | `OK` \| `ALERTA` | `OK` se a temperatura está dentro da faixa segura; `ALERTA` caso contrário |

## 2. Faixa segura e regra de alerta

- A faixa segura depende do `TipoRefrigeracao` do veículo (limites inclusos):

| `TipoRefrigeracao` | Hemocomponente | Faixa segura |
|--------------------|----------------|--------------|
| `REFRIGERADO_2_6` | Hemácias | **2 °C a 6 °C** |
| `CONGELADO_MENOS_20` | Plasma | **−30 °C a −20 °C** |
| `AMBIENTE_CONTROLADO_20_24` | Plaquetas | **20 °C a 24 °C** |

- Leitura fora da faixa → `status: "ALERTA"`. A leitura continua sendo entregue normalmente; o consumidor (painel/aplicação) decide como reagir.

## 3. Frequência de envio

- Um sensor simulado emite **uma leitura a cada 30 segundos** por veículo em trânsito.
- Na U1 a leitura é gerada pela própria aplicação (simulação); não há sensor físico. A temperatura simulada segue uma distribuição normal centrada no meio da faixa segura do veículo (desvio de 1,2 °C), então a maioria das leituras sai `OK` e algumas saem `ALERTA`.

## 4. Protocolo e porta

| Fase | Origem → Destino | Protocolo / Porta | Observação |
|------|------------------|-------------------|------------|
| **U1** | Cliente (painel/navegador) → API | `HTTPS/443` na borda; `HTTP/8080` interno até a aplicação | HTTP/JSON, requisição e resposta (síncrono) |
| **U2 (evolução)** | Sensor/veículo → Broker MQTT | `MQTT/1883` (ou `MQTTS/8883` com TLS) | Publicação assíncrona; aplicação assina o tópico e persiste as leituras |

Na U2 o broker ficaria na sub-rede privada de aplicação; a linha do diagrama seria **tracejada** (assíncrona), conforme a convenção da rubrica de RSD.

A entrada de leituras na aplicação por HTTP/JSON (`POST`, seção 6) usa o mesmo caminho `HTTPS/443` → `HTTP/8080` da U1, mas com resposta assíncrona (`202`).

## 5. Endpoint de consulta (U1)

`GET /api/telemetria/temperatura?veiculoId={id}`

| Situação | Status | Corpo |
|----------|--------|-------|
| Veículo existente | `200 OK` | Payload da seção 1 |
| Veículo inexistente | `404 Not Found` | `{"timestamp": "...", "erro": "IllegalArgumentException", "mensagem": "Veículo não encontrado: {id}"}` |
| `veiculoId` ausente | `400 Bad Request` | Erro padrão de validação |

## 6. Endpoint de ingestão (PI2-168)

`POST /api/v1/telemetria/leituras`

Recebe uma leitura, valida o corpo e a coloca em uma **fila em memória** (capacidade 1000), respondendo `202 Accepted` sem esperar o processamento. Um pool fixo de threads consumidoras (2 por padrão, configurável em `hemorede.telemetria.consumidores`) retira as leituras da fila e as repassa ao `AgregadorTelemetria` (PI2-169).

Corpo:

```json
{
  "veiculoId": 1,
  "temperaturaC": 4.5,
  "timestamp": "2026-11-02T09:00:00-03:00"
}
```

| Campo | Obrigatório | Observação |
|-------|-------------|------------|
| `veiculoId` | sim | Identificador do veículo |
| `temperaturaC` | sim | Temperatura em °C |
| `timestamp` | não | ISO-8601 com fuso, no mesmo formato da seção 1 |

O campo `status` não é enviado no corpo.

| Status | Quando |
|---|---|
| `202 Accepted` | Leitura válida, colocada na fila (processamento assíncrono) |
| `400 Bad Request` | `veiculoId` ou `temperaturaC` ausentes (corpo com `erros` por campo), ou corpo ilegível / `timestamp` fora do formato ISO-8601 |
| `503 Service Unavailable` | Fila cheia (capacidade 1000); o cliente deve tentar novamente |

Exemplo de `400` (campo ausente):

```json
{
  "timestamp": "2026-11-02T09:00:01",
  "erro": "DadosInvalidos",
  "mensagem": "Dados de entrada inválidos",
  "erros": { "veiculoId": "veiculoId é obrigatório" }
}
```

Exemplo de `503` (fila cheia):

```json
{
  "timestamp": "2026-11-02T09:00:01",
  "erro": "FilaTelemetriaCheiaException",
  "mensagem": "Fila de telemetria cheia (capacidade 1000); tente novamente em instantes"
}
```

## 7. Fora de escopo nesta entrega

- Histórico persistido de leituras e gráfico de temperatura (o consumidor apenas agrega em memória, via `AgregadorTelemetria`).
- Envio de alerta ativo (notificação) ao hospital.
- Autenticação do sensor (na U2: credenciais no broker MQTT).