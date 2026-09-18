# Modelo de Domínio — Rota Vital

**Task:** PI2-18 — W06-CRUD (POO)
**Responsável:** Luis Nunes
**Disciplina:** Programação Orientada a Objetos (POO)
**Sprint:** W06e07

Este documento descreve as entidades JPA do módulo de domínio, seus atributos, relacionamentos e as regras de negócio aplicadas pelos services. Serve de referência para as demais disciplinas (AED, RSD, SO, EST) que consomem esse modelo.

---

## 1. Entidades e Atributos

| Entidade | Atributos | Observações |
|---|---|---|
| **Doador** | id, nome, cpf (único), tipoSanguineo, dataUltimaDoacao | 1:N com `Bolsa` |
| **Bolsa** | id, hemoComponente, tipoSanguineo, dataColeta, dataValidade, status | N:1 com `Doador` e `Estoque`; N:N com `ItemRequisicao` |
| **Hospital** | id, nome, cnpj (único), endereco, latitude, longitude, codigoNo | `codigoNo` referencia o nó do hospital no grafo de rotas (`GrafoRotas`) |
| **Estoque** | id, hospital, capacidadeMaxima | N:1 com `Hospital`; 1:N com `Bolsa` |
| **Requisicao** | id, hospitalSolicitante, dataSolicitacao, prioridade, status, rota | N:1 com `Hospital` e `Rota`; 1:N com `ItemRequisicao` |
| **ItemRequisicao** | id, requisicao, hemoComponente, tipoSanguineo, quantidade, bolsasAlocadas | N:1 com `Requisicao`; N:N com `Bolsa` |
| **Veiculo** | id, placa (única), tipoRefrigeracao, capacidadeCarga, status | 1:N com `Rota` |
| **Motorista** | id, nome, cnh (única), telefone | 1:N com `Rota` |
| **Rota** | id, veiculo, motorista, requisicoes, dataSaida, dataChegadaPrevista, status | N:1 com `Veiculo` e `Motorista`; 1:N com `Requisicao` |

### Enums

`StatusBolsa` (DISPONIVEL, RESERVADA, EM_TRANSITO, UTILIZADA, DESCARTADA), `StatusRequisicao` (PENDENTE, APROVADA, EM_ROTA, ENTREGUE, CANCELADA), `StatusRota`, `StatusVeiculo`, `Prioridade`, `HemoComponente`, `TipoSanguineo`, `TipoRefrigeracao`.

---

## 2. Relacionamentos

```text
Doador 1───N Bolsa N───1 Estoque N───1 Hospital 1───N Requisicao
                 │                                        │
                 N                                        1
                 │                                        │
          ItemRequisicao N───1 Requisicao N───1 Rota N───1 Veiculo
                                                    │
                                                    1
                                                    │
                                                    N
                                               Motorista
```

`Bolsa` se relaciona com `ItemRequisicao` via `item_requisicao_bolsa` (tabela associativa), representando as bolsas efetivamente alocadas a cada item da requisição.

---

## 3. Regras de Negócio Aplicadas no Domínio

| # | Regra | Onde é aplicada |
|---|---|---|
| 1 | Compatibilidade ABO/Rh entre bolsa e item da requisição | `AlocacaoService`, `TipoSanguineo.compativel` |
| 2 | Alocação por FEFO (validade mais próxima primeiro) | `AlocacaoService`, `BolsaRepository` (query ordenada) |
| 3 | Bolsa vencida nunca é alocada | `Bolsa.isVencida()`, `AlocacaoService`, `ValidadeService` |
| 4 | Intervalo mínimo entre doações do mesmo doador | `DoacaoService` |
| 5 | Requisições urgentes têm prioridade na roteirização | `RoteirizacaoService.proximasParaRoteirizar` |
| 6 | Veículo só atende rota com refrigeração compatível | `RoteirizacaoService.selecionarVeiculoCompativel` |
| 7 | Estoque mínimo de segurança por tipo/componente | `EstoqueService.estoqueAbaixoDoMinimo` |
| 8 | Requisição só é aprovada se todos os itens forem 100% alocados (rollback caso contrário) | `RequisicaoService.aprovar` (`@Transactional`) |

---

## Fontes

- [`doc/escopo-grafo.md`](escopo-grafo.md) — grafo de rotas referenciado por `Hospital.codigoNo`
- [`doc/contratos-api.md`](contratos-api.md) — endpoints que expõem essas entidades
- `README.md` do módulo `rotavital` — convenções gerais do projeto
