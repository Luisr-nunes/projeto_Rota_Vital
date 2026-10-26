# Modelo de demanda e taxa de descarte — U2

## Objetivo

Descrever os métodos estatísticos usados para estimar o risco de desabastecimento e acompanhar o descarte de bolsas na Hemorrede simulada. Os dados do projeto são sintéticos; os resultados servem para demonstração e análise acadêmica.

## Modelo probabilístico de demanda

A demanda por bolsas de cada tipo sanguíneo é representada por uma distribuição de Poisson. Esse modelo é adequado para representar a quantidade de solicitações que ocorre em um intervalo de tempo, assumindo uma taxa média de demanda relativamente estável.

Para cada tipo sanguíneo `t`, calcula-se a média diária de unidades solicitadas na janela de observação de sete dias:

```text
λ_t = total de unidades solicitadas do tipo t nos últimos 7 dias / 7
