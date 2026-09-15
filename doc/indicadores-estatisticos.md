# Indicadores estatísticos - Rota Vital

## Objetivo

Apresentar uma visão descritiva do estoque e do atendimento de requisições do
Rota Vital. No ambiente de desenvolvimento, todos os registros utilizados são
sintéticos e não representam pessoas, hospitais ou operações reais. A
implementação desta sprint fica isolada em arquivos próprios e não altera as
entidades, serviços ou configurações já criadas por outros integrantes.

## Indicadores e metodologia

### 1. Nível de estoque por tipo sanguíneo

Conta as bolsas com status `DISPONIVEL` e validade igual ou posterior à data
atual, agrupadas pelos oito tipos sanguíneos.

Além da contagem, são calculadas medidas descritivas sobre os oito grupos:

- média de bolsas por tipo;
- mediana;
- valor mínimo e máximo;
- desvio-padrão populacional.

### 2. Percentual de bolsas próximas do vencimento

Considera as bolsas disponíveis e válidas cuja data de validade esteja entre a
data atual e o fim da janela informada. A janela padrão é de cinco dias.

```text
percentual = (bolsas próximas do vencimento / bolsas disponíveis válidas) * 100
```

### 3. Tempo médio de atendimento

Para a demonstração, a amostra sintética contém o status e o tempo de
atendimento, em minutos, de cada requisição. Os tempos foram definidos como a
diferença entre o instante da solicitação e o instante em que ela foi atendida:

```text
tempo de atendimento = dataAtendimento - dataSolicitacao
```

O indicador é a média aritmética desses tempos. Requisições sem tempo informado
não entram no cálculo. Quando não houver nenhuma observação, o valor retornado
será `null`, evitando apresentar zero como se fosse um tempo real medido.

### 4. Distribuição das requisições por status

Conta as requisições em cada estado: `PENDENTE`, `APROVADA`, `EM_ROTA`,
`ENTREGUE` e `CANCELADA`.

## Análise descritiva da amostra sintética

A amostra padrão contém 12 bolsas disponíveis e válidas. A distribuição por
tipo sanguíneo é:

| Tipo | Quantidade |
|---|---:|
| A+ | 2 |
| A- | 1 |
| B+ | 1 |
| B- | 1 |
| AB+ | 1 |
| AB- | 1 |
| O+ | 3 |
| O- | 2 |

Para essa distribuição, a média é 1,50 bolsa por tipo, a mediana é 1, o mínimo
é 1, o máximo é 3 e o desvio-padrão populacional é 0,71. Isso mostra uma
concentração um pouco maior em O+, enquanto cinco dos oito tipos possuem apenas
uma unidade na amostra.

Na janela de cinco dias, três das 12 bolsas estão próximas do vencimento, o que
corresponde a 25%. Esse resultado indica que, nessa amostra, um quarto do
estoque disponível deve ser priorizado pela política FEFO.

A amostra também contém cinco requisições, das quais três possuem horário de
atendimento. Os tempos são 120, 45 e 30 minutos, resultando em média de 65
minutos. Como a base é pequena e sintética, os números servem para demonstrar
o cálculo e não devem ser interpretados como desempenho real da rede.

## API e painel

Endpoint:

```http
GET /api/indicadores?diasProximoVencimento=5
```

Exemplo resumido de resposta:

```json
{
  "estoque": {
    "totalDisponivel": 12,
    "porTipoSanguineo": { "A_POS": 2, "O_POS": 3 },
    "mediaPorTipo": 1.5,
    "medianaPorTipo": 1.0,
    "minimoPorTipo": 1,
    "maximoPorTipo": 3,
    "desvioPadraoPorTipo": 0.71
  },
  "vencimento": {
    "janelaEmDias": 5,
    "quantidadeProximaDoVencimento": 3,
    "percentualProximoDoVencimento": 25.0
  },
  "requisicoes": {
    "total": 5,
    "atendidas": 3,
    "tempoMedioAtendimentoEmMinutos": 65.0
  }
}
```

Com a aplicação em execução, o painel pode ser acessado em:

```text
http://localhost:8080/indicadores.html
```
