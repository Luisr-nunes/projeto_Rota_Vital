# Telemetria de Temperatura da Cadeia Fria — Rota Vital

**Task:** PI2-120 — Especificação do canal de telemetria (RSD)
**Responsável:** Micaella Cabral
**Disciplina:** Redes e Sistemas Distribuídos (RSD)
**Sprint:** 08e09
**Implementação do endpoint:** PI2-121 (Lucas) — este documento é o contrato que ele deve seguir.

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

- Faixa segura para hemácias refrigeradas: **2 °C a 6 °C** (inclusive).
- Leitura fora da faixa → `status: "ALERTA"`. A leitura continua sendo entregue normalmente; o consumidor (painel/aplicação) decide como reagir.

## 3. Frequência de envio

- Um sensor simulado emite **uma leitura a cada 30 segundos** por veículo em trânsito.
- Na U1 a leitura é gerada pela própria aplicação (simulação); não há sensor físico.

## 4. Protocolo e porta

| Fase | Origem → Destino | Protocolo / Porta | Observação |
|------|------------------|-------------------|------------|
| **U1** | Cliente (painel/navegador) → API | `HTTPS/443` na borda; `HTTP/8080` interno até a aplicação | HTTP/JSON, requisição e resposta (síncrono) |
| **U2 (evolução)** | Sensor/veículo → Broker MQTT | `MQTT/1883` (ou `MQTTS/8883` com TLS) | Publicação assíncrona; aplicação assina o tópico e persiste as leituras |

Na U2 o broker ficaria na sub-rede privada de aplicação; a linha do diagrama seria **tracejada** (assíncrona), conforme a convenção da rubrica de RSD.

## 5. Endpoint de consulta (U1)

`GET /api/telemetria/temperatura?veiculoId={id}`

| Situação | Status | Corpo |
|----------|--------|-------|
| Veículo existente | `200 OK` | Payload da seção 1 |
| Veículo inexistente | `404 Not Found` | `{"timestamp": "...", "erro": "IllegalArgumentException", "mensagem": "Veículo não encontrado: {id}"}` |
| `veiculoId` ausente | `400 Bad Request` | Erro padrão de validação |

## 6. Fora de escopo nesta entrega

- Histórico persistido de leituras e gráfico de temperatura.
- Envio de alerta ativo (notificação) ao hospital.
- Autenticação do sensor (na U2: credenciais no broker MQTT).
