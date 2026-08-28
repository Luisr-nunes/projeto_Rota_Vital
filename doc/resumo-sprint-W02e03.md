# Resumo do Sprint — Week 02 e 03: Mod. / Def. Dados / Alg

## Visão Geral
- **Período planejado:** 10–21/08/2026
- **Concluído em:** 28/08/2026
- **Meta alcançada:** Sim. Os três entregáveis principais foram concluídos: modelo de domínio, escopo do grafo de rotas e contratos de API.
- **Resultado:** Escopo entregue, porém o fechamento ocorreu após a data planejada do sprint.

## Principais Conquistas
- Modelo de domínio Spring Boot concluído para a Hemorrede, incluindo entidades, repositórios, serviços, regras de negócio e controllers REST ([PI2-12](https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-12)).
- Escopo técnico do grafo e do estoque definido, incluindo Dijkstra, FEFO, hash de estoque, 6 nós fixos e critérios de integração ([PI2-13](https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-13)).
- Contratos de API documentados, com rotas REST, códigos de resposta, topologia e ambientes Dev/Prod ([PI2-14](https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-14)).

## Bloqueadores e Riscos
- **Pendência:** Validação dos pesos da matriz de distâncias ainda pendente, conforme indicado em [PI2-13](https://csprj-adsr-3p-e5.atlassian.net/browse/PI2-13).
  - *Impacto:* Pode afetar a validação final das rotas, mas não impediu a conclusão do escopo.
- **Pendência:** Testes unitários de Dijkstra e alinhamento dos DTOs com POO ainda precisam ser executados/confirmados.
  - *Impacto:* Risco para a integração REST e para a confiança nos algoritmos no próximo sprint.
- **Atraso:** O sprint foi encerrado em 28/08, após o término planejado em 21/08, indicando atraso de aproximadamente seis dias.

## Alteração de Escopo
- **Adicionado:** Documentação e anexos técnicos para os três entregáveis do sprint.
- **Removido:** Nenhum item identificado.
- **Transferido:** Nenhum item identificado; os três itens planejados foram concluídos.
- *Nota:* O escopo permaneceu focado em definição e modelagem, sem incluir ainda janelas de tempo ou cadeia fria no grafo.

## Foco Recomendado para o Próximo Sprint
- Validar os pesos definitivos da matriz e implementar os testes unitários de Dijkstra, FEFO e estoque.
- Fechar o contrato de DTOs com POO e iniciar a integração dos serviços com os endpoints REST.
