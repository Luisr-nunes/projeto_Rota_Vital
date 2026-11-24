# ADR 0004 — Anotações JPA em classes da camada de dados

**Status:** Aceito
**Data:** 23/11/2026 (registra a decisão do squad de 17/11/2026)
**Responsável:** Luis Felipe Farias Nunes
**Complementa:** [ADR 0002](0002-mapeamento-jpa-nas-entidades.md) (mapeamento JPA nas entidades, 19/11/2026). Não o invalida: o 0002 descreve a decisão tomada para o estado atual do código e segue valendo para as classes ainda não migradas. Este ADR registra o alvo alinhado à regra de dependência da entrega de 24/11 (C.2), válido para os agregados migrados na Parte 3, começando pelo Estoque.
**Relacionados:** seções B, C.2 e C.3 do trabalho U2 Parte 2

## Contexto

Hoje as nove entidades do Rota Vital (`com.hemorede.domain.model`) são classes com `@Entity`, `@Id`, `@ManyToOne` e outras anotações de `jakarta.persistence`. A mesma classe guarda as regras de negócio e descreve como ela é gravada no banco.

Os dois enunciados puxam em sentidos diferentes:

- O projeto de POO exige entidades persistidas (mínimo de 3) em Spring Boot.
- O trabalho de Infraestrutura de Software exige que o domínio não dependa de banco (regra de dependência da C.2: nada de `org.springframework` nem `jakarta.persistence` no domínio).

Nenhum dos dois exige que as anotações fiquem na classe de domínio. A persistência com JPA continua, o que está em aberto é onde ela fica.

## Decisão

**O domínio fica em Java puro, e as anotações JPA vão para classes da camada de dados.**

- Cada agregado tem uma classe de domínio sem anotações (por exemplo, `Estoque` e `Bolsa` em `estoque.negocio.dominio`) e uma classe de persistência com as anotações (`EstoqueJpaEntity`, `BolsaJpaEntity` em `estoque.dados`).
- Um mapper na camada de dados converte nos dois sentidos. O repositório JPA (`BolsaRepositoryJpa`) implementa a interface `BolsaRepository`, que mora no domínio.
- A migração começa pelo agregado do Estoque (Seção B), que é o core, e segue contexto a contexto na Parte 3.

## Consequências

**Positivas**

- O domínio roda sem Spring, sem banco e sem container. Os testes do agregado são rápidos, e o cenário Gherkin da Seção B pode rodar direto contra `Estoque.reservar`.
- A regra de dependência da C.2 passa a ser verificável por teste: nenhuma classe de domínio importa `jakarta.persistence`.
- Trocar a forma de persistir (outro banco, outro mapeamento) não mexe nas regras de negócio.
- Preparar uma futura separação em serviços (Seção D) fica mais barato, porque o domínio já não carrega detalhes de banco.

**Negativas**

- Mais classes: cada agregado ganha uma entidade de persistência e um mapper. Para um agregado com poucas classes, isso é pouco; para os nove modelos atuais, é trabalho de migração.
- O mapper é código a manter e a testar. Um campo novo no domínio precisa ser lembrado na entidade JPA, e um teste de ida e volta (domínio → banco → domínio) cobre esse risco.
- Perde-se a conveniência de carregar relacionamentos preguiçosos (`LAZY`) direto no objeto de domínio. O repositório precisa montar o agregado completo.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Manter `@Entity` nas classes de domínio | Menos código, mas o domínio passa a depender de `jakarta.persistence` e a regra da C.2 não se cumpre. |
| Mapeamento em XML (`orm.xml`) mantendo as classes limpas | Separa as anotações sem duplicar classes, mas o suporte de ferramentas é pior e o time não tem prática com esse formato. |
