# Estatística U2 — Relatório final de achados

**Disciplina:** Estatística e Probabilidade
**Atividade:** PI2-196 — M2 · Relatório final de achados (história PI2-35)
**Responsável:** Micaella Cabral
**Data:** 12/11/2026
**Método:** [`est-u2-probabilidade.md`](est-u2-probabilidade.md) (modelo de Poisson, lambdas e fórmulas) · [`indicador-descarte-pi2-178.md`](indicador-descarte-pi2-178.md)

> **Nota:** todos os dados são sintéticos e existem apenas para desenvolvimento e avaliação acadêmica. Não representam doadores, hospitais ou a operação real da Hemorrede.

## 1. Objetivo e fonte dos dados

Aplicar os indicadores probabilísticos da U2 **à base de produção** do Rota Vital e transformar os resultados em decisões de estoque: quais tipos repor primeiro, quanto manter de cada um e o que fazer com as bolsas que vencem.

Todos os números vêm da API de produção (`https://rota-vital-jnmx.onrender.com`), consultada em 12/11/2026 às 10:29. As respostas completas estão em [`doc/evidencias/est-u2/`](evidencias/est-u2/):

| Indicador | Endpoint | Arquivo |
|---|---|---|
| Estoque e requisições (U1) | `GET /api/indicadores` | `indicadores.json` |
| Probabilidade de desabastecimento | `GET /api/v1/indicadores/risco-desabastecimento?dias=1`, `3` e `7` | `risco-1.json`, `risco-3.json`, `risco-7.json` |
| Taxa de descarte por vencimento | `GET /api/v1/indicadores/descarte?dias=7` e `30` | `descarte-7.json`, `descarte-30.json` |
| Histórico de requisições | `GET /api/v1/requisicoes` | `requisicoes.json` |

## 2. Situação da base em 12/11/2026

| Medida | Valor |
|---|---:|
| Bolsas disponíveis e dentro da validade | **0** |
| Bolsas que chegaram à validade nos últimos 30 dias | 8 |
| Dessas, marcadas como `DESCARTADA` | 0 |
| Requisições registradas | 3 (1 pendente, 1 aprovada, 1 entregue) |
| Bolsas pedidas nessas requisições | 4 |
| Data da requisição mais recente | 04/10/2026 |
| Tempo médio de atendimento (U1) | 45 min |

A carga inicial do banco de produção foi feita em 04/10/2026, com validades variadas para demonstrar o FEFO, e nenhuma doação nova foi registrada depois. Resultado: em 12/11 **todo o estoque de hemácias venceu**, e o sistema está exatamente na situação que os indicadores da U2 existem para evitar.

## 3. Probabilidade de desabastecimento por tipo

Com estoque `S = 0`, qualquer pedido já é falta. A fórmula da seção 4.1 do método se reduz a:

```text
P(D_t(N) > 0) = 1 − P(D_t(N) = 0) = 1 − e^{−λ_t × N}
```

| Tipo | λ (bolsas/dia) | Estoque | P(falta) em 1 dia | P(falta) em 3 dias | P(falta) em 7 dias | Faixa do painel (1 dia) |
|---|---:|---:|---:|---:|---:|---|
| O+ | 1,44 | 0 | **76,31%** | 98,67% | 100,00% | vermelho |
| A+ | 1,36 | 0 | **74,33%** | 98,31% | 99,99% | vermelho |
| O− | 0,36 | 0 | 30,23% | 66,04% | 91,95% | amarelo |
| A− | 0,32 | 0 | 27,39% | 61,71% | 89,35% | amarelo |
| B+ | 0,32 | 0 | 27,39% | 61,71% | 89,35% | amarelo |
| AB+ | 0,10 | 0 | 9,52% | 25,92% | 50,34% | verde |
| B− | 0,08 | 0 | 7,69% | 21,34% | 42,88% | verde |
| AB− | 0,02 | 0 | 1,98% | 5,82% | 13,06% | verde |

**Conferência do cálculo (O+, 1 dia):** `1 − e^{−1,44} = 1 − 0,2369 = 0,7631`, o mesmo valor devolvido pela API (`0.763072`). Os outros 23 valores também batem com a fórmula até a quarta casa.

**Achados:**

