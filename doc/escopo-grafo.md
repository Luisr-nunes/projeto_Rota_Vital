# Escopo do Grafo — Rota Vital

**Task:** PI2-13 — Escopo do Grafo
**Responsáveis:** Matheus Larré + Luis Lucena
**Disciplina:** Algoritmos e Estruturas de Dados (AED)
**Sprint:** U1 — Estruturas-base (grafo de rotas, estoque e FEFO)

Este documento especifica a inteligência algorítmica do módulo de
roteirização e alocação de estoque do sistema Rota Vital, conforme os
critérios técnicos mínimos da disciplina: caminho mínimo correto, hash
de estoque funcional e FEFO priorizando por validade — todos com dados
sintéticos, prontos para integração com o módulo de POO (Spring Boot).

---

## 1. Lista de nós

O grafo representa a malha de transporte da rede de sangue: o
Hemocentro (ponto central de processamento/estoque) e 5 hospitais
atendidos. As coordenadas são sintéticas (não correspondem a locais
reais), usadas apenas para calcular distâncias euclidianas simuladas.

| ID | Nó                | Tipo        | Coordenada (x, y) |
|----|-------------------|-------------|--------------------|
| N0 | Hemocentro Central | Hemocentro  | (0, 0)             |
| N1 | Hospital Norte      | Hospital    | (10, 15)           |
| N2 | Hospital Sul        | Hospital    | (-8, -12)          |
| N3 | Hospital Leste      | Hospital    | (18, -5)           |
| N4 | Hospital Oeste      | Hospital    | (-15, 6)           |
| N5 | Hospital Central II | Hospital    | (4, -9)            |

**Observações de escopo:**
- Número de nós fixado em 6 (1 Hemocentro + 5 hospitais), conforme
  limite definido pela coordenação do PI para manter o grafo controlado.
- Cada nó terá um identificador único (`id`), nome de exibição e
  coordenadas sintéticas, representados como uma classe/record simples
  (`NoRota`) reutilizável entre os módulos de AED e POO.
- Não há adição dinâmica de nós nesta fase — o grafo é estático e
  conhecido em tempo de compilação/carga.

---

## 2. Matriz de distâncias

Matriz de adjacência simétrica com o custo (em km simulados) de
deslocamento entre cada par de nós. Valores inventados para fins
didáticos, coerentes com as coordenadas sintéticas acima (não
necessariamente a distância euclidiana exata, para permitir simular
rotas mais realistas com obstáculos/vias).

|        | N0 (Hemo) | N1 (Norte) | N2 (Sul) | N3 (Leste) | N4 (Oeste) | N5 (Central II) |
|--------|-----------|------------|----------|------------|------------|------------------|
| **N0** | 0         | 18         | 15       | 20         | 17         | 10               |
| **N1** | 18        | 0          | 30       | 25         | 22         | 20               |
| **N2** | 15        | 30         | 0        | 28         | 12         | 8                |
| **N3** | 20        | 25         | 28       | 0          | 35         | 14               |
| **N4** | 17        | 22         | 12       | 35         | 0          | 19               |
| **N5** | 10        | 20         | 8        | 14         | 19         | 0                |

**Observações de escopo:**
- Grafo não-direcionado e totalmente conectado (todo par de nós tem
  uma aresta direta) — simplificação didática que ainda permite
  demonstrar que o Dijkstra encontra a rota de menor custo mesmo
  quando existem atalhos indiretos mais baratos (ex.: N0 → N5 → N2
  pode custar menos que N0 → N2 direto, dependendo dos pesos ajustados
  na implementação).
- A matriz será representada como `double[][]` ou `Map<String, Map<String, Double>>`,
  a decidir na implementação, carregada a partir de dados sintéticos
  (hardcoded ou de um arquivo de configuração/JSON de teste).
- Peso da aresta = custo de deslocamento (proxy de tempo/distância);
  não modela ainda janelas de tempo ou cadeia fria — isso é tratado em
  unidades futuras (RSD/SO), citado apenas como próximo passo.

---

## 3. Esboço do Dijkstra

Algoritmo de caminho mínimo para calcular a rota de menor custo do
Hemocentro até qualquer hospital (ou entre hospitais, se necessário).

### Estrutura de dados
- **Fila de prioridade (min-heap)** ordenada pela menor distância
  acumulada conhecida até o momento.
- **Mapa de distâncias** (`distancias[nó] = custo mínimo conhecido`),
  inicializado com `infinito` para todos os nós exceto a origem (`0`).
- **Mapa de predecessores** (`anterior[nó] = nó de origem da aresta`),
  usado para reconstruir o caminho percorrido ao final.
- **Conjunto de visitados**, para evitar reprocessar nós já
  finalizados.

### Pseudocódigo

```
função dijkstra(grafo, origem):
    distancias[origem] = 0
    para cada nó v diferente de origem:
        distancias[v] = infinito

    filaPrioridade.inserir(origem, 0)

    enquanto filaPrioridade não estiver vazia:
        atual = filaPrioridade.removerMenor()
        se atual já foi visitado:
            continuar
        marcar atual como visitado

        para cada vizinho de atual:
            novoCusto = distancias[atual] + peso(atual, vizinho)
            se novoCusto < distancias[vizinho]:
                distancias[vizinho] = novoCusto
                anterior[vizinho] = atual
                filaPrioridade.inserir(vizinho, novoCusto)

    retornar distancias, anterior

função reconstruirCaminho(anterior, destino):
    caminho = []
    nó = destino
    enquanto nó existir:
        caminho.inserir(nó, no início)
        nó = anterior[nó]
    retornar caminho
```

