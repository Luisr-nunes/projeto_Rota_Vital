# Regras de negócio do domínio

Todas as regras ficam em `rotavital/src/main/java/com/hemorede/domain/regras/`, em **Java puro** (sem Spring, sem JPA, só `java.time` e os enums do domínio). Para usar em outra parte (outro serviço, job, app), basta copiar o pacote `domain.regras` com `domain.enums`.

Os services não reimplementam regra: buscam os dados, chamam a classe abaixo e gravam o resultado.

| ID | Regra | Classe | Usada em | Testes |
|---|---|---|---|---|
| RN-01 | Compatibilidade ABO/Rh de hemácias (receptor ← doadores) | `TipoSanguineo.compativel` / `doadoresCompativeis` | `AlocacaoService`, `CompatibilidadeSanguineaService` | `MatrizCompatibilidadeTest`, BDD `alocar_bolsa.feature` |
| RN-02 | FEFO: sai primeiro a bolsa que vence primeiro; empate prefere o tipo idêntico ao pedido (preserva O-) e depois a coleta mais antiga; a saída é auditada (nenhuma bolsa que fica vence antes das que saíram) | `RegraFEFO.comparar` / `respeitaFEFO` | `AlocacaoService.alocar` | `RegraFEFOTest`, `RegrasAplicadasNosServicosTest.Fefo` |
| RN-03 | Validade: bolsa vencida nunca sai; coleta não pode ser futura; validade depois da coleta e dentro do prazo do componente (hemácias 42 d, plaquetas 5 d, plasma e crio 365 d) | `RegraValidadeBolsa` | `DoacaoService`, `ValidadeService`, `AlocacaoService` | `RegraValidadeBolsaTest` |
| RN-04 | Elegibilidade do doador: ativo; 16 a 69 anos; 50 kg ou mais; intervalo de 60 dias (masculino) ou 90 dias (feminino); no máximo 4 (masc.) ou 3 (fem.) doações em 12 meses; tipo da bolsa igual ao tipo cadastrado | `RegraElegibilidadeDoador` | `DoacaoService`, `PortalDoadorService` | `RegraElegibilidadeDoadorTest`, `DoacaoServiceTest` |
| RN-05 | Requisição urgente tem prioridade na fila de roteirização | `RequisicaoRepository` (ordenação) | `RoteirizacaoService` | `RequisicaoControllerIntegrationTest` |
| RN-06 | Cadeia fria: veículo só leva componente da mesma faixa de refrigeração; requisição que mistura faixas não sai num despacho único | `RegraRefrigeracao` | `RastreamentoService.despachar`, `RoteirizacaoService` | `RegraRefrigeracaoTest` |
| RN-07 | Estoque mínimo de segurança por tipo/componente | `EstoqueService.estoqueAbaixoDoMinimo` | painéis | `EstoqueControllerIntegrationTest` |
| RN-08 | Alocação tudo ou nada: se um item não fecha, nenhuma bolsa é reservada (rollback) | transação em `RequisicaoService.aprovar` / `AlocacaoService` | — | `RequisicaoFluxoIntegrationTest` |
| RN-09 | Máquina de estados da requisição (ver SDD, seção 5) | `RegraTransicaoRequisicao` | `RequisicaoService`, `AlocacaoService`, `RastreamentoService` | `RegraTransicaoRequisicaoTest`, `RegrasAplicadasNosServicosTest.Aprovar` |
| RN-10 | Posição estimada da entrega: interpolação ao longo do trajeto, proporcional à distância de cada trecho e ao tempo decorrido | `RegraProgressoEntrega` | `RastreamentoService` | `RegraProgressoEntregaTest` |

## Exemplo de reuso fora do Spring

```java
var r = RegraElegibilidadeDoador.avaliar(true, Sexo.FEMININO, LocalDate.of(1995, 4, 10), 62.0,
        LocalDate.now().minusDays(70), 1, LocalDate.now());
r.apto();                 // false
r.motivos();              // ["intervalo mínimo de 90 dias desde a última doação; faltam 20 dia(s)"]
r.proximaDataPossivel();  // hoje + 20 dias
```

## O que mudou nesta revisão

- `aprovar` recusava nada: uma requisição ENTREGUE voltava a APROVADA. Agora só PENDENTE pode ser aprovada (409).
- O intervalo de doação era único (60 dias). Agora depende do sexo (60/90).
- A doação aceitava bolsa de tipo diferente do doador e validade acima do prazo do componente. Agora recusa (422 e 400).
- FEFO desempatava só pela ordem de leitura. Agora prefere o tipo idêntico, para não gastar O- à toa.
