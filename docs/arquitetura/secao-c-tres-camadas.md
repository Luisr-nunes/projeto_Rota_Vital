# Seção C — Arquitetura em três camadas

## C.1 As três camadas

| Camada | Responsabilidade | No Rota Vital |
|---|---|---|
| **Apresentação** | Recebe a requisição HTTP, valida o formato, converte DTO em comando e devolve resposta ou erro HTTP. Inclui as telas. | Controllers REST (`/api/v1/...`), DTOs, tradução de exceções para HTTP; telas HTML/JS em `resources/static`. |
| **Negócio: aplicação** | Casos de uso: orquestram o domínio, abrem a transação e publicam eventos. Sem regra própria. | `RegistrarDoacao`, `ReservarBolsas`, `AprovarRequisicao`, `CalcularRota`, `ConfirmarEntrega`. |
| **Negócio: domínio** | Agregados, entidades, objetos de valor, invariantes, eventos e as interfaces dos repositórios. Java puro. | `Estoque`, `Bolsa`, `Validade`, `Requisicao`, `Rota`, `FilaFEFO`, `GrafoRotas`. |
| **Dados** | Implementa os repositórios (JPA e PostgreSQL), converte linha do banco em objeto de domínio e cuida das integrações externas. | `BolsaRepositoryJpa`, `BolsaJpaEntity` e mapper, adaptador do simulador de telemetria. |

**Já mudou no código (17/11):** nenhum controller acessa repository direto (5 controllers corrigidos, com `ControllerSemRepositorioTest`); 4 testes saíram de `src/main` para `src/test` e o escopo do `spring-boot-starter-test` foi corrigido. **Falta para a Parte 3:** os pacotes ainda são por tipo técnico, os services misturam caso de uso e regra, e as entidades ainda têm `@Entity`.

## C.2 Regra de dependência

- A chamada só desce: apresentação → aplicação → domínio. A aplicação usa o banco pela interface do repositório.
- O domínio não importa `org.springframework`, `jakarta.persistence`, controller, DTO nem repositório JPA.
- Entre negócio e dados a dependência é **invertida**: `BolsaRepository` mora em `estoque.negocio.dominio` e `BolsaRepositoryJpa` (em `estoque.dados`) a implementa.
- A apresentação devolve DTO, nunca entidade, e não chama dados direto.
- Entre contextos, só pelo caso de uso público do outro ou por evento; nunca importar `dominio` ou `dados` de outro contexto (exceto o kernel).

**Como provar:** o teste `ControllerSemRepositorioTest` roda no `mvn test` da esteira e falha se um `@RestController` tiver campo do pacote de repositórios. No código anterior ele falha e lista os 5 controllers; no novo, passa. Na Parte 3 o mesmo teste cresce para o domínio inteiro.

## C.3 Estrutura de pacotes

```
com.hemorede
├── compartilhado/dominio   (TipoSanguineo, HemoComponente, TipoRefrigeracao)
├── captacao
│   ├── apresentacao        (DoadorController, DoacaoController)
│   ├── negocio
│   │   ├── aplicacao       (CadastrarDoador, RegistrarDoacao)
│   │   └── dominio         (Doador, IntervaloEntreDoacoes, BolsaColetada, DoadorRepository)
│   └── dados               (DoadorRepositoryJpa, DoadorJpaEntity)
├── estoque
│   ├── apresentacao        (EstoqueController)
│   ├── negocio
│   │   ├── aplicacao       (RegistrarBolsa, ReservarBolsas, ExpurgarVencidas)
│   │   └── dominio         (Estoque, Bolsa, Validade, StatusBolsa, FilaFEFO, BolsaRepository)
│   └── dados               (BolsaRepositoryJpa, BolsaJpaEntity)
├── requisicao
│   ├── apresentacao        (RequisicaoController)
│   ├── negocio             aplicacao (AprovarRequisicao) · dominio (Requisicao, ItemRequisicao, Prioridade)
│   └── dados               (RequisicaoRepositoryJpa, ReservaEstoqueAdapter -> estoque.ReservarBolsas)
├── expedicao
│   ├── apresentacao        (RotaController)
│   ├── negocio             aplicacao (CalcularRota, ConfirmarEntrega) · dominio (Rota, Veiculo, Carga, GrafoRotas)
│   └── dados               (RotaRepositoryJpa, TradutorBolsaParaCarga [ACL])
├── telemetria
│   ├── apresentacao        (TelemetriaLeituraController)
│   ├── negocio             aplicacao (RegistrarLeitura) · dominio (Leitura, FaixaSegura)
│   └── dados               (FilaLeiturasEmMemoria, AdaptadorSimulador [ACL])
├── indicadores
│   ├── apresentacao        (IndicadoresController)
│   ├── negocio             aplicacao (CalcularTaxaDescarte, PreverDemanda) · dominio (TaxaDescarte)
│   └── dados               (ConsultasIndicadoresJpa)
├── aed                     (estruturas e benchmarks da disciplina, fora dos contextos)
└── HemoredeApplication     (bootstrap e tratador global de erros)
```

As telas continuam em `resources/static`. Na Parte 3 não se migra tudo de uma vez: o Estoque (agregado da Seção B) vai primeiro e os outros contextos seguem a mesma forma.

## C.4 Camada lógica × tier físico

Camada é como o código se organiza; tier é onde ele roda. No Rota Vital as três camadas **não** correspondem a três tiers.

| Tier físico | Onde roda | Camadas lógicas | Ligação |
|---|---|---|---|
| Cliente | Navegador do usuário | Parte da apresentação: telas HTML/JS | HTTPS/443 até a borda do Render |
| Aplicação | Container Docker no Render (JRE 17, porta 8080 atrás da borda TLS) | Apresentação (controllers), negócio e dados (repositórios JPA, pool, fila e simulador em memória) | HTTP/8080, interno ao Render |
| Banco | PostgreSQL gerenciado no Supabase | Nenhuma camada de código: só guarda os dados | JDBC/TCP 5432 com TLS |

A camada de dados é código que roda no tier de aplicação, e a apresentação fica em dois tiers. Os seis contextos sobem no mesmo processo e no mesmo deploy (monolito modular), então separar por contexto e camada não muda a infraestrutura. Em desenvolvimento, o `docker compose` reproduz aplicação e banco (`postgres:16`); nos testes rápidos o H2 roda dentro do mesmo processo. A fila de telemetria vive na memória do container: se ele reinicia, as leituras pendentes se perdem (ponto levado à Seção D).

## C.5 O que muda na Parte 3 (entrega de 01/12) e como será testado

| Aspecto | Hoje | Parte 3 |
|---|---|---|
| Estrutura | Pacotes por tipo técnico | Pacotes por contexto e camada (C.3), Estoque primeiro |
| Modelo | Entidades com `@Entity` e pouco comportamento | Agregado `Estoque` com comportamento (`reservar`, B.5) em Java puro; JPA em `BolsaJpaEntity` ([ADR 0004](adr/0004-anotacoes-jpa-camada-dados.md), que complementa o [ADR 0002](adr/0002-mapeamento-jpa-nas-entidades.md)) |
| Regra de dependência | Teste só para controllers | Teste estendido ao domínio inteiro |
| TDD | 456 testes em `mvn test` (`doc/TDD.md`); cobertura em percentual ainda não medida | JaCoCo na esteira; meta proposta de 80% de linhas no domínio do Estoque; teste antes de cada regra migrada |
| BDD | `alocar_bolsa.feature` roda pela API | O mesmo cenário (B.4) roda também contra `Estoque.reservar` |
| SDD | `doc/SDD.md` | Especificação do agregado antes do código |
