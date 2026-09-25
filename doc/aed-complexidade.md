# 📊 Análise de Complexidade Assintótica — Algoritmos e Estruturas de Dados (AED)

**Projeto:** Rota Vital — Sistema de Roteirização e Gestão Logística de Hemocomponentes  
**Módulo:** Algoritmos de Distribuição e Roteirização  
**Autor:** Matheus Rodrigues Larré  
**Data:** 25/09/2026  

---

> [!NOTE]
> **Nota de Contexto e Escopo Pedagógico:**  
> Este documento consolida a análise teórica de complexidade assintótica (Notação Big-O) e a justificativa de engenharia de software para as estruturas avançadas de alocação e roteirização do sistema (`FilaFEFO`, `IndiceEstoque` e `GrafoRotas`/Dijkstra).  
> Tais estruturas representam a evolução algorítmica planejada para o sistema frente às estruturas lineares fundamentais (Listas, Filas FIFO e Pilhas). Este estudo estabelece o embasamento teórico para os módulos da aplicação e orienta a integração contínua com a camada de domínio POO (Spring Boot).

---

## 1. Visão Geral das Estruturas e Operações

O ecossistema algorítmico do **Rota Vital** foi projetado para responder a dois desafios centrais de missão crítica na saúde pública:
1. **Minimização do desperdício de hemocomponentes:** Garantir que bolsas com data de validade mais próxima sejam alocadas prioritariamente (política FEFO — *First-Expired, First-Out*).
2. **Tempo de resposta logístico mínimo:** Determinar o trajeto mais rápido/curto entre o Hemocentro e os hospitais da rede sob restrições estritas da cadeia de frio.

Abaixo, detalha-se o comportamento assintótico de cada estrutura implementada.

---

## 2. Análise Detalhada por Estrutura

### 2.1. `FilaFEFO` (`java.util.PriorityQueue` / Min-Heap Binário)

A classe [`FilaFEFO`](../rotavital/src/main/java/com/hemorede/algoritmos/FilaFEFO.java) implementa a política *First-Expired, First-Out*, ordenando as bolsas de sangue por data de validade crescente (com desempate pela data de coleta mais antiga). A estrutura interna utilizada é a `PriorityQueue` do Java, baseada em uma árvore binária quase completa implementada sobre array (Min-Heap binário).

#### Tabela de Operações:

| Operação | Método | Complexidade Temporal (Pior Caso) | Complexidade Temporal (Caso Médio) | Complexidade Espacial |
| :--- | :--- | :---: | :---: | :---: |
| **Inserção** | `inserir(Bolsa bolsa)` | $O(\log n)$ | $O(\log n)$ | $O(1)$ amortizado |
| **Remoção do Mínimo (Alocação)** | `alocar()` | $O(\log n)$ | $O(\log n)$ | $O(1)$ |
| **Consulta ao Topo** | `consultarProxima()` | $O(1)$ | $O(1)$ | $O(1)$ |
| **Verificação de Tamanho** | `totalDisponivel()` | $O(1)$ | $O(1)$ | $O(1)$ |
| **Verificação de Vazio** | `estaVazia()` | $O(1)$ | $O(1)$ | $O(1)$ |

#### Justificativa Teórica:
* **Inserção ($O(\log n)$):** O novo elemento é inserido na última posição livre do heap e sobe na hierarquia (*sift-up*) comparando sua validade com o nó pai. A altura máxima da árvore binária para $n$ bolsas é $\lfloor \log_2 n \rfloor$.
* **Remoção do Mínimo ($O(\log n)$):** O elemento do topo (raiz do heap, que possui a menor data de validade) é extraído em $O(1)$. Em seguida, a última folha é promovida à raiz e descende na hierarquia (*sift-down*) recompondo a propriedade de min-heap, operação limitada à altura da árvore ($O(\log n)$).
* **Consulta ao Topo ($O(1)$):** Acessa diretamente o índice zero do array subjacente (`fila.peek()`), sem alterar o estado do heap.

---

### 2.2. `IndiceEstoque` (`java.util.HashMap` / `EnumMap` por Tipo Sanguíneo)

