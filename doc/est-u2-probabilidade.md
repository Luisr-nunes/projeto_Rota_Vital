# Estatística U2 — Modelo de demanda e taxa de descarte

**Disciplina:** Estatística e Probabilidade
**Atividade:** PI2-177 (subtarefa da PI2-32 — risco de desabastecimento e taxa de descarte)
**Responsável:** Luis Felipe Farias Nunes
**Data:** 27/10/2026
**Relacionados:** PI2-176 (endpoint de risco), PI2-178 (endpoint de descarte), PI2-179 (painel)

> **Nota:** todos os dados e parâmetros deste documento são sintéticos e existem
> apenas para desenvolvimento e avaliação acadêmica. Não representam doadores,
> hospitais ou a operação real da Hemorrede.

## 1. Objetivo

Definir o método probabilístico que alimenta os dois indicadores da U2:

1. **Probabilidade de desabastecimento por tipo sanguíneo** — chance de a demanda
   de um horizonte de N dias superar o estoque disponível hoje
   (`GET /api/v1/indicadores/risco-desabastecimento?dias=N`).
2. **Taxa de descarte por vencimento** — proporção das bolsas que chegaram à
   validade e foram descartadas (`GET /api/v1/indicadores/descarte?dias=N`).

O documento também fixa os parâmetros (lambdas) que a PI2-176 aguardava e
registra os achados parciais sobre a base sintética.

## 2. Modelo de demanda (Poisson)

### 2.1 Variável aleatória

Para cada tipo sanguíneo `t`, seja `D_t(N)` o número de **bolsas de hemácias
solicitadas** nos próximos N dias. Adota-se:

```text
D_t(N) ~ Poisson(μ_t),   com   μ_t = λ_t × N
```

`λ_t` é a taxa média diária de bolsas solicitadas do tipo `t`.

### 2.2 Por que Poisson

- A demanda é uma **contagem** de eventos (bolsas pedidas) num intervalo de tempo.
- As requisições chegam de hospitais diferentes, de forma aproximadamente
  **independente** entre si.
- A taxa média por dia pode ser tratada como **constante** dentro de um horizonte
  curto (1 a 7 dias), que é o horizonte de decisão do estoque.
- A distribuição é definida por um único parâmetro (λ), o que é adequado para uma
  base pequena: estimar mais parâmetros com poucos dados geraria valores instáveis.

### 2.3 Premissas e limitações

| Premissa | Consequência |
|---|---|
| Unidade da demanda = bolsas (soma de `ItemRequisicao.quantidade`), não número de requisições | Estoque e demanda ficam na mesma unidade |
| Modelo agregado por tipo sanguíneo, apenas hemácias | A base persistida só contém hemácias (relatório U1, seção 3.2); plasma, plaquetas e crioprecipitado ficam fora até haver carga |
| Sem substituição entre tipos compatíveis | O risco de O+ ignora que O− pode atendê-lo; o valor é um **limite superior** do risco real |
| Sem reposição nem vencimentos no horizonte | O estoque é a fotografia de hoje; o risco em 7 dias é conservador |
| Variância = média (propriedade da Poisson) | Se o histórico mostrar dispersão maior (picos), migrar para Binomial Negativa |
| Status contados como demanda: `PENDENTE`, `APROVADA`, `EM_ROTA`, `ENTREGUE` | `CANCELADA` não conta: o pedido não chegou a consumir estoque |

## 3. Estimação dos lambdas

### 3.1 Estimador para quando houver histórico

O estimador de máxima verossimilhança de λ na Poisson é a média amostral:

```text
λ̂_t = (bolsas do tipo t solicitadas na janela) / (dias da janela)
```

Regras de uso:

- **Janela:** últimos 7 dias, ou seja, λ̂_t = unidades solicitadas do tipo t nos últimos 7 dias ÷ 7.
- **Dias sem requisição contam como zero** no denominador. Excluí-los inflaria λ.
- **Sem histórico não é demanda zero:** se a base não tiver requisições registradas
  na janela, mantém-se o parâmetro de referência da seção 3.2 em vez de estimar λ = 0.

### 3.2 Parâmetros de referência adotados agora (cenário sintético)

A base persistida tem 3 requisições e 4 bolsas solicitadas (relatório U1, seção 4).
Com isso não é possível estimar oito lambdas. Por isso a equipe adota um
**cenário sintético declarado**, com duas decisões explícitas:

