# Rota Vital - Contratos de API e Arquitetura

Este documento detalha os contratos de comunicação da aplicação web **Rota Vital** (Gestão e Distribuição de Hemocomponentes), conforme os requisitos do Projeto Integrador (3º Semestre).

## 1. Endpoints REST

Endpoints implementados no código (`rotavital/src/main/java/com/hemorede/controller`) na Sprint 08e09. Base local: `http://localhost:8080`; em produção o acesso externo é sempre `HTTPS/443` (ver `doc/topologia.md`). Corpos em JSON (`Content-Type: application/json`).

| Método | Caminho | Descrição | Resposta (corpo) | Status |
|--------|---------|-----------|------------------|--------|
| `POST` | `/api/doacoes` | Registra a doação e a bolsa coletada, na mesma transação (HU01). | `Bolsa` criada (id, tipo, hemocomponente, datas, status `DISPONIVEL`); cabeçalho `Location: /api/doacoes/{id}` | `201`, `400`, `404`, `422` |
| `GET` | `/api/estoques` | Lista os estoques dos hemocentros. | `List<Estoque>` | `200` |
| `GET` | `/api/estoques/{id}/alerta?tipoSanguineo=&hemoComponente=` | Informa se o estoque está abaixo do mínimo para o tipo/hemocomponente. | `{"abaixoDoMinimo": true\|false}` | `200`, `404` |
| `GET` | `/api/hospitais` | Lista os hospitais. | `List<Hospital>` | `200` |
| `GET` | `/api/hospitais/{id}` | Detalha um hospital. | `Hospital` | `200`, `404` |
| `POST` | `/api/hospitais` | Cadastra um hospital. | `Hospital` criado | `200` |
| `GET` | `/api/requisicoes` | Lista as requisições hospitalares. | `List<Requisicao>` | `200` |
| `POST` | `/api/requisicoes` | Registra uma requisição hospitalar com seus itens (HU03). | `Requisicao` criada | `200` hoje; `201` após PI2-130 |
| `POST` | `/api/requisicoes/{id}/aprovar` | Aprova a requisição e aloca as bolsas por FEFO (HU04). | `Requisicao` atualizada | `200`, `404`, `422` |
| `POST` | `/api/rotas/calcular` | Calcula a rota de menor custo (Dijkstra) do hemocentro ao hospital da requisição (HU05). | `{requisicaoId, caminho[], distanciaTotalKm, tempoEstimadoMinutos}` | `200`, `404`, `422` |
| `GET` | `/api/rotas/proximas-requisicoes` | Lista as requisições prontas para roteirizar, por prioridade. | `List<Requisicao>` | `200` |
| `GET` | `/api/rotas/veiculo-compativel/{requisicaoId}` | Seleciona o veículo com refrigeração compatível com a requisição. | `Veiculo` | `200`, `404`, `422` |
| `GET` | `/api/indicadores?diasProximoVencimento=5` | Indicadores estatísticos de estoque, vencimento e requisições (HU07). | `IndicadoresResponse` | `200` |
| `GET` | `/api/v1/relatorios/historico?tamanho=&modo=&threads=` | Relatório histórico processado de forma sequencial ou paralela (entrega de SO). | `RelatorioHistoricoResponse` | `200`, `404` |
| `GET` | `/actuator/health` | Verificação de saúde para o monitoramento do deploy. | `{"status":"UP"}` | `200`, `503` |
| `GET` | `/api/telemetria/temperatura?veiculoId=` | Última leitura de temperatura do veículo. **Especificado em `doc/telemetria.md`; implementação em PI2-121.** | Leitura de temperatura | `200`, `404` |

### 1.1 Exemplos

**`POST /api/doacoes`** — request:

```json
{
  "doadorId": 1,
  "tipoSanguineo": "O_NEG",
  "hemocomponente": "HEMACIAS",
  "dataColeta": "2026-09-29",
  "validade": "2026-11-10"
}
```

Validações (Bean Validation): todos os campos obrigatórios; `dataColeta` no passado ou hoje; `validade` no futuro. Falha → `400`.

**`POST /api/rotas/calcular`** — request `{"requisicaoId": 1}` → response:

