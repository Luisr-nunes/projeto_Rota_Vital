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
- HU06: dois cenarios (entrega de requisicao alocada e recusa 409 de requisicao
  pendente). Escritos antes do endpoint; desbloqueados com a PI2-181 e agora
  executados na suite padrao.

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

## HU06 — confirmar entrega (PI2-181)

Contrato do Jira PI2-127/PI2-181, implementado em `RequisicaoService.confirmarEntrega`:

- `PATCH /api/v1/requisicoes/{id}/entrega` (alias legado `/api/requisicoes/{id}/entrega`);
- requisicao ALOCADA (ou APROVADA/EM_ROTA) passa a ENTREGUE; bolsas RESERVADA
  (ou EM_TRANSITO) passam a UTILIZADA; resposta 200 com a requisicao;
- requisicao nao alocada (PENDENTE, ENTREGUE, CANCELADA) retorna 409 sem alterar nada;
- requisicao inexistente retorna 404.

Os dois cenarios de `confirmar_entrega.feature` rodam na suite padrao (a tag
`@pendente_PI2_181` e o filtro de `junit-platform.properties` foram removidos).
Cobertura complementar em `ConfirmarEntregaIntegrationTest`.

```bash
mvn -Dtest=AceitacaoBddTest -Dcucumber.filter.tags=@HU06 test
```

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
HTML. A quantidade de cenarios HU08 e HU06 executados deve aparecer separadamente. Teste local verde nao comprova deploy nem execucao verde do CI.
