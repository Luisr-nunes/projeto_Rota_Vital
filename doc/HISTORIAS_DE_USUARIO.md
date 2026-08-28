# Histórias de Usuário - Rota Vital

## HU01 - Registrar doação

**Como** atendente do hemocentro, **quero** registrar uma doação e gerar sua bolsa, **para** disponibilizar o hemocomponente no estoque com origem e validade rastreáveis.

### Regras de negócio

- O doador sintético deve existir e estar ativo.
- A doação e a bolsa devem ser salvas juntas.
- A bolsa deve receber tipo sanguíneo, hemocomponente, validade e status `DISPONIVEL`.

### Cenários BDD

```gherkin
Cenário: Registrar uma doação válida
  Dado que o doador sintético está ativo
  Quando o atendente registrar uma doação com dados válidos
  Então o sistema deve salvar a doação
  E deve gerar uma bolsa disponível no estoque

Cenário: Impedir doação de doador inativo
  Dado que o doador sintético está inativo
  Quando o atendente tentar registrar uma doação
  Então o sistema não deve salvar a doação nem gerar a bolsa
  E deve informar que o doador está inativo
```

## HU02 - Consultar estoque

**Como** profissional do hemocentro, **quero** consultar o estoque por tipo sanguíneo, hemocomponente e validade, **para** saber quais bolsas estão disponíveis para atendimento.

### Regras de negócio

- A consulta deve usar as bolsas armazenadas no banco de dados.
- Bolsas vencidas, bloqueadas ou reservadas não devem aparecer como disponíveis.
- As bolsas disponíveis devem ser ordenadas pela validade mais próxima.

### Cenários BDD

```gherkin
Cenário: Consultar bolsas disponíveis
  Dado que existem bolsas disponíveis de diferentes tipos no estoque
  Quando o profissional filtrar por tipo sanguíneo e hemocomponente
  Então o sistema deve exibir somente as bolsas correspondentes
  E deve ordenar o resultado pela validade mais próxima

Cenário: Consultar estoque sem resultado
  Dado que não existem bolsas disponíveis para o filtro selecionado
  Quando o profissional realizar a consulta
  Então o sistema deve informar que nenhuma bolsa foi encontrada
```

## HU03 - Registrar requisição hospitalar

**Como** profissional de um hospital, **quero** solicitar hemocomponentes informando tipo, quantidade e prioridade, **para** que o hemocentro possa atender à necessidade do hospital.

### Regras de negócio

- A requisição deve estar vinculada a um hospital ativo.
- A quantidade deve ser maior que zero.
- A nova requisição deve ser salva com status `PENDENTE`.

### Cenários BDD

```gherkin
Cenário: Registrar uma requisição válida
  Dado que o hospital está ativo
  Quando o profissional informar tipo, quantidade e prioridade válidos
  Então o sistema deve salvar a requisição com status pendente
  E deve gerar um identificador para acompanhamento

Cenário: Rejeitar quantidade inválida
  Dado que o profissional está preenchendo uma requisição
  Quando informar uma quantidade menor ou igual a zero
  Então o sistema não deve salvar a requisição
  E deve solicitar uma quantidade válida
```

## HU04 - Alocar bolsas compatíveis por FEFO

**Como** profissional do hemocentro, **quero** alocar bolsas compatíveis a uma requisição, priorizando as que vencem primeiro, **para** atender o hospital e reduzir perdas por validade.

### Regras de negócio

- Apenas bolsas compatíveis, disponíveis e dentro da validade podem ser alocadas.
- Entre as bolsas compatíveis, deve ser escolhida primeiro a de validade mais próxima, seguindo o FEFO.
- A bolsa alocada deve ficar reservada para a requisição.

### Cenários BDD

```gherkin
Cenário: Alocar uma bolsa compatível
  Dado que existe uma requisição pendente
  E existem bolsas disponíveis e compatíveis
  Quando o profissional solicitar a alocação
  Então o sistema deve reservar a bolsa compatível com validade mais próxima
  E deve associá-la à requisição

Cenário: Não encontrar bolsa compatível
  Dado que não existe bolsa disponível compatível com a requisição
  Quando o profissional solicitar a alocação
  Então o sistema deve manter a requisição pendente
  E deve informar a falta de estoque compatível
```

## HU05 - Planejar rota de entrega

**Como** operador de logística, **quero** calcular a rota de menor custo até o hospital solicitante, **para** planejar uma entrega eficiente e rastreável.

### Regras de negócio

- A requisição deve possuir ao menos uma bolsa alocada.
- A rota deve usar os hospitais e as conexões armazenados no banco.
- O caminho calculado deve ser salvo e associado à requisição.

### Cenários BDD

```gherkin
Cenário: Calcular uma rota de entrega
  Dado que a requisição possui uma bolsa alocada
  E existe um caminho entre o hemocentro e o hospital
  Quando o operador solicitar o planejamento
  Então o sistema deve calcular a rota de menor custo
  E deve salvar a rota associada à requisição

Cenário: Não existir caminho para o hospital
  Dado que o hospital não possui caminho alcançável no grafo
  Quando o operador solicitar o planejamento
  Então o sistema não deve liberar a entrega
  E deve informar que não existe rota disponível
```

## HU06 - Monitorar a temperatura do transporte

**Como** operador de logística, **quero** acompanhar a temperatura durante o transporte, **para** identificar desvios na cadeia fria antes da conclusão da entrega.

### Regras de negócio

- Cada leitura sintética deve ser salva com transporte, valor, data e hora.
- A faixa aceitável deve estar configurada para o hemocomponente transportado.
- Uma leitura fora da faixa deve gerar um alerta.

### Cenários BDD

```gherkin
Cenário: Registrar temperatura dentro da faixa
  Dado que existe um transporte ativo
  Quando o sistema receber uma leitura dentro da faixa configurada
  Então deve salvar a leitura no banco de dados
  E deve manter o transporte sem alerta de temperatura

Cenário: Alertar temperatura fora da faixa
  Dado que existe um transporte ativo
  Quando o sistema receber uma leitura fora da faixa configurada
  Então deve salvar a leitura
  E deve gerar um alerta associado ao transporte
```

## HU07 - Visualizar painel gerencial

**Como** gestor da rede simulada, **quero** visualizar indicadores de estoque, requisições e transportes, **para** acompanhar a operação e tomar decisões com base nos dados do sistema.

### Regras de negócio

- Os indicadores devem ser calculados usando os dados sintéticos armazenados.
- O painel deve mostrar estoque por tipo sanguíneo, requisições pendentes e alertas de transporte.
- Quando não houver dados, o sistema não deve apresentar números inventados.

### Cenários BDD

```gherkin
Cenário: Exibir indicadores do sistema
  Dado que existem bolsas, requisições e transportes registrados
  Quando o gestor acessar o painel
  Então o sistema deve calcular e exibir os indicadores usando os dados armazenados
  E deve informar que os dados são sintéticos

Cenário: Exibir painel sem dados
  Dado que não existem registros para o período selecionado
  Quando o gestor acessar o painel
  Então o sistema deve apresentar um estado vazio
  E não deve exibir valores fictícios
```