1. **O+ e A+ faltam já no primeiro dia** em 3 de cada 4 cenários possíveis. São os tipos mais pedidos (juntos, 70% da demanda) e são a prioridade absoluta de reposição.
2. **O−, A− e B+ ficam no amarelo** no curto prazo, mas passam de 89% em uma semana. A janela para repor sem falta é de poucos dias.
3. **AB+, B− e AB− parecem seguros** pelo indicador, mas só porque a demanda é baixa. AB− tem 13% de chance de faltar em 7 dias com estoque zero; o risco existe, só demora mais para aparecer.
4. Como o modelo não considera substituição entre tipos compatíveis (método, seção 2.3), o risco real de O−, que é doador universal, é **maior** do que o calculado: em falta de outros negativos, os pedidos de A−, B− e AB− também consomem O−.

## 4. Taxa de descarte por vencimento

| Janela | Bolsas que chegaram à validade | Marcadas `DESCARTADA` | Taxa devolvida pela API |
|---|---:|---:|---:|
| 7 dias (06/11–12/11) | 1 (O+) | 0 | 0,0% |
| 30 dias (14/10–12/11) | 8 (O+ 2; A+, A−, B+, B−, O−, AB+ 1 cada) | 0 | 0,0% |

**O indicador está correto pela definição, mas esconde a perda.** A taxa mede quantas das bolsas que venceram foram marcadas como descartadas. Nenhuma foi marcada, então a taxa é 0%. Na prática, as 8 bolsas que venceram nos últimos 30 dias **não foram usadas**: a perda por vencimento foi de 8 em 8 (100%). A carga inicial cria as bolsas como `DISPONIVEL` e nada no sistema muda esse status quando a validade passa; elas só deixam de aparecer no estoque porque a consulta filtra pela validade.

Isso confirma a limitação registrada no método (seção 5.1): sem um processo que marque as bolsas vencidas como `DESCARTADA`, a taxa de descarte fica em 0% mesmo quando tudo se perde.

**Probabilidade de descarte (olhando para a frente).** Pela política FEFO, a próxima bolsa a vencer só se perde se nenhum pedido compatível chegar antes da validade `d`: `P = e^{−λ × d}`. Para uma bolsa que vence em 7 dias:

| Tipo | λ | P(vencer sem uso em 7 dias) |
|---|---:|---:|
| O+ | 1,44 | 0,00% |
| A+ | 1,36 | 0,01% |
| O− | 0,36 | 8,05% |
| A− / B+ | 0,32 | 10,65% |
| AB+ | 0,10 | 49,66% |
| B− | 0,08 | 57,12% |
| AB− | 0,02 | **86,94%** |

Tipos comuns praticamente não vencem na prateleira; os raros quase sempre vencem se ficarem parados num só hemocentro.

## 5. Previsão simples de demanda

A previsão segue a orientação do professor: simples, sem aprendizado de máquina.

**Método.** Média móvel de 7 dias, que é também o estimador de máxima verossimilhança de λ na Poisson (método, seção 3.1):

```text
λ̂_t = (bolsas do tipo t pedidas nos últimos 7 dias) ÷ 7
previsão para os próximos N dias = λ̂_t × N
```

**Aplicação à produção.** A última requisição é de 04/10/2026. Nos últimos 7 dias (e nos últimos 30) não houve pedidos, então a média móvel daria λ̂ = 0 para todos os tipos. Pela regra do método, **ausência de registros não é demanda zero**: a base não está recebendo pedidos porque o sistema não está em operação, não porque os hospitais pararam de precisar de sangue. Nesse caso a previsão usa os lambdas de referência. É exatamente o que faz o endpoint `GET /api/v1/indicadores/previsao-demanda` (M1, PI2-195, entregue em 14/11): sem pedidos de um tipo na janela, ele devolve `origem` = referência em vez de λ = 0. Desde a M1, o risco de desabastecimento também usa esse λ, então os números da seção 3 continuam valendo enquanto a base não receber pedidos novos.

**Previsão para os próximos 7 dias (lambdas de referência, intervalo de 90% pela Poisson):**

| Tipo | Demanda esperada (bolsas) | Intervalo de 90% |
|---|---:|---:|
| O+ | 10,1 | 5 a 16 |
| A+ | 9,5 | 5 a 15 |
| O− | 2,5 | 0 a 5 |
| A− | 2,2 | 0 a 5 |
| B+ | 2,2 | 0 a 5 |
| AB+ | 0,7 | 0 a 2 |
| B− | 0,6 | 0 a 2 |
| AB− | 0,1 | 0 a 1 |
| **Total** | **28,0** | |