A classe [`IndiceEstoque`](../rotavital/src/main/java/com/hemorede/algoritmos/IndiceEstoque.java) funciona como um catálogo indexado onde a chave de acesso é o tipo sanguíneo ([`TipoSanguineo`](../rotavital/src/main/java/com/hemorede/domain/enums/TipoSanguineo.java) — 8 valores fixos) e o valor associado é uma instância exclusiva de [`FilaFEFO`](../rotavital/src/main/java/com/hemorede/algoritmos/FilaFEFO.java).

#### Tabela de Operações:

| Operação | Método | Complexidade Temporal (Caso Médio) | Complexidade Temporal (Pior Caso) | Complexidade Espacial |
| :--- | :--- | :---: | :---: | :---: |
| **Busca de Fila por Tipo** | `consultarEstoque(tipo)` | $O(1)$ | $O(k)$* | $O(1)$ |
| **Inserção de Bolsa** | `adicionarBolsa(bolsa)` | $O(\log n)$ | $O(\log n)$ | $O(1)$ |
| **Alocação por Tipo** | `alocarBolsa(tipo)` | $O(\log n)$ | $O(\log n)$ | $O(1)$ |
| **Consulta de Saldo por Tipo** | `totalDisponivelPorTipo(tipo)` | $O(1)$ | $O(1)$ | $O(1)$ |
| **Contagem Geral de Estoque** | `totalGeralDisponivel()` | $O(1)$ ($k$ fixo = 8) | $O(1)$ | $O(1)$ |

*\* Nota: Como a chave é um `enum` com domínio finito e estático de 8 elementos (`A+`, `A-`, `B+`, `B-`, `AB+`, `AB-`, `O+`, `O-`), o custo de indexação em `EnumMap` baseia-se no ordinal inteiro do enum, eliminando colisões de hash e assegurando $O(1)$ determinístico no pior caso.*

#### Justificativa Teórica:
* **Busca Direta em $O(1)$:** Em vez de percorrer um repositório global de bolsas para filtrar por tipo, o acesso ao estoque do tipo solicitado ocorre em tempo constante através da função hash / deslocamento em tabela.
* **Composição de Complexidade:** A operação completa de adicionar ou retirar uma bolsa tem custo $T = T_{\text{hash}} + T_{\text{heap}} = O(1) + O(\log n) = O(\log n)$, onde $n$ é a quantidade de bolsas disponíveis daquele tipo sanguíneo específico (particionando a carga total do banco de sangue entre 8 heaps menores).

---

### 2.3. Algoritmo de Dijkstra com Heap Binário (`GrafoRotas`)

A classe [`GrafoRotas`](../rotavital/src/main/java/com/hemorede/algoritmos/GrafoRotas.java) implementa o cálculo do menor caminho entre qualquer par de nós (origem e destino) da rede hospitalar.

#### Especificação Assintótica:
* **Complexidade Temporal:** $O((V + E) \log V)$
* **Complexidade Espacial:** $O(V + E)$

#### Demonstração Matemática da Complexidade:
1. **Estrutura de Dados:**
   - Lista de Adjacência implementada como `Map<String, Map<String, Double>>`, ocupando $O(V + E)$ em memória.
   - Fila de Prioridade (Min-Heap) de nós a visitar (`PriorityQueue<NoDistancia>`), ordenada pela distância acumulada provisória.
2. **Execução:**
   - **Extração de Vértices:** Cada vértice $u \in V$ é extraído do heap no máximo uma vez com custo $O(\log V)$. Para todos os vértices, o somatório é $V \cdot O(\log V) = O(V \log V)$.
   - **Relaxamento de Arestas:** Para cada vértice processado, todas as suas arestas incidentes são inspecionadas. Ao longo de todo o algoritmo, cada aresta é percorrida uma única vez (ou duas no caso bidirecional). Quando uma distância menor é encontrada, o novo par `(vizinho, novaDistancia)` é inserido na fila de prioridade, operação com custo $O(\log V)$. Para o conjunto de arestas $E$, o custo acumulado é $E \cdot O(\log V) = O(E \log V)$.
   - **Custo Total Acumulado:**  
     $$T(V, E) = O(V \log V + E \log V) = O((V + E) \log V)$$

