# Testes BDD — PI2-188

## Escopo e estado

Implementacao baseada na main e7e10b3 e na branch luis/pi2-145-alocacao,
recebidas em 19/10/2026. Cucumber executa as features em portugues usando
MockMvc, controllers, servicos e repositorios reais. Os dados sao sinteticos,
armazenados em um H2 exclusivo dos cenarios BDD e limpos antes de cada cenario.
Nenhum controller ou service de negocio e substituido por mock.

- HU08: cenarios executaveis de FEFO entre tipos compativeis, incompatibilidade
  (422), estoque insuficiente (409), bolsa vencida ignorada, hospital sem rota
  e rollback integral quando um segundo item nao pode ser atendido.
  Conferem tambem status e associacoes persistidos, nao apenas o HTTP.
- HU06: dois cenarios e respectivos steps preparados, mas BLOQUEADOS pela
  PI2-181 de Matheus. Estao marcados `@pendente_PI2_181` e excluidos da execucao
  padrao. Nao sao testes aprovados e a PI2-188 NAO esta totalmente concluida.

## Execucao

Na pasta `rotavital`, com JDK 17 (versao declarada no pom) e Maven:

```bash
mvn clean test
```

Somente BDD de alocacao:

```bash
mvn -Dtest=AceitacaoBddTest -Dcucumber.filter.tags=@HU08 test
```

Relatorios: `rotavital/target/cucumber/relatorio.html` e `relatorio.json`.
O runner termina em `Test` para ser descoberto pelo Surefire usado por `mvn test`.
O pipeline deve usar esse comando sem `skipTests`; sua configuracao cabe a PI2-189.

## Desbloquear HU06

O contrato vigente usado aqui e o Jira PI2-127/PI2-181:

- `PATCH /api/v1/requisicoes/{id}/entrega`;
- requisicao ALOCADA passa a ENTREGUE; bolsas RESERVADA passam a UTILIZADA;
- requisicao nao alocada retorna 409.

O teste de sucesso espera HTTP 200; confirmar esse detalhe com Matheus, pois o
ticket nao fixa o codigo de sucesso. O documento antigo de historias usa
ATENDIDA/ENTREGUE, divergindo do Jira e dos enums atuais. Nao renomear os enums
sem alinhar esse contrato.

Apos integrar PI2-181, executar:

```bash
mvn -Dtest=AceitacaoBddTest -Dcucumber.filter.tags=@HU06 test
```

Hoje esse comando deve falhar por endpoint ausente (404), explicitando o bloqueio.
Quando os dois cenarios passarem, remover a tag `@pendente_PI2_181` da feature
e o filtro de `junit-platform.properties`, executar a suite completa e registrar
no Jira o commit e a execucao do CI. Nao marcar a tarefa concluida antes disso.

## Integracao da alocacao — PI2-164

Endpoint canonico: `POST /api/v1/requisicoes/{id}/alocacao`.
O caminho `/api/requisicoes/{id}/alocacao` permanece como alias da branch anterior.
Resposta: `requisicaoId`, `status`, `bolsas`, `rota`, `custoTotal`.
Ausencia de bolsas compativeis: 422; quantidade compativel insuficiente: 409,
com `quantidadeNecessaria` e `quantidadeDisponivel`.
Excecoes centralizadas no `GlobalExceptionHandler`. O cadastro HU03 e o contrato
legado de `/aprovar` da main foram preservados.

`CompatibilidadeSanguineaService` ainda delega ao enum existente `TipoSanguineo`.
A classe MatrizCompatibilidade nao consta dos dois arquivos recebidos: a troca
para a matriz da PI2-165 deve ser feita nesse ponto de integracao, junto de Joao.
O algoritmo existente de compatibilidade foi preservado; os cenarios novos
cobrem hemacias. Regras especificas de outros hemocomponentes nao foram validadas
por esta entrega.

## Evidencias

Registrar no Jira o link do commit publicado, a execucao verde do CI e o relatorio
HTML. A quantidade de cenarios HU08 executados e a pendencia HU06 devem aparecer
separadamente. Teste local verde nao comprova deploy nem execucao verde do CI.
