# Roteiro da demonstração — compatibilidade sanguínea (L2)

Demonstra a regra de compatibilidade ABO/Rh da alocação (HU08) em cerca de **3 minutos**, em dois blocos: os cenários Gherkin rodando e a mesma regra na API com o seed da aplicação. É um complemento do [roteiro da demo ponta a ponta](roteiro-demo.md) (K2), que mostra o caminho completo requisição → alocação → rota → entrega.

Ticket: L2 (PI2). Cenários: [`compatibilidade_sanguinea.feature`](../../rotavital/src/test/resources/features/compatibilidade_sanguinea.feature). Resumo da suíte: [`doc/testes-bdd.md`](../testes-bdd.md).

---

## 1. O que os cenários cobrem

| Tag | O que prova | Cenários |
|---|---|---|
| `@rh_negativo` | Receptor Rh− nunca recebe bolsa Rh+, nem do mesmo grupo nem do O+. Receptor Rh+ aceita Rh− do mesmo grupo. Empate de validade favorece o tipo idêntico; validade mais próxima vence o tipo idêntico (FEFO). | 5 + 4 + 3 + 1 (combinado com vencida) |
| `@so_o_negativo` | Estoque só com O− atende os 8 receptores. O− não recebe O+. Uma bolsa O− não atende dois pacientes nem dois itens da mesma requisição. | 8 + 3 |
| `@bolsa_vencida` | Vencida nunca é reservada, nem sendo a única compatível. Vence hoje ainda vale. Vencida não entra na contagem do 409. Entre duas do mesmo tipo, a válida é a escolhida. | 2 + 3 |

Total: **14 cenários, 29 execuções** (os esquemas de cenário rodam uma vez por linha da tabela). Todos conferem o HTTP **e** o que ficou gravado: status da requisição, status de cada bolsa e as associações.

---

## 2. Preparação

| # | Item | Como conferir |
|---|---|---|
| 1 | Terminal aberto na pasta `rotavital` | `pwd` termina em `rotavital` |
| 2 | Banco limpo para a parte da API | Aplicação reiniciada **antes** de gravar: a requisição criada será a **#4** |
| 3 | Navegador com `rotavital/target/cucumber/relatorio.html` pronto para abrir | Gerado pela execução do bloco A |

---

## 3. Roteiro

### Bloco A — Os cenários rodando (0:00–1:30)

| Tela | Ação | Fala / legenda |
|---|---|---|
| Terminal | `.\mvnw.cmd "-Dtest=AceitacaoBddTest" "-Dcucumber.filter.tags=@compatibilidade" test` | "A regra de compatibilidade está escrita como cenários de aceitação e roda contra a aplicação de verdade: controllers, serviços e banco." |
| Terminal | Mostrar o final da execução: **29 cenários, 0 falhas** | "Vinte e nove execuções verdes." |
| `relatorio.html` | Abrir o relatório e filtrar por `@rh_negativo` | "Primeiro, o fator Rh. Paciente Rh negativo recusa qualquer bolsa Rh positivo, inclusive O positivo, e a bolsa continua disponível." |
| | Filtrar por `@so_o_negativo` e abrir a linha de A+ | "Só O negativo no estoque: atende todos os oito tipos. Mas uma bolsa só atende um paciente." |
| | Filtrar por `@bolsa_vencida` e abrir **Bolsa que vence hoje ainda pode ser reservada** | "Bolsa vencida nunca sai do estoque; a que vence hoje ainda vale." |

Só os cenários de compatibilidade, sem os de outras histórias, vêm do filtro `@compatibilidade`. Para rodar tudo, tire o filtro.

### Bloco B — A mesma regra na API (1:30–3:00)

Com a aplicação recém-iniciada (`.\mvnw.cmd spring-boot:run`), em outro terminal:

```bash
B=http://localhost:8080

# 1. O- pede 3 bolsas. O seed tem 2 O- e 3 O+: so as 2 O- servem.
curl -X POST $B/api/requisicoes -H 'Content-Type: application/json' \
  -d '{"hospitalId":2,"prioridade":"URGENTE","itens":[{"hemoComponente":"HEMACIAS","tipoSanguineo":"O_NEG","quantidade":3}]}'
curl -X POST $B/api/v1/requisicoes/4/alocacao
# esperado: 409 com quantidadeNecessaria 3 e quantidadeDisponivel 2; nada e reservado

# 2. AB- pede 2 bolsas. A O+ de 2 dias e a A+ de 3 dias existem, mas sao Rh+.
curl -X POST $B/api/requisicoes -H 'Content-Type: application/json' \
  -d '{"hospitalId":2,"prioridade":"URGENTE","itens":[{"hemoComponente":"HEMACIAS","tipoSanguineo":"AB_NEG","quantidade":2}]}'
curl -X POST $B/api/v1/requisicoes/5/alocacao
# esperado: 200, bolsas O_NEG (vence em 4 dias) e AB_NEG (vence em 7 dias), com rota e custo
```

| Passo | Fala / legenda |
|---|---|
| 1 | "Paciente O negativo pede três bolsas. Há três O positivo no estoque, mas O positivo não serve. Só duas O negativo servem, então a API responde 409 e não reserva nada." |
| 2 | "Paciente AB negativo pede duas. A O positivo vence em dois dias e a A positivo em três, e mesmo assim ficam de fora porque são Rh positivo. O sistema reserva a O negativo e a AB negativo." |
| Fechamento | "A regra é a mesma nos cenários e na API: o paciente só recebe o que pode receber, a bolsa vencida nunca sai, e entre as compatíveis vale a que vence primeiro." |

A bolsa vencida não aparece no Bloco B porque o seed não cria bolsa vencida. Esse caso fica no Bloco A, onde o cenário cria a bolsa com validade passada.

---

## 4. Pontos de atenção

- **Ordem dos passos do Bloco B:** o 409 do passo 1 não reserva nada, então o passo 2 ainda encontra as duas O−. Se inverter a ordem, o O− reservado muda o resultado do outro.
- **Número da requisição:** os comandos assumem #4 e #5. Se aparecerem outros números, alguém já criou requisições: reinicie a aplicação.
- **Datas relativas:** o seed e os cenários usam validade a partir do dia da execução ("vence em 4 dias" vale em qualquer dia).
- **Escopo:** a regra é a de concentrado de hemácias. Plasma e plaquetas têm outras regras e não estão neste roteiro.
- **Errou um passo?** Reinicie a aplicação: o banco H2 é em memória e volta ao seed.

## 5. Depois de gravar ou ensaiar

1. Guardar o `relatorio.html` gerado e um print do terminal com a contagem de cenários verdes.
2. Registrar no comentário do ticket L2 o link do commit, a execução verde do CI e o print.