#### Parametrização Real com [`GrafoRotasConfig`](../rotavital/src/main/java/com/hemorede/config/GrafoRotasConfig.java):
Na configuração padrão de dados sintéticos da aplicação ([`DadosSinteticos.criarGrafoRotaVital()`](../rotavital/src/main/java/com/hemorede/algoritmos/DadosSinteticos.java)):
* **Vértices ($V = 6$):**
  1. `N0`: Hemocentro Central
  2. `N1`: Hospital Norte
  3. `N2`: Hospital Sul
  4. `N3`: Hospital Leste
  5. `N4`: Hospital Oeste
  6. `N5`: Hospital Central II
* **Arestas ($E = 15$ arestas bidirecionais / 30 arcos direcionados):**
  - O grafo é completo ($K_6$), onde cada nó se conecta diretamente aos outros 5 nós:
    $$E = \frac{V(V - 1)}{2} = \frac{6 \times 5}{2} = 15 \text{ arestas}$$
* **Cálculo com os Valores do Projeto:**
  - $V = 6$, $E = 15$
  - Número de operações de chave na fila: $(V + E) = 6 + 15 = 21$
  - Multiplicador logarítmico: $\log_2(6) \approx 2.585$
  - Total estimado de operações de relaxamento/visita: $\approx 21 \times 2.585 \approx 54 \text{ operações elementares}$.
  - Isso garante tempo de resposta da rota em frações de milissegundo no Spring Boot, atendendo com folga à janela crítica da cadeia de frio.

---

## 3. Justificativa de Escolha das Estruturas frente a Alternativas

A escolha de cada estrutura foi norteada pelo compromisso entre **tempo de resposta em tempo real**, **uso racional de memória** e **prevenção de perdas biológicas**.

### 3.1. `FilaFEFO` (Min-Heap) vs. Alternativas Lineares

| Critério | Lista Encadeada Não-Ordenada | Lista Ordenada (Array / ArrayList) | Min-Heap (`PriorityQueue`) [ESCOLHIDA] |
| :--- | :---: | :---: | :---: |
| **Inserção (`add`)** | $O(1)$ | $O(n)$ (busca + *shift* de elementos) | **$O(\log n)$** |
| **Alocação (`poll`)** | $O(n)$ (busca linear pelo menor) | $O(1)$ (remove do início) | **$O(\log n)$** |
| **Consulta Topo (`peek`)**| $O(n)$ | $O(1)$ | **$O(1)$** |
| **Impacto no Sistema** | Péssimo para alto volume de saídas | Inserção lenta e custosa em doações em lote | **Equilíbrio assintótico ótimo** |

* **Por que não a Lista Não-Ordenada?** Embora a inserção de doações seja rápida ($O(1)$), cada pedido de emergência médica exigiria percorrer todas as bolsas do estoque ($O(n)$) para encontrar a mais antiga/próxima do vencimento. Em cenários de UTI e cirurgias de urgência, a busca linear aumenta perigosamente a latência de despacho.
* **Por que não a Lista Ordenada?** Manter uma lista ordenada em memória requer inserção por busca binária ($O(\log n)$) seguida de deslocamento físico em memória dos elementos adjacentes (*array shift* — $O(n)$). Em campanhas de doação com entrada de centenas de bolsas por hora, a lista ordenada gera sobrecarga de processamento desnecessária.
* **Conclusão:** O **Min-Heap binário** oferece o equilíbrio assintótico perfeito: tanto a entrada quanto a saída são garantidas em $O(\log n)$, com a consulta imediata da próxima bolsa em $O(1)$.

---

### 3.2. `IndiceEstoque` (Tabela Hash) vs. Busca Linear em Lista Única

| Critério | Lista Global Não-Indexada (`List<Bolsa>`) | Índice Hash por Tipo (`Map<Tipo, FilaFEFO>`) [ESCOLHIDO] |
| :--- | :---: | :---: |
| **Triagem por Tipo Sanguíneo** | $O(n)$ (percorre todo o inventário) | **$O(1)$ (acesso direto ao bucket)** |
| **Consumo de Memória** | Mínimo (apenas os nós da lista) | Mínimo adicional (apenas 8 buckets estáticos) |
| **Isolamento de Concorrência** | Bloqueia o estoque inteiro durante alocação | Permite particionamento de leitura/escrita por tipo |

