# Rota Vital - Contratos de API e Arquitetura

Este documento detalha os contratos de comunicação da aplicação web **Rota Vital** (Gestão e Distribuição de Hemocomponentes), conforme os requisitos do Projeto Integrador (3º Semestre).

## 1. Endpoints REST

Abaixo estão listadas as rotas principais da API para gestão do estoque, requisições e rotas.

| Método | Caminho | Descrição | Resposta (Corpo) | Status Code |
|--------|---------|-----------|------------------|-------------|
| `POST` | `/api/doacoes` | Registra a entrada de uma nova bolsa de sangue no estoque. | Objeto da bolsa criada (id, tipo, validade). | `201 Created` |
| `GET`  | `/api/estoque` | Retorna a visão geral do estoque de hemocomponentes. | Lista de totais agrupados por tipo sanguíneo e componente. | `200 OK` |
| `GET`  | `/api/estoque/{tipoSangue}` | Consulta as bolsas disponíveis para um tipo sanguíneo específico. | Lista de bolsas detalhadas e ordenadas por validade (FEFO). | `200 OK` |
| `POST` | `/api/requisicoes` | Cria uma requisição hospitalar solicitando bolsas de sangue. | Detalhes da requisição (id, status inicial `PENDENTE`). | `201 Created` |
| `GET`  | `/api/requisicoes/{id}` | Consulta o status e detalhes de uma requisição específica. | Objeto da requisição (bolsas alocadas, status atual). | `200 OK` |
| `PUT`  | `/api/requisicoes/{id}/status` | Atualiza o status de uma requisição (ex: `EM_TRANSITO`, `ENTREGUE`). | Objeto da requisição atualizado. | `200 OK` |
| `POST` | `/api/rotas/calcular` | Aciona o algoritmo de caminhos mínimos (Dijkstra) para uma requisição. | Objeto detalhando a rota otimizada, distância e tempo estimado. | `200 OK` |
| `GET`  | `/api/telemetria/temperatura` | Obtém os dados mais recentes de monitoramento de temperatura. | Lista com histórico recente de leitura de sensores (simulados). | `200 OK` |

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
