# TDD — Estratégia e matriz de testes

**Especificação:** [`SDD.md`](SDD.md) · **Regras:** [`regras-de-negocio.md`](regras-de-negocio.md) · **BDD:** [`testes-bdd.md`](testes-bdd.md)

## 1. Ciclo

1. A mudança nasce de um requisito do SDD (`RF-xx`) ou de uma regra (`RN-xx`).
2. Escreve o teste da regra em `domain/regras` (unitário, sem Spring), roda e vê **vermelho**.
3. Implementa o mínimo na regra até ficar **verde**.
4. Teste do service com mocks (a regra está sendo aplicada?), depois o teste de integração HTTP (status, JSON, perfil).
5. Refatora com a suíte verde. O PR só entra com o check `build-and-test` verde.

## 2. Pirâmide

| Camada | Ferramenta | Onde | Roda em |
|---|---|---|---|
| Regras de domínio | JUnit 5 (+ `@ParameterizedTest`) | `src/test/java/com/hemorede/domain/regras` | `mvn test` |
| Algoritmos (AED) | JUnit 5 | `algoritmos/`, `estruturas/` | `mvn test` |
| Services | JUnit 5 + Mockito | `service/` | `mvn test` |
| API + segurança | `@SpringBootTest` + MockMvc + H2, JWT real | `controller/`, `seguranca/` | `mvn test` |
| Aceitação | Cucumber (Gherkin em pt) | `bdd/`, `resources/features` | `mvn test` |
| Banco real | Testcontainers PostgreSQL (`*IT`) | `integration/` | `mvn verify` (precisa de Docker) |

## 3. Matriz regra → testes

| Regra / requisito | Casos cobertos |
|---|---|
| RN-02 FEFO | validade manda; empate prefere tipo idêntico; empate do mesmo tipo prefere coleta mais antiga; coleta nula; auditoria detecta violação; saída real de 3 de 6 bolsas respeita FEFO; fluxo completo deixa no estoque a de validade mais longa |
| RN-03 Validade | vence no fim do dia; validade padrão por componente; dias para vencer negativos; coleta futura; validade antes da coleta; plaquetas acima de 5 dias |
| RN-04 Elegibilidade | sem histórico apto; 70 dias: homem apto, mulher não (faltam 20); sexo ausente usa 60; 15 e 70 anos; 69 anos apto; 49,9 kg; inativo; 3 doações/ano para mulher; acúmulo de 5 motivos; tipo da bolsa diferente do cadastro |
| RN-06 Refrigeração | veículo da mesma faixa; plasma + crio no mesmo veículo; hemácias + plasma não |
| RN-09 Estados | 9 transições permitidas, 8 recusadas, nulos, estados finais; ENTREGUE não volta a APROVADA; despachar antes de alocar = 409 |
| RN-10 Progresso | fração do tempo (antes, durante, depois, duração zero); início, fim de trecho, meio do 2º trecho, fim; trajeto de um ponto; entradas inválidas |
| RF-01 Login | senha errada e e-mail inexistente com a mesma mensagem 401; token adulterado 401; sem token 401; health público |
| RF-02/03 Doador | cadastro 201 com perfil DOADOR; e-mail repetido 409; corpo inválido 400 com campos; painel com CPF mascarado; doador recebe 403 na API do hemocentro e do hospital |
| RF-06/07 Hospital | hospitalId do corpo é ignorado (vem do token); outro hospital recebe 404 em abrir, rastrear e confirmar; hospital recebe 403 no despacho |
| RF-10/11/12 Entrega | alocar → despachar (veículo EM_ROTA, bolsas EM_TRANSITO) → rastrear (trajeto começa em N0 e termina no nó do hospital, % entre 0 e 100, previsão) → confirmar (ENTREGUE, veículo liberado, 100%) |
| RF-15 Contas | hemocentro cria conta HOSPITAL; não cria DOADOR (400); listagem nunca expõe hash |

## 4. O que foi refeito nesta revisão

- Os testes de integração existentes passaram a rodar autenticados como HEMOCENTRO (`@WithMockUser`); os passos do Cucumber usam `.with(user(...))`.
- `MetricasRedeControllerIntegrationTest` usa `pollInSameThread()`: o Awaitility rodava a checagem em outra thread, sem o usuário de teste.
- Novos: 39 testes de regra, 10 de service (`RegrasAplicadasNosServicosTest`) e 7 de integração ponta a ponta com JWT (`PerfisEFluxoEntregaIntegrationTest`).

**Total:** 456 testes em `mvn test`, todos verdes. Os `*IT` com PostgreSQL rodam no CI com `mvn verify`.

## 5. Como rodar

```bash
cd rotavital
mvn test                          # unitários, integração H2, segurança e BDD
mvn test -Dtest='Regra*Test'      # só as regras de domínio
mvn verify                        # inclui os *IT com PostgreSQL (precisa de Docker)
```