```json
{
  "requisicaoId": 1,
  "caminho": ["N0", "N5", "N2"],
  "distanciaTotalKm": 18.5,
  "tempoEstimadoMinutos": 18.5
}
```

`tempoEstimadoMinutos = distanciaTotalKm / velocidade média (60 km/h) × 60`.

### 1.2 Formato de erro e códigos HTTP

Os erros de regra de negócio seguem o corpo abaixo (`GlobalExceptionHandler`):

```json
{ "timestamp": "2026-09-29T09:00:00", "erro": "DoadorInativoException", "mensagem": "Doador 3 está inativo e não pode realizar doações." }
```

| Status | Quando ocorre | Exceção |
|--------|---------------|---------|
| `201 Created` | Doação registrada (`Location` informado). Em requisições passará a valer após PI2-130; hoje `POST /api/requisicoes` e `POST /api/hospitais` respondem `200` | — |
| `400 Bad Request` | Campo obrigatório ausente/inválido; requisição em estado que não permite a operação | `MethodArgumentNotValidException`, `IllegalStateException` |
| `404 Not Found` | Recurso inexistente (doador, requisição, hospital) | `IllegalArgumentException` |
| `422 Unprocessable Entity` | Regra de negócio violada: doador inativo, doação fora do intervalo mínimo (60 dias homens / 90 dias mulheres), estoque insuficiente, incompatibilidade sanguínea, veículo incompatível, rota indisponível | `DoadorInativoException`, `DoacaoForaDoIntervaloException`, `EstoqueInsuficienteException`, `IncompatibilidadeSanguineaException`, `VeiculoIncompativelException`, `RotaIndisponivelException` |

---

## 2. Diagrama de Topologia

O diagrama abaixo ilustra como os componentes do sistema se comunicam (quem fala com quem):

```text
       [ Clientes Web / Hospitais / Hemocentros ]
                            |
                            | (1) Trafego Externo
                            v
+-------------------------------------------------------+
|                 API Gateway / Load Balancer           |
+-------------------------------------------------------+
                            |
                            | (2) Trafego Interno
                            v
+-------------------------------------------------------+
|             Aplicação Java / Spring Boot              |
|        (Lógica de Negócio, Algoritmos, APIs)          |
+-------------------------------------------------------+
                |                       |
            (3) |                       | (3)
                v                       v
+-------------------------+   +-------------------------+
|     Banco de Dados      |   |   Serviços Externos /   |
|  (Estoque, Requisições) |   |    Telemetria Simulada  |
+-------------------------+   +-------------------------+
```

---

## 3. Tabela de Protocolos

A comunicação na rede é dividida em diferentes camadas e protocolos, garantindo segurança na borda e eficiência internamente.

| Camada | Protocolo / Porta | Descrição |
|--------|-------------------|-----------|
| **Borda (Externa)** | `HTTPS / 443` | Comunicação criptografada e segura entre os clientes (navegadores dos hospitais) e o balanceador de carga / servidor de borda. |
| **Interna (Backend)**| `HTTP / 8080` | Comunicação interna em texto plano dentro da rede privada entre o Gateway e os contêineres/instâncias da aplicação Spring Boot. |
| **Banco de Dados** | `TCP / 5432` | Conexão persistente de dados entre a aplicação Spring Boot e o serviço de banco de dados relacional. |

---

## 4. Ambientes de Execução

O ciclo de desenvolvimento do projeto está mapeado em dois ambientes principais, cada um com configurações de persistência adequadas:

| Ambiente | Banco de Dados | Descrição |
|----------|----------------|-----------|
| **Dev (Desenvolvimento)** | **H2 Database (Local)** | Banco de dados em memória. Utilizado pelos desenvolvedores localmente para execução rápida, testes e validações sem necessidade de infraestrutura externa. Os dados são voláteis e resetados ao reiniciar. |
| **Prod (Produção)** | **PostgreSQL (Nuvem)** | Banco de dados relacional e robusto hospedado na nuvem. Mantém a persistência real dos dados das requisições e do estoque, suportando a concorrência e acessos simultâneos. |
