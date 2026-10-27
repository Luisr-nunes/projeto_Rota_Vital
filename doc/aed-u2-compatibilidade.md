# Algoritmos e Estruturas de Dados U2 — Compatibilidade Sanguínea ABO/Rh, Complexidade do Matching e Fluxo Integrado (HU08)

**Disciplina:** Algoritmos e Estruturas de Dados (AED)
**Atividade:** PI2-167 · B4 (subtarefa da PI2-157 — HU08: Alocação por compatibilidade ABO/Rh + FEFO + rota)
**Responsável:** Matheus Rodrigues Larré
**Data:** 27/10/2026
**Relacionados:** PI2-165 (Matriz ABO/Rh 8×8), PI2-166 (Alocação devolve rota Dijkstra), PI2-145 (`AlocacaoService`), `doc/aed-complexidade.md` (Estruturas U1)

---

## 1. Objetivo e Contexto

Na Unidade 1 (`doc/aed-complexidade.md`), foram consolidadas as três estruturas fundamentais do módulo algorítmico do **Rota Vital**:
- **`FilaFEFO`** (`PriorityQueue` / Min-Heap binário ordenado pela data de validade mais próxima);
- **`IndiceEstoque`** (tabela de dispersão indexada por `TipoSanguineo`, particionando o estoque em $k = 8$ filas FEFO independentes);
- **`GrafoRotas`** (lista de adjacência + algoritmo de Dijkstra com Min-Heap para cálculo do caminho mínimo entre o Hemocentro e os hospitais).

Na **Unidade 2 (HU08 — PI2-157)**, essas três estruturas passam a operar de forma integrada com a **regra de compatibilidade transfusional ABO/Rh**: quando um hospital solicita bolsas para um receptor de determinado tipo sanguíneo, o sistema não se restringe ao tipo idêntico (*isogrupo*), mas avalia todos os tipos doadores biologicamente compatíveis (*heterogrupo*), selecionando de maneira gulosa a bolsa válida que vence primeiro (**FEFO global entre tipos compatíveis**) e acoplando o cálculo prévio da **rota de menor custo (Dijkstra)**.

Este documento registra:
1. As **regras de compatibilidade ABO/Rh adotadas** (matriz $8 \times 8$ para concentrado de hemácias) e suas fontes didáticas e normativas;
2. A **análise de complexidade assintótica (Big-O)** da consulta à matriz de compatibilidade e do algoritmo de *matching* + alocação;
3. O **diagrama e especificação do fluxo completo** `Alocacao = Compatibilidade ABO/Rh + FEFO + Rota (Dijkstra)`.

---

## 2. Regras de Compatibilidade ABO/Rh Adotadas e Fontes

### 2.1 Escopo Didático: Transfusão de Concentrado de Hemácias (Erithrócitos)

Para fins didáticos e operacionais do domínio do **Rota Vital**, a regra de compatibilidade sanguínea implementada em [`TipoSanguineo.java`](../rotavital/src/main/java/com/hemorede/domain/enums/TipoSanguineo.java) e exposta por [`CompatibilidadeSanguineaService.java`](../rotavital/src/main/java/com/hemorede/service/CompatibilidadeSanguineaService.java) adota o modelo clássico de **transfusão de concentrado de hemácias** (*packed red blood cells*):

1. **Sistema ABO (Antígenos de superfície A e B vs. Anticorpos plasmáticos anti-A e anti-B):**
   - O **doador $O$** não possui antígenos $A$ nem $B$ na membrana das hemácias, podendo doar hemácias para receptores $O, A, B$ e $AB$.
   - O **doador $A$** possui antígeno $A$, podendo doar para receptores que não possuam anticorpos anti-$A$ ($A$ e $AB$).
   - O **doador $B$** possui antígeno $B$, podendo doar para receptores que não possuam anticorpos anti-$B$ ($B$ e $AB$).
   - O **doador $AB$** possui ambos os antígenos $A$ e $B$, podendo doar hemácias exclusivamente para receptores $AB$ (receptor universal de hemácias).
2. **Fator Rh — Antígeno D ($\text{Rh}^+$ vs. $\text{Rh}^-$):**
   - Doadores $\text{Rh}^-$ (ausência do antígeno D) podem doar tanto para receptores $\text{Rh}^-$ quanto para receptores $\text{Rh}^+$.
   - Doadores $\text{Rh}^+$ (presença do antígeno D) só podem doar para receptores $\text{Rh}^+$, evitando aloimunização anti-D em pacientes $\text{Rh}^-$.

Formalmente, dados um tipo doador $D = (\text{ABO}_D, \text{Rh}_D)$ e um tipo receptor $R = (\text{ABO}_R, \text{Rh}_R)$:

$$\text{compatível}(D, R) \iff \left(\text{Antígenos}_{\text{ABO}}(D) \subseteq \text{Antígenos}_{\text{ABO}}(R)\right) \land \left(\text{Rh}_D = + \implies \text{Rh}_R = +\right)$$

