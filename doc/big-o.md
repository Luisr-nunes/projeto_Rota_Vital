# Notação Big O — fluxos do sistema

Complemento de [`aed-complexidade.md`](aed-complexidade.md) (que detalha `FilaFEFO`, `IndiceEstoque` e Dijkstra). Aqui está o custo de cada **fluxo de ponta a ponta** e dos laços apontados na revisão de código.

**Variáveis:** `n` = bolsas disponíveis do hemocomponente pedido · `q` = bolsas pedidas num item · `i` = itens da requisição · `b` = bolsas alocadas na requisição · `V`, `E` = nós e arestas do grafo de rotas (hoje 6 e 15) · `p` = pontos do trajeto (≤ V) · `H` = hospitais · `T` = timers HTTP do Micrometer · `K` = baldes de latência (fixo, 10) · `R` = requisições do hospital.

## 1. Regras de domínio (`domain.regras`)

| Regra | Tempo | Espaço | Observação |
|---|---|---|---|
| `RegraTransicaoRequisicao.permitida` | O(1) | O(1) | `EnumMap` + `EnumSet` |
| `RegraFEFO.comparar` | O(1) | O(1) | até 3 comparações |
| `RegraFEFO.respeitaFEFO` | O(s + r) | O(1) | um máximo e um mínimo |
| `RegraValidadeBolsa.*` | O(1) | O(1) | aritmética de datas |
| `RegraElegibilidadeDoador.avaliar` | O(1) | O(1) | a contagem anual vem pronta do banco |
| `RegraRefrigeracao.exigidas` | O(c) | O(1) | c = componentes; `EnumSet` de 3 faixas |
| `RegraProgressoEntrega.interpolar` | O(p) | O(1) | soma dos pesos + uma passada |

## 2. Fluxos

| Fluxo | Tempo | Dominante |
|---|---|---|
| Alocar um item (`AlocacaoService.alocar`) | **O(n log n)** | montar o índice: n inserções no heap. Selecionar q bolsas custa O(q · (8 + log n)); a auditoria FEFO custa O(n) |
| Alocar/aprovar a requisição | O(i · n log n) | um `alocar` por item |
| Confirmar entrega | O(i + b) | ver seção 3 |
| Calcular rota (Dijkstra com heap) | O((V + E) log V) | grafo pequeno e fixo, na prática constante |
| Despachar | O((V + E) log V + H + b) | Dijkstra + mapa de coordenadas dos hospitais + mudança de status das bolsas |
| Rastrear | O((V + E) log V + H + p) | recalcula a rota (determinística) e interpola |
| Elegibilidade do doador | O(1) + 1 consulta `COUNT` | índice por `doador_id` |
| Login | O(1) + BCrypt (custo fixo ~100 ms, proposital) | busca por e-mail único |
| Listar requisições do hospital | O(R log R) no banco | `ORDER BY data_solicitacao` com filtro por hospital |
| Métricas de rede | O(T · K) = O(T) | ver seção 3 |

## 3. Os pontos levantados na revisão

**`try/catch` dentro do `for` em `RequisicaoService.aprovar`.** Não é erro de compilação e não pesa: em Java o `try` não custa nada quando não há exceção. Funcionalmente está certo: ele traduz a exceção do item para o contrato 422 e a relança, e o `@Transactional` desfaz as reservas dos itens anteriores (RN-08, tudo ou nada). O defeito real estava antes do laço: não havia checagem de status, então uma requisição ENTREGUE podia ser "aprovada" de novo. Isso foi corrigido com a RN-09.

**`for` dentro de `for` em `RequisicaoService.confirmarEntrega`.** Parece O(n²), mas é **O(i + b)**: o laço externo passa pelos itens e o interno pelas bolsas *daquele* item. Cada bolsa é visitada uma vez só.

**`for` dentro de `for` em `MetricasRedeService.percentilMs`.** É **O(T · K)** com K fixo (10 baldes configurados em `application.yml`), então O(T) na prática.

**Corrigido nesta revisão:** a auditoria FEFO usava `List.contains` dentro de um stream (O(n · q)). Agora usa um `Set` por identidade: O(n).

## 4. Melhorias recomendadas (não feitas nesta revisão)

| Prioridade | Ponto | Por quê | Sugestão |
|---|---|---|---|
| Alta | Login sem limite de tentativas | Força bruta de senha | Bucket por IP/e-mail (ex.: Bucket4j) ou bloqueio após N falhas |
| Alta | `ddl-auto: update` em produção | Migração implícita, sem histórico nem rollback | Flyway com scripts versionados |
| Média | `GET /api/v1/requisicoes` faz `findAll` e serializa entidades com relações lazy | N+1 consultas e payload grande quando a base crescer | DTO + `@EntityGraph` ou paginação |
| Média | `EstoqueService` monta o `IndiceEstoque` a cada consulta | O(n log n) por chamada | Já é ordenado no banco; para listas, usar só a query |
| Média | `POST /api/hospitais` recebe a entidade inteira | Atribuição em massa de campos | DTO com validação (agora só HEMOCENTRO acessa) |
| Média | Render gratuito "dorme" após 15 min | Primeiro acesso leva ~1 min | Plano pago ou ping agendado (health a cada 10 min) |
| Baixa | `operacao.html` e `indicadores.html` duplicam telas novas | Dois visuais para a mesma função | Migrar o registro de doação para `hemocentro/` e aposentar a página |
| Baixa | Token em `localStorage` com "manter conectado" | Exposto a XSS | Cookie `HttpOnly` + CSRF se o projeto evoluir |