### Complexidade esperada
- Com min-heap: `O((V + E) log V)`, onde `V = 6` nós e `E` até 15
  arestas (grafo completo não-direcionado) — trivial para o tamanho
  do escopo, mas implementado de forma genérica para eventual
  expansão controlada.

### Pontos de integração com POO
- Entrada: nó de origem (tipicamente o Hemocentro) e nó de destino
  (hospital que fez a requisição).
- Saída: custo total da rota + lista ordenada de nós do caminho, para
  exibição na tela de rotas da aplicação.
- Exposto como serviço/módulo (`RotaService.calcularMenorRota(origem, destino)`),
  consumido pelo controller de requisições hospitalares.

---

## 4. Estrutura FEFO (First-Expired, First-Out)

Fila de prioridade responsável por alocar bolsas de sangue priorizando
as que vencem primeiro, evitando descarte por validade.

### Estrutura de dados
- **Min-heap (fila de prioridade)** ordenada pela **data de validade**
  da bolsa (a mais próxima do vencimento fica no topo).
- Cada elemento da fila representa uma `Bolsa`, com atributos mínimos:
  `id`, `tipoSanguineo`, `dataColeta`, `dataValidade`, `status`.
- Uma fila FEFO por combinação de tipo sanguíneo + componente (ex.:
  concentrado de hemácias O+, plasma A-, etc.), para que a alocação
  já considere a compatibilidade antes de aplicar a priorização por
  validade.

### Operações principais

```
inserirBolsa(bolsa):
    fila = obterFilaPorTipo(bolsa.tipoSanguineo)
    fila.heap.inserir(bolsa, chave = bolsa.dataValidade)

alocarBolsa(tipoSanguineo):
    fila = obterFilaPorTipo(tipoSanguineo)
    se fila estiver vazia:
        retornar erro "sem estoque disponível"
    bolsa = fila.heap.removerMenor()   // menor data de validade
    bolsa.status = "alocada"
    retornar bolsa

verificarProximasAoVencimento(diasLimite):
    para cada fila:
        enquanto topo da fila vencer em <= diasLimite:
            reportar bolsa em risco de descarte
```

### Critério técnico
- A alocação **sempre** retorna a bolsa com menor `dataValidade`
  disponível e compatível — nunca uma bolsa mais recente enquanto
  houver uma mais antiga ainda válida na mesma categoria.
- Complexidade: `O(log n)` para inserção e remoção, `O(1)` para
  consulta ao topo (verificação de vencimento próximo).

### Pontos de integração com POO
- Exposta como serviço (`EstoqueService.alocarBolsa(tipoSanguineo)`),
  chamada quando uma requisição hospitalar é aprovada.
- Job/rotina periódica (a definir com SO) pode chamar
  `verificarProximasAoVencimento` para alimentar os painéis de EST.

---

## 5. Hash de estoque

Estrutura de consulta rápida ao estoque disponível, organizada por
tipo sanguíneo.

### Estrutura de dados

```
HashMap<TipoSanguineo, ListaDeBolsas>
```

- **Chave:** `TipoSanguineo` (enum com os 8 tipos ABO/Rh: O+, O-, A+,
  A-, B+, B-, AB+, AB-).
- **Valor:** `ListaDeBolsas`, que internamente pode ser a própria fila
  de prioridade FEFO (item 4) ou uma lista simples que referencia essa
  fila — a decidir na implementação, mas logicamente é "todas as
  bolsas daquele tipo, ordenáveis por validade".

### Operações principais

```
consultarEstoque(tipoSanguineo):
    retornar hashEstoque.obter(tipoSanguineo)   // O(1) amortizado

adicionarAoEstoque(bolsa):
    lista = hashEstoque.obter(bolsa.tipoSanguineo)
    lista.inserir(bolsa)                        // delega à fila FEFO

removerDoEstoque(bolsa):
    lista = hashEstoque.obter(bolsa.tipoSanguineo)
    lista.remover(bolsa)

totalDisponivelPorTipo(tipoSanguineo):
    retornar hashEstoque.obter(tipoSanguineo).tamanho()
```

### Justificativa técnica
- Consulta por tipo sanguíneo é a operação mais frequente do sistema
  (toda requisição hospitalar começa perguntando "há estoque do tipo
  X?"), por isso o acesso precisa ser `O(1)` amortizado — daí o uso de
  hash em vez de busca sequencial/linear.
- O hash resolve **"qual tipo tem estoque"**; a fila FEFO dentro de
  cada entrada resolve **"qual bolsa específica alocar"**. As duas
  estruturas trabalham juntas, não são alternativas.

### Pontos de integração com POO
- Serviço único de estoque (`EstoqueService`) concentra o `HashMap` e
  expõe métodos de consulta/alocação para os controllers REST.
- Estrutura pronta para alimentar os painéis de estoque/demanda da
  disciplina de Estatística (EST) via contrato de dados a combinar.

---

## Resumo dos critérios técnicos mínimos (checklist)

- [ ] Caminho mínimo correto (Dijkstra validado com casos de teste)
- [ ] Hash de estoque funcional (`HashMap<TipoSanguineo, ListaDeBolsas>`)
- [ ] FEFO priorizando corretamente por validade
- [ ] Código integrável ao app (serviços expostos para POO consumir)
- [ ] Grafo mantido no tamanho limitado (6 nós, dados sintéticos)

## Próximos passos

- Validar os pesos definitivos da matriz de distâncias.
- Escrever casos de teste unitários para o Dijkstra (pelo menos 3
  cenários: rota direta mais barata, rota indireta mais barata, nó
  isolado/sem estoque).
- Alinhar com POO o formato exato dos DTOs de saída (rota e bolsa
  alocada) para a integração via REST.
