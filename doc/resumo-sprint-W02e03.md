Resumo do sprint — Week 02e03-Mod./Def. Dados/Alg
Visao geral
Periodo planejado: 10–21/08/2026
Concluido em: 28/08/2026
Meta alcançada: Sim. Os três entregáveis principais foram concluídos: modelo de domínio, escopo do grafo de rotas e contratos de API.
Resultado: escopo entregue, porém o fechamento ocorreu após a data planejada do sprint.

Principais conquistas
Modelo de dominio Spring Boot concluido para o Hemorede, incluindo entidades, repositorios, servicos, regras de negocio e controllers REST — https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-12.
Escopo tecnico do grafo e do estoque definido, incluindo Dijkstra, FEFO, hash de estoque, 6 nos fixos e criterios de integracao — https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-13.
Contratos de API documentados, com rotas REST, codigos de resposta, topologia e ambientes Dev/Prod — https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-14.

Bloqueadores e riscos
Validacao dos pesos da matriz de distancias ainda pendente, conforme indicado em https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-13.
Impacto: pode afetar a validação final das rotas, mas não impediu a conclusão do escopo.

Testes unitarios de Dijkstra e alinhamento dos DTOs com POO ainda precisam ser executados/confirmados.
Impacto: risco para a integração REST e para a confiança nos algoritmos no próximo sprint.

O sprint foi encerrado em 28/08, apos o termino planejado em 21/08, indicando atraso de aproximadamente seis dias.

Alteracao de escopo
Adicionado: documentação e anexos técnicos para os três entregáveis do sprint.
Removido: nenhum item identificado.
Transferido: nenhum item identificado; os três itens planejados foram concluídos.
O escopo permaneceu focado em definicao e modelagem, sem incluir ainda janelas de tempo ou cadeia fria no grafo.

Foco recomendado para o proximo sprint
Validar os pesos definitivos da matriz e implementar os testes unitarios de Dijkstra, FEFO e estoque.
Fechar o contrato de DTOs com POO e iniciar a integracao dos servicos com os endpoints REST.
