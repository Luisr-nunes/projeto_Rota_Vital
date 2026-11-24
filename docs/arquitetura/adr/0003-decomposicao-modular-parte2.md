# ADR 0003 — Decomposição da Parte 2: monolito modular com seis contextos

**Status:** Aceito
**Data:** 23/11/2026 (registra a decisão do squad de 17/11/2026)
**Responsável:** Micaella Maria Barbosa Cabral
**Complementa:** [ADR 0001](0001-decomposicao-por-contexto.md) (decomposição por contexto e camada, 19/11/2026). Não o substitui: o 0001 continua valendo e descreve o primeiro passo já feito em Requisição e Expedição. Este ADR acrescenta a decisão de manter um único deploy, a estrutura de pacotes alinhada à entrega de 24/11 e a Telemetria como primeiro contexto a sair.
**Relacionados:** seções A.2, C.3, D e E do trabalho U2 Parte 2

## Contexto

O Rota Vital tem seis bounded contexts (Captação, Estoque, Requisição, Expedição, Telemetria, Indicadores), definidos na Seção A.2. Hoje todos rodam no mesmo processo e no mesmo deploy. Requisição e Expedição já foram reorganizados por contexto (ADR 0001, com `domain`, `application` e `infrastructure`); os demais ainda estão organizados por tipo técnico (`controller`, `service`, `repository`, `domain`).

Duas formas de decompor o sistema estavam em jogo:

1. Manter um único deploy e reorganizar o código por contexto e camada (monolito modular).
2. Separar cada contexto em um serviço com banco próprio (microsserviços), como projetado na Seção D.

Os fatos que pesaram na decisão:

- A carga medida em 12/11/2026 mostrou que o limite é a CPU da instância gratuita (0,1 vCPU), não a arquitetura. Em 100 usuários o sistema entregou 49 req/s com 0,03% de perda e nenhum erro 5xx ([`rsd-benchmarking.md`](../../../doc/rsd-benchmarking.md)).
- O banco não foi gargalo em nenhum dos três cenários de carga.
- Hoje aprovar uma requisição é uma transação única (`RequisicaoService.aprovar`). Separar Estoque e Requisição em bancos diferentes troca essa garantia por uma saga com compensação (D.4).
- A Telemetria é diferente dos demais contextos: guarda estado em memória (`AgregadorTelemetria`, fila `LinkedBlockingQueue`) e pede **uma única instância** para manter um único agregador ([`so-u2-sincronizacao.md`](../../../doc/so-u2-sincronizacao.md)).
- O squad tem sete pessoas e um semestre, e o orçamento de infraestrutura deve ser realista e sem *over-engineering* (Seção E).

## Decisão

**Relação com o ADR 0001.** Os pacotes `application` e `infrastructure` do ADR 0001 passam a se dividir, na estrutura da C.3, em `apresentacao` (controladores), `negocio.aplicacao` (casos de uso) e `dados` (repositórios). O contexto e a regra de que Requisição só reserva bolsas pelo caso de uso público `ReservaEstoque` são mantidos.

**O Rota Vital continua um monolito modular.** Os seis contextos viram pacotes independentes (`com.hemorede.<contexto>.{apresentacao, negocio.aplicacao, negocio.dominio, dados}`, estrutura da C.3), com a regra de dependência da C.2 verificada por teste. Há um único artefato, uma única esteira e um único serviço no Render.

**A Telemetria é o primeiro contexto a sair.** No cenário alto da Seção E, ela roda como um segundo serviço, com a mesma imagem Docker e um perfil de configuração que liga só a ingestão, numa instância fixa. Os demais contextos só se separam se a medição mostrar uma razão concreta (carga ou ritmo de mudança), e a Seção D descreve como seria.

## Consequências

**Positivas**

- Uma transação continua cobrindo reserva, aprovação e rota. O "tudo ou nada" não precisa de saga.
- Um deploy, um rollback e um painel de operação. A esteira atual (`ci.yml`, auto-deploy do Render depois do CI verde) não muda.
- Custo de infraestrutura baixo nos cenários baixo e moderado (US$ 7 e US$ 50 por mês).
- A separação por pacotes e a regra de dependência deixam a extração de um contexto para serviço possível sem reescrever o domínio. Isso vale primeiro para a Telemetria.

**Negativas e riscos aceitos**

- Os contextos escalam juntos: um pico em um deles exige instância maior para todos, exceto a Telemetria no cenário alto.
- O monolito só continua modular se houver disciplina. Sem verificação automática, um `import` errado desfaz a fronteira. Mitigação: estender o `ControllerSemRepositorioTest` para varrer todo o domínio e proibir `org.springframework`, `jakarta.persistence` e dependências entre contextos que não passem pela camada de aplicação.
- Uma falha no processo derruba todos os contextos ao mesmo tempo. No cenário alto, a API roda com no mínimo duas instâncias.
- A Telemetria com estado em memória só tem sincronização correta com uma instância. Se precisar de mais, o agregador passa para armazenamento compartilhado (tabela de leituras no PostgreSQL ou Redis), e a sincronização passa a ser feita pelo banco.

## Alternativas consideradas

| Alternativa | Por que não |
|---|---|
| Microsserviços para os seis contextos | Custo de operação e de consistência eventual sem carga que justifique (D.5). Piso de custo 5 vezes maior no cenário baixo. |
| Manter a organização por tipo técnico | Não deixa clara a fronteira entre contextos e não permite verificar a regra de dependência por pacote. |
