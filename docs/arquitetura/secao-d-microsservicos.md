# Seção D — Projeção com microsserviços

Projeção de como o Rota Vital seria se cada contexto da A.2 virasse um serviço. A decisão adotada para a Parte 3 está no [ADR 0003](adr/0003-decomposicao-modular-parte2.md), que complementa o [ADR 0001](adr/0001-decomposicao-por-contexto.md): continua um monolito modular, e esta projeção mostra onde a separação compensaria primeiro. Diagramas: [`servicos.mmd`](diagramas/servicos.mmd) e [`saga-requisicao.mmd`](diagramas/saga-requisicao.mmd).

## D.1 Quais contextos viram serviços, e por quê

| Contexto | Vira serviço? | Por quê |
|---|---|---|
| **Estoque** (core) | Sim | Guarda o agregado da Seção B e exige consistência forte dentro do seu banco; muda por motivos próprios (validade, FEFO, compatibilidade). |
| Captação | Não: vai junto com o Estoque | Volume baixo, muda junto com o Estoque e alimenta o mesmo banco de bolsas. Continua um contexto separado no código. |
| **Requisição** | Sim | Orquestra o fluxo do pedido e guarda o estado da saga (D.4); muda por prioridade e aprovação. |
| **Expedição** (core) | Sim | A roteirização usa CPU e tem o grafo de rotas como dado próprio; escala por número de rotas, não de bolsas. |
| **Telemetria** | Sim, o primeiro a justificar | Recebe até 500 leituras/s, guarda estado em memória e exige **uma única instância** (fila e agregador). Escala diferente de todo o resto (Seção E). |
| **Indicadores** | Sim, como serviço de leitura | Com um banco por serviço, a junção de dados deixa de existir: passa a manter a própria visão, alimentada por eventos. |
| Identidade | Não | Subdomínio genérico: solução pronta. |

Resultado: cinco serviços (Estoque com Captação, Requisição, Expedição, Telemetria, Indicadores), mais um provedor de identidade e um broker de mensagens.

## D.2 Comunicação

| De → Para | Estilo | Por quê |
|---|---|---|
| Requisição → Estoque: reservar | **REST síncrono**, comando `ReservarBolsas` com chave de idempotência (id da requisição) | A Requisição precisa da resposta para decidir se aprova; repetir o comando devolve o mesmo resultado. |
| Requisição → Expedição | **Evento** `RequisicaoAprovada` na fila | A Expedição não precisa responder na hora; se estiver fora do ar, a mensagem espera. |
| Expedição → Requisição, Estoque | **Eventos** `RotaIniciada`, `EntregaConfirmada` | Vários consumidores reagem ao mesmo fato. |
| Telemetria → Expedição | **Evento** `TemperaturaForaDaFaixa` | A Telemetria avisa; a Expedição decide sobre a rota em andamento. |
| Estoque → Indicadores | **Eventos** `BolsaVencida`, `EstoqueAbaixoDoMinimo` | Indicadores só consome. |

Cada serviço grava o evento numa tabela de saída (padrão *outbox*) na mesma transação da mudança, e um processo à parte publica, para não perder evento se o serviço cair. A escolha do broker e o preço dele ficam para a Parte 3 e **não entram na conta de custo** da Seção E.

## D.3 Dados: um banco por serviço

Estoque (com Captação), Requisição, Expedição e Indicadores têm um PostgreSQL próprio; a Telemetria guarda leituras em memória (tabela ou Redis se houver mais de uma instância). **Nenhum serviço lê o banco de outro:** dado de outro contexto entra por evento ou pela API pública do dono, e as referências são só por identificador (`hospitalId`, `requisicaoId`, `rotaId`). Custo real da separação: o mesmo hospital existe em dois bancos, com identificação na Requisição e nó e coordenadas na Expedição.

## D.4 Saga: requisição → reserva → rota → entrega