1. **Demanda total da rede:** λ_total = 4 bolsas/dia. Com o estoque sintético de
   12 bolsas, isso corresponde a cerca de 3 dias de cobertura, uma escala em que
   os riscos variam entre os tipos e o painel tem o que mostrar.
2. **Participação por tipo:** pesos didáticos que preservam a ordem usual de
   frequência ABO/Rh (O+ e A+ mais frequentes, AB− o mais raro). São uma
   premissa de simulação, não uma estatística populacional.

| Tipo | Peso | λ (bolsas/dia) | Propriedade |
|---|---:|---:|---|
| O+ | 0,360 | 1,44 | `O_POS` |
| A+ | 0,340 | 1,36 | `A_POS` |
| O− | 0,090 | 0,36 | `O_NEG` |
| A− | 0,080 | 0,32 | `A_NEG` |
| B+ | 0,080 | 0,32 | `B_POS` |
| AB+ | 0,025 | 0,10 | `AB_POS` |
| B− | 0,020 | 0,08 | `B_NEG` |
| AB− | 0,005 | 0,02 | `AB_NEG` |
| **Total** | **1,000** | **4,00** | |

Os valores estão em `rotavital/src/main/resources/application.yml`, na chave
`hemorede.indicadores.risco.lambda-diario`, lida por `ModeloProbabilisticoDemanda`.
Para trocar o cenário basta alterar essas oito linhas; o código não muda.

## 4. Probabilidade de desabastecimento

### 4.1 Fórmula

Com `S_t` = bolsas do tipo `t` com status `DISPONIVEL` e validade ≥ hoje:

```text
P(desabastecimento_t) = P(D_t(N) > S_t) = 1 − Σ_{k=0}^{S_t} e^{−μ_t} · μ_t^k / k!
```

A comparação é estrita (`>`): se a demanda for igual ao estoque, todos os pedidos
são atendidos e a reserva zera, mas não falta bolsa.

### 4.2 Exemplo de cálculo (O+, N = 1)

`S = 3`, `μ = 1,44 × 1 = 1,44`

```text
P(D ≤ 3) = e^{−1,44} × (1 + 1,44 + 1,44²/2 + 1,44³/6)
         = 0,2369 × (1 + 1,44 + 1,0368 + 0,4977)
         = 0,2369 × 3,9745 = 0,9417
P(D > 3) = 1 − 0,9417 = 0,0583  →  5,83%
```

### 4.3 Leitura no painel

| Faixa de P(desabastecimento) | Cor do card | Ação sugerida |
|---|---|---|
| < 10% | verde | rotina |
| 10% a 50% | amarelo | priorizar coleta do tipo |
| > 50% | vermelho | campanha de doação / remanejamento entre unidades |

## 5. Taxa de descarte por vencimento

### 5.1 Definição (indicador observado)

Já implementada na PI2-178 (`doc/indicador-descarte-pi2-178.md`). Para cada tipo
sanguíneo e hemocomponente, numa janela de N dias que termina hoje:

```text
taxa de descarte = bolsas DESCARTADA com validade na janela
                   ─────────────────────────────────────────── × 100
                   todas as bolsas com validade na janela
```

- O denominador são as bolsas que **chegaram à validade** no período, e não o
  estoque total: assim a taxa mede quanto do que venceu foi de fato perdido.
- Grupos sem bolsas vencendo na janela não aparecem (não se exibe 0% sem base).
- Limitação conhecida: `Bolsa` não tem `dataDescarte` nem motivo. A taxa assume
  que `DESCARTADA` significa descarte por vencimento.

### 5.2 Probabilidade de descarte (indicador previsto)

A taxa observada olha para trás. O mesmo modelo de Poisson permite olhar para a
frente: pela política FEFO, a bolsa com validade mais próxima é a primeira a ser
alocada. Ela só vence na prateleira se **nenhuma** bolsa do tipo for pedida até
a data de validade `d`:

```text
P(descarte da próxima bolsa a vencer) = P(D_t(d) = 0) = e^{−λ_t × d}
```

Isso liga os dois indicadores: tipos com demanda alta têm risco de faltar;
tipos com demanda baixa têm risco de vencer.

