# Histórias de Usuário - Rota Vital

## HU01 - Cadastrar uma bolsa no estoque

> “Como atendente do hemocentro, quero registrar uma doação e cadastrar a bolsa coletada, para que ela fique disponível no estoque e possa atender futuras solicitações dos hospitais.”

### Regras de negócio

- O atendente deve informar o doador sintético, o tipo sanguíneo, o hemocomponente e a validade da bolsa.
- O doador informado deve existir e estar ativo.
- A doação e a bolsa devem ser salvas juntas, e a bolsa deve entrar no estoque com status `DISPONIVEL`.

### Cenários BDD

```gherkin
Cenário: Cadastrar uma nova bolsa no estoque
  Dado que o doador sintético está ativo
  Quando o atendente registrar a doação e os dados da bolsa coletada
  Então o sistema deve salvar a doação
  E deve cadastrar a bolsa como disponível no estoque

Cenário: Impedir cadastro de bolsa para doador inativo
  Dado que o doador sintético está inativo
  Quando o atendente tentar registrar a doação e cadastrar a bolsa
  Então o sistema não deve salvar a doação nem cadastrar a bolsa
  E deve informar que o doador está inativo
```

## HU02 - Consultar estoque

> “Como profissional do hemocentro, quero consultar o estoque por tipo sanguíneo, hemocomponente e validade, para saber quais bolsas estão disponíveis para atendimento.”

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

> “Como profissional de um hospital, quero solicitar hemocomponentes informando tipo, quantidade e prioridade, para que o hemocentro possa atender à necessidade do hospital.”

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

> “Como profissional do hemocentro, quero alocar bolsas compatíveis a uma requisição, priorizando as que vencem primeiro, para atender o hospital e reduzir perdas por validade.”

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

> “Como operador de logística, quero calcular a rota de menor custo até o hospital solicitante, para planejar uma entrega eficiente e rastreável.”

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

## HU06 - Confirmar entrega ao hospital

> “Como operador de logística, quero confirmar a entrega das bolsas ao hospital, para registrar que a solicitação foi atendida e manter o histórico da operação atualizado.”

### Regras de negócio

- A entrega só pode ser confirmada quando existir uma rota e houver bolsas associadas à requisição.
- O sistema deve registrar a data e a hora da entrega.
- Ao confirmar a entrega, a requisição deve mudar para `ATENDIDA` e as bolsas para `ENTREGUE`.

### Cenários BDD

```gherkin
Cenário: Confirmar uma entrega realizada
  Dado que a requisição possui bolsas associadas e uma rota registrada
  Quando o operador confirmar a entrega ao hospital
  Então o sistema deve registrar a data e a hora da entrega
  E deve alterar a requisição para atendida
  E deve alterar as bolsas para entregues

Cenário: Impedir confirmação sem bolsas associadas
  Dado que a requisição não possui bolsas associadas
  Quando o operador tentar confirmar a entrega
  Então o sistema não deve concluir a requisição
  E deve informar que não existem bolsas para entregar
```

## HU07 - Visualizar painel gerencial

> “Como gestor da rede simulada, quero visualizar indicadores de estoque, requisições e entregas, para acompanhar a operação e tomar decisões com base nos dados do sistema.”

### Regras de negócio

- Os indicadores devem ser calculados usando os dados sintéticos armazenados.
- O painel deve mostrar estoque por tipo sanguíneo, requisições pendentes e entregas realizadas.
- Quando não houver dados, o sistema não deve apresentar números inventados.

### Cenários BDD

```gherkin
Cenário: Exibir indicadores do sistema
  Dado que existem bolsas, requisições e entregas registradas
  Quando o gestor acessar o painel
  Então o sistema deve calcular e exibir os indicadores usando os dados armazenados
  E deve informar que os dados são sintéticos

Cenário: Exibir painel sem dados
  Dado que não existem registros para o período selecionado
  Quando o gestor acessar o painel
  Então o sistema deve apresentar um estado vazio
  E não deve exibir valores fictícios
```