* **Por que não a Busca Linear?** Se mantivéssemos uma única lista global contendo todas as bolsas do Hemocentro, toda requisição de um hospital para um tipo sanguíneo específico (ex: `O-`) exigiria inspecionar linearmente milhares de bolsas de tipos não compatíveis (`A+`, `B+`, etc.) antes de filtrar as candidatas válidas.
* **Conclusão:** A **Tabela Hash** particiona o estoque em 8 sub-heaps independentes. A busca pelo grupo correto é imediata ($O(1)$), permitindo que a alocação subsequente atue apenas sobre as bolsas estritamente relevantes.

---

### 3.3. Dijkstra com Min-Heap vs. Dijkstra Ingênuo com Matriz de Adjacência

| Critério | Dijkstra Ingênuo (Matriz $O(V^2)$) | Dijkstra com Min-Heap ($O((V + E) \log V)$) [ESCOLHIDO] |
| :--- | :---: | :---: |
| **Estrutura do Grafo** | Matriz de Adjacência $V \times V$ | Lista de Adjacência baseada em Map |
| **Busca do Próximo Menor** | Varredura linear em array de distâncias: $O(V)$ | Extração da raiz do Min-Heap: $O(\log V)$ |
| **Desempenho com Malha Expansível** | Degrada quadraticamente conforme novos postos são adicionados | Escala com suavidade quasi-linear para redes reais |
| **Consumo de Memória** | $O(V^2)$ fixo | $O(V + E)$ proporcional apenas às vias existentes |

* **Por que o Min-Heap foi escolhido?**  
  Em grafos esparsos ou semi-esparsos (que representam fielmente redes viárias urbanas e regionais onde cada hospital se conecta fisicamente a poucas vias principais, e não a todos os outros pontos), $E \ll V^2$. Nessas condições, $O((V + E) \log V)$ é assintoticamente muito superior a $O(V^2)$. Mesmo no protótipo atual de 6 nós ($K_6$), a solução baseada em heap já entrega portabilidade e prontidão arquitetural para a futura expansão geográfica da malha da Hemorrede sem necessidade de refatoração algorítmica.

---

## 4. Tabela Resumo Consolidada

| Componente | Estrutura de Dados Subjacente | Operação Crítica | Complexidade Temporal | Complexidade Espacial |
| :--- | :--- | :--- | :---: | :---: |
| **`FilaFEFO`** | Min-Heap Binário (`PriorityQueue`) | Inserção de bolsa | $O(\log n)$ | $O(n)$ |
| **`FilaFEFO`** | Min-Heap Binário (`PriorityQueue`) | Remoção da bolsa mais antiga | $O(\log n)$ | $O(n)$ |
| **`FilaFEFO`** | Min-Heap Binário (`PriorityQueue`) | Consulta do próximo vencimento | $O(1)$ | $O(n)$ |
| **`IndiceEstoque`** | Tabela Hash (`EnumMap` / `HashMap`) | Localização do estoque por tipo | $O(1)$ | $O(k + n)$ ($k=8$) |
| **`IndiceEstoque`** | Hash + Heap Integrado | Entrada/Saída completa de bolsa | $O(\log n)$ | $O(n)$ |
| **`GrafoRotas`** | Lista de Adjacência + Min-Heap | Menor caminho (Dijkstra) | $O((V + E) \log V)$ | $O(V + E)$ |

---

## 5. Rastreabilidade com o Código-Fonte do Projeto

* **Estrutura de Fila de Prioridade:** [`FilaFEFO.java`](../rotavital/src/main/java/com/hemorede/algoritmos/FilaFEFO.java)
* **Estrutura de Índice Hash:** [`IndiceEstoque.java`](../rotavital/src/main/java/com/hemorede/algoritmos/IndiceEstoque.java)
* **Estrutura de Grafo e Algoritmo de Dijkstra:** [`GrafoRotas.java`](../rotavital/src/main/java/com/hemorede/algoritmos/GrafoRotas.java)
* **Definição dos Nós e Arestas do Grafo ($V=6, E=15$):** [`DadosSinteticos.java`](../rotavital/src/main/java/com/hemorede/algoritmos/DadosSinteticos.java) e [`GrafoRotasConfig.java`](../rotavital/src/main/java/com/hemorede/config/GrafoRotasConfig.java)
* **Suíte de Testes Automatizados:** [`AEDTesteManualTest.java`](../rotavital/src/test/java/com/hemorede/algoritmos/AEDTesteManualTest.java)
