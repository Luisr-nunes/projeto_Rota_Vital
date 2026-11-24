# Uso de IA

Ferramenta: Claude (Anthropic), usada para redigir o rascunho das seções B, D e E e dos ADRs a partir dos documentos do repositório, condensar as seções A e C da pré-entrega e recalcular as contas. Categoria Condicionado da PO-CESAR-021: toda saída foi conferida pelo responsável antes de entrar na entrega. Só os dados sintéticos do Rota Vital passaram pela ferramenta. **Nenhum preço veio da IA:** os da Seção E são das páginas oficiais, consultadas pelo squad em 12/11/2026.

| # | Uso | Finalidade | Como foi validado |
|---|---|---|---|
| 1 | Seções A e C | Condensar o texto da pré-entrega de 17/11 para caber nas 12 páginas | Conferido com o PDF da pré-entrega; a C.5 é nova e usa os números de `doc/TDD.md` |
| 2 | Seções B e D | Organizar as regras já existentes num agregado e projetar a separação em serviços | Cenário Gherkin comparado com `alocar_bolsa.feature` da `main` (idêntico); passos da saga conferidos com `RequisicaoService.aprovar` |
| 3 | Contas da Seção E | Recalcular carga, instâncias, conexões e custo por pedido | Refeitas na planilha `orcamento-cenarios.xlsx`, com fórmulas; Little conferida com o k6 de `doc/rsd-benchmarking.md` |
| 4 | Diagramas e ADRs | Desenhar os diagramas em Mermaid e registrar as duas decisões de 17/11 | Portas e protocolos conferidos com `doc/topologia.md`; decisões e datas com o texto da pré-entrega |
