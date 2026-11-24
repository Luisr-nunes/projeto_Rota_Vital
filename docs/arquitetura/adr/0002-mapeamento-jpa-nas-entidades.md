# ADR 0002: Mapeamento JPA nas entidades do domínio

- **Status:** Aceito
- **Data:** 2026-11-19

## Contexto

O sistema usa Spring Data JPA e precisa mapear entidades e relacionamentos para persistir os dados em H2 durante o desenvolvimento e em PostgreSQL na produção. Era necessário decidir se as anotações de persistência ficariam junto das entidades ou em arquivos de mapeamento separados.

## Decisão

Manter as anotações JPA, como `@Entity`, `@Table`, `@Id`, `@ManyToOne` e `@OneToMany`, nas próprias classes de modelo persistidas. Repositórios continuam em pacotes de infraestrutura e concentram as consultas do Spring Data. Os serviços de aplicação coordenam os casos de uso sem transferir o mapeamento para os controladores.

## Consequências

- O mapeamento pode ser consultado junto à estrutura da entidade e fica simples de manter com Spring Data JPA.
- O modelo de domínio fica dependente de Jakarta Persistence, o que reduz a separação entre domínio e infraestrutura.
- Essa dependência é aceita neste monólito acadêmico para manter o mapeamento explícito e reduzir configuração adicional.
- Consultas e persistência continuam encapsuladas nos repositórios; a decisão não autoriza controladores a acessar diretamente o banco.