O intervalo mostra por que a média sozinha não basta para decidir o estoque: para O+, uma semana "normal" pode pedir de 5 a 16 bolsas.

## 6. Recomendações de estoque

### 6.1 Estoque-alvo por tipo

Estoque mínimo para que a chance de faltar em 7 dias, sem reposição, fique abaixo de 10% (nível de serviço de 90%) e de 5% (95%). Calculado como o menor `S` com `P(D(7) > S) ≤ 10%` (ou 5%).

| Tipo | Nível 90% | Nível 95% | Ação recomendada hoje |
|---|---:|---:|---|
| O+ | 14 | 16 | **Repor imediatamente** (risco de 76% em 1 dia) |
| A+ | 14 | 15 | **Repor imediatamente** (risco de 74% em 1 dia) |
| O− | 5 | 5 | Repor em até 2 dias; também cobre falta de outros negativos |
| A− | 4 | 5 | Repor em até 3 dias |
| B+ | 4 | 5 | Repor em até 3 dias |
| AB+ | 2 | 2 | Manter 1 a 2 bolsas; pode ser atendido por O+, A+ e B+ |
| B− | 2 | 2 | Não estocar no hemocentro: pedir por remanejamento |
| AB− | 1 | 1 | Não estocar: atender com O−, A− ou B− (matriz ABO/Rh) |

Total no nível de 90%: **46 bolsas de hemácias**, contra 0 hoje.

### 6.2 Decisões que os achados apoiam

| Achado | Decisão |
|---|---|
| O+ e A+ com risco acima de 70% em 1 dia | Campanha de coleta direcionada a O+ e A+ antes de qualquer outro tipo |
| Tipos raros (AB−, B−) com mais de 50% de chance de vencer parados | Centralizar o estoque de raros numa unidade e remanejar sob demanda, em vez de manter uma bolsa em cada hemocentro |
| O− cobre todos os receptores Rh− | Manter O− no nível de 95% e usar a alocação FEFO entre tipos compatíveis (HU08), que já reserva a compatível que vence primeiro |
| Taxa de descarte em 0% com 8 bolsas perdidas | Incluir no sistema uma rotina diária que marque como `DESCARTADA` toda bolsa `DISPONIVEL` com validade vencida, para o indicador refletir a perda real |
| Nenhuma doação registrada desde 04/10 | Incluir no painel um alerta de "estoque zerado por tipo", que hoje só aparece indiretamente pelo risco de 100% |

## 7. Limitações

- **Base pequena e sintética:** 3 requisições não permitem estimar os oito lambdas. As probabilidades usam os parâmetros de referência declarados no método (seção 3.2); o método está correto, mas os números descrevem um cenário de demonstração.
- **Estoque congelado:** como nenhuma doação foi registrada depois da carga inicial, a base de produção não mostra reposição nem consumo ao longo do tempo. A série de demanda é plana desde 04/10.
- **Sem substituição entre tipos no risco:** o modelo calcula cada tipo isolado, o que subestima o risco de O− e superestima o de AB−.
- **Sem data nem motivo do descarte:** a entidade `Bolsa` não guarda quando nem por que a bolsa foi descartada; o indicador assume que todo descarte é por vencimento.
- **Só hemácias:** plasma, plaquetas e crioprecipitado não têm carga suficiente para o modelo.
- **Previsão sem sazonalidade:** a média móvel não captura feriados, fins de semana ou campanhas, que mudam a demanda real de sangue.

## 8. Como reproduzir

```bash
curl "https://rota-vital-jnmx.onrender.com/api/v1/indicadores/risco-desabastecimento?dias=1"
curl "https://rota-vital-jnmx.onrender.com/api/v1/indicadores/descarte?dias=30"
curl "https://rota-vital-jnmx.onrender.com/api/indicadores"
```

Os valores das seções 5 e 6 podem ser conferidos no Python com SciPy:

```python
from scipy.stats import poisson
mu = 1.44 * 7                                                   # O+ em 7 dias
poisson.ppf([0.05, 0.95], mu)                                   # intervalo de 90%: 5 e 16
next(s for s in range(60) if poisson.sf(s, mu) <= 0.10)         # estoque-alvo 90%: 14
```

O painel consolidado (`gestor/indicadores.html`, PI2-197) mostra os mesmos cards de risco e o gráfico de descarte, lidos da API em tempo real.