## 6. Achados parciais (base sintética, 27/10/2026)

Estoque: 12 bolsas de hemácias disponíveis e válidas (carga padrão do `DataInitializer`).

### 6.1 Risco de desabastecimento

| Tipo | Estoque | λ/dia | Cobertura (dias) | P(falta) em 1 dia | P(falta) em 7 dias |
|---|---:|---:|---:|---:|---:|
| A+ | 2 | 1,36 | 1,5 | 15,69% | 99,59% |
| O+ | 3 | 1,44 | 2,1 | 5,83% | 99,03% |
| A− | 1 | 0,32 | 3,1 | 4,15% | 65,51% |
| B+ | 1 | 0,32 | 3,1 | 4,15% | 65,51% |
| O− | 2 | 0,36 | 5,6 | 0,60% | 46,13% |
| AB+ | 1 | 0,10 | 10,0 | 0,47% | 15,58% |
| B− | 1 | 0,08 | 12,5 | 0,30% | 10,89% |
| AB− | 1 | 0,02 | 50,0 | 0,02% | 0,89% |

Cobertura = estoque ÷ λ (dias até o estoque esperado zerar).

### 6.2 Risco de descarte das bolsas próximas do vencimento

| Bolsa (FEFO) | Vence em | λ/dia | P(nenhum pedido até vencer) |
|---|---:|---:|---:|
| AB− | 7 dias | 0,02 | 86,94% |
| O− | 4 dias | 0,36 | 23,69% |
| O+ | 2 dias | 1,44 | 5,61% |
| A+ | 3 dias | 1,36 | 1,69% |

### 6.3 Taxa de descarte observada

Na carga padrão nenhuma bolsa está `DESCARTADA` e nenhuma teve validade nos
últimos 7 dias. O endpoint retorna `taxas: []`, o comportamento esperado para
"sem base de cálculo". O indicador passa a ter valores quando o job
`ValidadeService` marcar as bolsas vencidas ou quando a carga incluir bolsas
já vencidas.

### 6.4 Interpretação

1. **A+ e O+ concentram o risco de falta.** São os tipos mais pedidos e têm
   cobertura abaixo de 2,5 dias; em uma semana sem reposição a falta é quase certa.
   A+ é o caso mais crítico no curto prazo (15,69% já em 1 dia).
2. **AB− concentra o risco de perda.** A demanda é tão baixa que a única bolsa
   tem 86,94% de chance de vencer sem uso. Manter estoque de tipos raros exige
   remanejamento entre unidades, não só coleta.
3. **O− tem risco nos dois sentidos** (46% de faltar em 7 dias e 24% de a bolsa
   de 4 dias vencer). Como doador universal, o risco real de vencimento é menor,
   porque a matriz ABO/Rh (PI2-157) pode alocá-lo a outros tipos.
4. Os números descrevem um cenário de 12 bolsas e servem para demonstrar o
   método. Não são estimativas da rede real.

## 7. Integração e pendências

| Item | Situação | Responsável |
|---|---|---|
| Lambdas configurados em `application.yml` | feito nesta entrega (endpoint de risco deixa de responder 503) | Luis Felipe |
| Endpoint de risco (`/risco-desabastecimento`) | integrado na main (PI2-176) | Luis Gallindo |
| Endpoint de descarte (`/descarte`) | integrado na main (PI2-178) | Luis Gallindo |
| Cards de risco e gráfico de descarte no `indicadores.html` | pendente, usa as faixas da seção 4.3 | Mariana (PI2-179) |
| Estimar λ pelo histórico (seção 3.1) | evolução opcional da U2 (previsão simples de demanda) | EST |
| `dataDescarte` e motivo em `Bolsa` | sugestão para POO, não bloqueia a U2 | POO |

## 8. Como reproduzir

```bash
cd rotavital
mvn spring-boot:run
curl "http://localhost:8080/api/v1/indicadores/risco-desabastecimento?dias=1"
curl "http://localhost:8080/api/v1/indicadores/risco-desabastecimento?dias=7"
curl "http://localhost:8080/api/v1/indicadores/descarte?dias=7"
```

As probabilidades da seção 6 podem ser conferidas com qualquer calculadora de
Poisson acumulada (por exemplo, `1 - poisson.cdf(S, λ×N)` no SciPy).
