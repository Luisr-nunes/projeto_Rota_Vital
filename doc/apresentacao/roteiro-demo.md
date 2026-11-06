# Roteiro da demo end-to-end — Entrega 04

Vídeo curto (cerca de 4min30s) mostrando o caminho completo de uma bolsa de sangue no
Rota Vital: **requisição → alocação → rota → entrega**. Ênfase nas histórias da Entrega 04
(HU02 consultar estoque, HU08 alocação compatível e HU06 confirmar entrega), com legenda ou
narração.

Ticket: K2 (PI2) · Responsável: Matheus · Gravação até 09/11/2026.

---

## 1. Preparação (antes de gravar)

| # | Item | Como conferir |
|---|---|---|
| 1 | Subir a aplicação **do zero**, para os dados do seed estarem limpos | `cd rotavital && ./mvnw spring-boot:run` (ou `docker compose up`) e abrir `http://localhost:8080` |
| 2 | Não criar nada antes de gravar | A lista de requisições deve ter só as 3 do seed (#1 Pendente, #2 Aprovada, #3 Entregue) |
| 3 | Abrir as abas na ordem da demo | Aba 1: `login.html` · Aba 2: `operacao.html` |
| 4 | Navegador em tela cheia, zoom 100–110%, notificações do sistema desligadas | — |
| 5 | Gravador pronto (OBS, Xbox Game Bar ou Loom), 1080p, microfone testado | Gravar 10 s de teste e ouvir |

A requisição criada na demo será a **#4**. Se aparecer outro número, alguém já criou
requisições nesta execução: reinicie a aplicação.

---

## 2. Roteiro

A demo passa por duas telas. Cada troca é apresentada como uma **troca de perfil**: o
hospital pede e a logística roteiriza na tela de operação; o hemocentro aloca e confirma a
entrega na interface nova.

### Cena 1 — Abertura (0:00–0:20)

| Tela | Ação | Fala / legenda |
|---|---|---|
| Home (`/`) | Mostrar a página inicial | "O Rota Vital apoia a rede de sangue: do pedido do hospital até a bolsa entregue. Vamos acompanhar uma requisição urgente do começo ao fim." |

### Cena 2 — O hospital faz o pedido (0:20–0:55)

| Tela | Ação | Fala / legenda |
|---|---|---|
| `operacao.html` | Clicar em **+ Novo Pedido** | "Primeiro, o hospital registra o pedido." |
| | Preencher: Hospital **Metropolitano Sul** · Prioridade **URGENTE** · **HEMÁCIAS** · Tipo **O+** · Quantidade **2** | "Hospital Metropolitano Sul, urgente: duas bolsas de hemácias para um paciente O+." |
| | Clicar em **Criar Requisição** | "A requisição #4 entra no sistema como pendente." (HU03) |

### Cena 3 — O hemocentro consulta o estoque (0:55–1:30)

| Tela | Ação | Fala / legenda |
|---|---|---|
| `login.html` | Entrar com qualquer e-mail e senha (perfil hemocentro) | "Agora o profissional do hemocentro." |
| Painel do hemocentro | Clicar em **Estoque** no menu | — |
| Estoque | Mostrar o resumo por tipo sanguíneo no topo | "O estoque mostra só bolsas disponíveis, ordenadas pela validade mais próxima." (HU02) |
| | Selecionar **Hemocomponente: Hemácias** e o chip **O** | "Filtrando hemácias do grupo O: temos bolsas O+ e O−." |
| | Clicar na primeira bolsa (O+, vence em 2 dias) e mostrar o detalhe **Vence em breve** | "Esta O+ vence em 2 dias, então tem prioridade máxima na próxima alocação compatível. Guarde essa bolsa." |

### Cena 4 — O hemocentro aloca por compatibilidade + FEFO (1:30–2:30)

| Tela | Ação | Fala / legenda |
|---|---|---|
| Requisições · aba **Pendentes** | Mostrar a fila: #1 e #4, ambas urgentes (a #1 do seed vem antes por ser mais antiga); clicar em **Alocar** na **#4** | "Urgentes aparecem primeiro e, entre elas, vale a ordem de chegada. Vamos atender o nosso pedido, o #4." |
| Detalhe da requisição | Parar em **Bolsas sugeridas por FEFO** | "O sistema só sugere bolsas compatíveis (ABO/Rh), disponíveis e dentro da validade, da que vence primeiro para a que vence por último." |
| | Apontar as duas bolsas: a **O+** que vimos no estoque (vence em 2 dias) e uma **O−** (vence em 4 dias) | "É a mesma O+ que vimos no estoque. E repare: o paciente é O+, mas a segunda bolsa é O−. O− é compatível e vence antes das outras O+. O FEFO atravessa os tipos compatíveis para reduzir o descarte." (HU08) |
| | Clicar em **Aprovar e reservar 2 bolsas** | "As duas bolsas ficam reservadas para este pedido." |
| Modal de sucesso | Mostrar as bolsas com status **Reservada**; clicar em **Voltar à fila** | — |
| (opcional) Estoque | Abrir de novo com o mesmo filtro | "As bolsas reservadas saíram do estoque disponível." |

### Cena 5 — A logística calcula a rota (2:30–3:20)

| Tela | Ação | Fala / legenda |
|---|---|---|
| `operacao.html` (aba 2, recarregar) | Mostrar a #4 com as bolsas reservadas e o selo de FEFO | "Na visão da logística, o pedido já aparece com as bolsas reservadas." |
| | Clicar em **Calcular Rota Dijkstra** na #4 | "A rota sai do hemocentro (N0) até o hospital (N2) pelo menor caminho, calculado com Dijkstra sobre o grafo da rede." (HU05) |
| | Mostrar o caminho destacado no grafo: **N0 → N2, 15 km, ~15 min** e o veículo **ROTA-1001 (refrigerado 2–6 °C)** | "E o sistema escolhe um veículo com a refrigeração exigida por hemácias." |

### Cena 6 — Confirmação da entrega (3:20–4:10)

| Tela | Ação | Fala / legenda |
|---|---|---|
| Requisições (aba 1) · aba **Aprovadas** | Mostrar a #4 e clicar em **Confirmar entrega** | "Quando a bolsa chega ao hospital, a entrega é confirmada." (HU06) |
| Modal de confirmação | Ler o aviso e clicar em **Confirmar entrega** | "A requisição passa a Entregue e as bolsas a Utilizada; a operação não pode ser desfeita." |
| Lista | Mostrar o aviso de sucesso e abrir a aba **Entregues** com a #4 | "O ciclo está completo e o histórico fica registrado." |
| (opcional) aba **Pendentes** | Mostrar a requisição #1 do seed, que não tem o botão | "Requisição que ainda não foi alocada não pode ser entregue: a API responde 409." |

### Cena 7 — Fechamento (4:10–4:30)

| Tela | Ação | Fala / legenda |
|---|---|---|
| Painel do hemocentro | Mostrar o painel | "Pedido, consulta de estoque, alocação compatível por FEFO, rota de menor custo e entrega confirmada: o fluxo inteiro do Rota Vital, com dados 100% sintéticos." |

---

## 3. Pontos de atenção

- **Datas relativas:** o seed cria as bolsas com validade a partir do dia em que a aplicação
  sobe. "Vence em 2 dias" e "em 4 dias" valem em qualquer dia de gravação.
- **Status "Aprovada":** a tela de alocação usa o endpoint `/aprovar`, que deixa a requisição
  como APROVADA (a aba "Aprovadas" mostra APROVADA e ALOCADA juntas). O botão de entrega aceita
  os dois status.
- **Errou um passo?** Reinicie a aplicação: o banco H2 é em memória e volta ao seed.
- **Não mostrar** o console do navegador nem dados pessoais da máquina.

## 4. Plano B

Se uma tela falhar durante a gravação, os mesmos passos rodam pela API. O resultado
abaixo foi conferido em 06/11/2026 com a aplicação recém-iniciada:

```bash
B=http://localhost:8080
curl -X POST $B/api/requisicoes -H 'Content-Type: application/json' \
  -d '{"hospitalId":2,"prioridade":"URGENTE","itens":[{"hemoComponente":"HEMACIAS","tipoSanguineo":"O_POS","quantidade":2}]}'
# → #4 PENDENTE
curl "$B/api/estoques/bolsas?tipo=O_POS&componente=HEMACIAS"
# → 3 bolsas O+ disponíveis; a primeira vence em 2 dias
curl -X POST $B/api/requisicoes/4/aprovar
# → APROVADA; bolsas O+ (vence em 2 dias) e O− (vence em 4 dias) RESERVADA
curl -X POST $B/api/rotas/calcular -H 'Content-Type: application/json' -d '{"requisicaoId":4}'
# → caminho N0 → N2, 15 km, 15 min
curl -X PATCH $B/api/v1/requisicoes/4/entrega
# → ENTREGUE; bolsas UTILIZADA
```

## 5. Depois de gravar

1. Publicar no YouTube (público ou não listado), com legenda ou narração.
2. Adicionar o link no README, seção **Entrega 04 › Screencast do sistema**
   (commit `docs(readme): link do vídeo de demonstração`).
3. Registrar o link do vídeo no comentário do ticket K2 no Jira.
