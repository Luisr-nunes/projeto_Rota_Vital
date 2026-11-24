# ADR 0001: Decomposição da aplicação por contexto e camada

- **Status:** Aceito
- **Data:** 2026-11-19

## Contexto

A aplicação começou com classes agrupadas principalmente por tipo técnico, como `controller`, `service`, `repository` e `domain`. Com o crescimento das funcionalidades, esse arranjo passou a espalhar regras de uma mesma capacidade por vários pacotes e tornou mais difícil identificar os limites entre Requisição, Expedição e Estoque.

## Decisão

Organizar gradualmente o monólito Spring Boot primeiro por contexto de negócio e, dentro de cada contexto, por camada. Nesta etapa, Requisição e Expedição ficam sob `com.hemorede.requisicao` e `com.hemorede.expedicao`, respectivamente. As entidades e regras ficam em `domain`, os casos de uso e controladores em `application`, e os repositórios em `infrastructure`.

O contexto de Requisição não acessa diretamente repositórios ou regras internas de Estoque para reservar bolsas. Ele usa o caso de uso público `ReservaEstoque`, implementado pelo contexto de Estoque. A reserva mantém as regras de compatibilidade e FEFO dentro do Estoque.

## Consequências

- Fica mais fácil localizar e testar as classes relacionadas a cada fluxo de negócio.
- As dependências entre contextos ficam mais explícitas e passam por uma interface de caso de uso.
- O projeto continua sendo um monólito Spring Boot; a decisão não cria serviços independentes nem muda as rotas HTTP.
- Algumas entidades JPA mantêm associações entre contextos, como Requisição e Rota. Essas referências são necessárias ao modelo atual e devem ser revistas se os contextos forem separados fisicamente no futuro.
- Contextos que ainda usam os pacotes antigos poderão ser reorganizados em etapas posteriores, evitando uma migração ampla em uma única mudança.