### 2.2 Matriz de Compatibilidade $8 \times 8$ (Doador $\times$ Receptor)

A tabela abaixo representa a matriz booleana $M_{8 \times 8}$ onde as **linhas** correspondem ao tipo sanguíneo da bolsa **doadora** em estoque e as **colunas** correspondem ao tipo sanguíneo do **receptor** informado no item da requisição hospitalar (`1` / `✓` = compatível; `0` / `—` = incompatível):

| Doador $\downarrow$ \ Receptor $\rightarrow$ | `O-` (`O_NEG`) | `O+` (`O_POS`) | `A-` (`A_NEG`) | `A+` (`A_POS`) | `B-` (`B_NEG`) | `B+` (`B_POS`) | `AB-` (`AB_NEG`) | `AB+` (`AB_POS`) | Total de Receptores Atendidos |
| :--- | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: | :---: |
| **`O-` (`O_NEG`) — Doador Universal** | **✓** | **✓** | **✓** | **✓** | **✓** | **✓** | **✓** | **✓** | **8 / 8** |
| **`O+` (`O_POS`)** | — | **✓** | — | **✓** | — | **✓** | — | **✓** | **4 / 8** |
| **`A-` (`A_NEG`)** | — | — | **✓** | **✓** | — | — | **✓** | **✓** | **4 / 8** |
| **`A+` (`A_POS`)** | — | — | — | **✓** | — | — | — | **✓** | **2 / 8** |
| **`B-` (`B_NEG`)** | — | — | — | — | **✓** | **✓** | **✓** | **✓** | **4 / 8** |
| **`B+` (`B_POS`)** | — | — | — | — | — | **✓** | — | **✓** | **2 / 8** |
| **`AB-` (`AB_NEG`)** | — | — | — | — | — | — | **✓** | **✓** | **2 / 8** |
| **`AB+` (`AB_POS`)** | — | — | — | — | — | — | — | **✓** | **1 / 8** |
| **Total de Tipos Doadores Aceitos** | **1** | **2** | **2** | **4** | **2** | **4** | **4** | **8 (Receptor Universal)** | **27 pares válidos de 64** |

### 2.3 Conjuntos de Doadores Compatíveis por Receptor (Visão da Alocação)

Na implementação de [`TipoSanguineo.java`](../rotavital/src/main/java/com/hemorede/domain/enums/TipoSanguineo.java#L13-L22), cada receptor $R$ mapeia diretamente para a lista de seus tipos doadores válidos $C(R)$:

| Receptor Solicitado (`ItemRequisicao`) | Conjunto de Doadores Compatíveis $C(R)$ | Cardinalidade $|C(R)|$ |
| :--- | :--- | :---: |
| **`O_NEG` (`O-`)** | `[O_NEG]` | 1 |
| **`O_POS` (`O+`)** | `[O_POS, O_NEG]` | 2 |
| **`A_NEG` (`A-`)** | `[A_NEG, O_NEG]` | 2 |
| **`A_POS` (`A+`)** | `[A_POS, A_NEG, O_POS, O_NEG]` | 4 |
| **`B_NEG` (`B-`)** | `[B_NEG, O_NEG]` | 2 |
| **`B_POS` (`B+`)** | `[B_POS, B_NEG, O_POS, O_NEG]` | 4 |
| **`AB_NEG` (`AB-`)** | `[A_NEG, B_NEG, AB_NEG, O_NEG]` | 4 |
| **`AB_POS` (`AB+`)** | `[A_POS, A_NEG, B_POS, B_NEG, AB_POS, AB_NEG, O_POS, O_NEG]` | 8 |

### 2.4 Fontes Didáticas e Normativas Adotadas

1. **Ministério da Saúde (Brasil):** *Guia para o Uso de Hemocomponentes*, 2ª edição, Secretaria de Atenção à Saúde, Departamento de Atenção Especializada e Temática (DAET), Brasília: Ministério da Saúde, 2015 — Capítulos de Transfusão de Concentrado de Hemácias e Compatibilidade ABO/RhD.
2. **Legislação Hemoterápica Brasileira:** Portaria de Consolidação MS nº 5/2017, Anexo IV (origem: Portaria MS nº 158/2016) e RDC ANVISA nº 34/2014 (*Boas Práticas no Ciclo do Sangue*).
3. **Referência Clássica de Imuno-hematologia:** Harmening, D. M. *Modern Blood Banking & Transfusion Practices*, 7th Edition, F.A. Davis Company — Tabelas de compatibilidade eritrocitária ABO e sistema Rh.
4. **Referência de Estruturas de Dados e Algoritmos:** Cormen, T. H.; Leiserson, C. E.; Rivest, R. L.; Stein, C. *Algoritmos: Teoria e Prática* (3ª ed.), Campus/Elsevier — Filas de Prioridade baseadas em Min-Heap (Cap. 6), Tabelas de Dispersão (Cap. 11) e Caminho Mínimo de Fonte Única / Algoritmo de Dijkstra (Cap. 24).
