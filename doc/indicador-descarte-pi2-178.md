# Indicador de descarte por vencimento — PI2-178

Endpoint: `GET /api/v1/indicadores/descarte?dias=7` (também disponível em `/api/indicadores/descarte`). O período padrão é 7 dias; valores não inteiros são rejeitados pelo Spring e valores menores ou iguais a zero retornam HTTP 400.

## Definição operacional adotada

Para cada grupo de tipo sanguíneo e hemocomponente, a taxa é:

`bolsas marcadas DESCARTADA com validade nos últimos N dias / total de bolsas cuja validade caiu nos últimos N dias × 100`.

A janela inclui hoje e os N−1 dias anteriores. O indicador representa a proporção das bolsas que chegaram à validade no período e foram registradas como descartadas. A aplicação usa `dataValidade` como referência temporal porque `Bolsa` não armazena a data efetiva do descarte; o job `ValidadeService` marca como `DESCARTADA` bolsas vencidas que ainda estavam disponíveis.

A resposta inclui o tamanho do denominador, numerador, percentual, período e data/hora do cálculo. Grupos sem bolsas com validade no período não são devolvidos; um banco vazio retorna `taxas: []`, sem percentuais artificiais.

## Dados de demonstração

A massa sintética existente tem bolsas com datas relativas a hoje e pode não conter bolsas com validade nos últimos sete dias. Os testes usam fixtures com uma amostra sintética e validam o cálculo por tipo/componente. Para demonstrar uma amostra no painel, as bolsas de teste devem cobrir ambos os estados (`DESCARTADA` e não descartada) dentro da janela. Não foram alterados os dados globais do `DataInitializer`.

## Limitação

Como não há `dataDescarte` nem motivo do descarte, o endpoint infere o evento por `status = DESCARTADA` e validade na janela. Esta definição pressupõe que `DESCARTADA` represente descarte por vencimento; se houver descarte por outros motivos, o domínio precisa registrar o motivo/data para separar os casos.