Hoje aprovar uma requisição é uma única transação (`RequisicaoService.aprovar`, `@Transactional`): se a reserva do segundo item falha, o banco desfaz a do primeiro. Com bancos separados isso não existe mais, e o fluxo vira uma **saga orquestrada** pela Requisição, que guarda em qual passo está e manda o próximo comando.

| Passo | Serviço | Ação | Se der errado: compensação |
|---|---|---|---|
| 1 | Requisição | Registra a requisição (`PENDENTE`) | Nada a desfazer |
| 2 | Estoque | `ReservarBolsas` com **todos** os itens, numa só operação do agregado | `ReservaRecusada`: a requisição fica `PENDENTE`. Não há reserva parcial, porque o agregado garante tudo ou nada (I4) |
| 3 | Requisição | Aprova (`APROVADA`) e grava `RequisicaoAprovada` na saída | Falha ao gravar: a transação local desfaz e a saga reenvia |
| 4 | Expedição | Calcula rota e escolhe veículo e motorista | `RotaImpossivel` (hospital sem nó no grafo ou sem veículo com a refrigeração exigida): a Requisição manda `LiberarReserva`, as bolsas voltam a `DISPONIVEL` e a requisição a `PENDENTE` |
| 5 | Expedição | Inicia a rota | Cancelada antes de sair: mesma compensação do passo 4 |
| 6 | Expedição | Confirma a entrega: bolsas `UTILIZADA`, requisição `ENTREGUE` | Em trânsito, `TemperaturaForaDaFaixa` e a Expedição aborta: as bolsas viram `DESCARTADA` e a requisição volta a `PENDENTE`. Aqui a compensação **refaz**, não desfaz: bolsa estragada não volta ao estoque |

Todo comando e evento pode chegar duas vezes, então a chave é o `requisicaoId` (`ReservarBolsas` repetido devolve o resultado já gravado; `LiberarReserva` repetido não faz nada). Entre os passos 2 e 4 o Estoque mostra as bolsas como `RESERVADA` sem rota ainda: é consistência eventual, que no monolito não existe.

## D.5 Monolito modular × microsserviços

| Critério | Monolito modular (adotado) | Microsserviços (projeção) |
|---|---|---|
| **Unidade de deploy** | Uma imagem Docker, uma esteira, um serviço no Render, rollback em um clique. | Cinco imagens e cinco esteiras, com versões compatíveis entre si; mudança que atravessa contextos exige deploy coordenado. |
| **Escala** | Vertical nos cenários baixo e moderado; no alto, só a API horizontal e a Telemetria extraída. | Cada serviço escala sozinho, mas só Telemetria (500 leituras/s) e Expedição (CPU) ganham de verdade. |
| **Consistência dos dados** | ACID: uma transação cobre reserva, aprovação e rota. | Eventual: saga com compensação, idempotência e estado de saga a manter. |
| **Custo** | US$ 7 / 50 / 209 a 275 por mês nos três cenários (Seção E). | No piso, 5 serviços sempre ligados: 5 × US$ 7 = US$ 35, contra US$ 7 no cenário baixo, mais broker e bancos separados (não orçados). |
| **Complexidade de operação** | Um log, um health check, um painel; um problema se investiga num lugar. | Logs e métricas em cinco serviços, rastreamento distribuído, fila para monitorar, testes de contrato; pressupõe equipes por serviço, e o squad tem sete pessoas. |

**Leitura.** A única separação que a medição justifica hoje é a Telemetria. Os demais contextos ganhariam independência de deploy, mas pagariam com consistência eventual, mais operação e mais custo, sem carga que exija isso. Por isso o ADR 0003 mantém o monolito modular e registra a Telemetria como primeiro candidato a sair. O ADR 0004 ([0004](adr/0004-anotacoes-jpa-camada-dados.md)) trata de onde ficam as anotações JPA e complementa o [ADR 0002](adr/0002-mapeamento-jpa-nas-entidades.md).
