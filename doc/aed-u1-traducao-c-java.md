# AED — Entrega do Projeto Integrador, Unidade 1

## Estruturas de domínio do Rota Vital em C (memória manual) e em Java (Spring Boot), com tradução comentada

**Projeto:** Rota Vital — gestão e distribuição de hemocomponentes · **Equipe 05** · ADS 3º período · CESAR School 2026.2
**Disciplina:** Algoritmos e Estruturas de Dados (AED) · **Avaliação:** AV1 (semanas 8–9)

| Parte da entrega | Onde está |
|---|---|
| Código-fonte em C (malloc/free, ponteiros explícitos) | [`aed/u1/c/`](../aed/u1/c) |
| Reimplementação em Java pronta para o Spring Boot | [`rotavital/src/main/java/com/hemorede/estruturas/`](../rotavital/src/main/java/com/hemorede/estruturas) |
| Testes em C (85 verificações + valgrind) | [`aed/u1/c/testes.c`](../aed/u1/c/testes.c) |
| Testes em Java (JUnit 5, mesmos cenários) | [`EstruturasU1Test.java`](../rotavital/src/test/java/com/hemorede/estruturas/EstruturasU1Test.java) |
| Saídas de execução (evidência) | [`aed/u1/evidencias/`](../aed/u1/evidencias) |
| Este documento (tradução comentada) | `doc/aed-u1-traducao-c-java.md` |

---

## 1. Escopo

**Conteúdo da AV1 coberto:** listas, filas, pilhas, árvores (ABB) e recursão.

| Estrutura | Papel no domínio | C | Java |
|---|---|---|---|
| Lista simplesmente encadeada | **Estoque** de bolsas do hemocentro | `lista_estoque.c` | `ListaEstoque` |
| Fila encadeada (FIFO) | **Requisições** hospitalares aguardando atendimento | `fila_requisicoes.c` | `FilaRequisicoes` |
| Pilha encadeada (LIFO) | **Histórico** de operações do estoque, com *desfazer* | `pilha_historico.c` | `PilhaHistorico` |
| Lista + pilha | Entrada/saída de bolsa registrada no histórico | `gestao_estoque.c` | `GestaoEstoque` |
| Árvore binária de busca | **Catálogo** de bolsas ordenado por id | `arvore_bolsas.c` | `ArvoreBolsas` |

**Fora do escopo, de propósito** (reservado para a Unidade 2, como pede o enunciado): roteirização, FEFO, índice por hash e compatibilidade ABO/Rh. Por isso a lista guarda as bolsas **na ordem de chegada** (não ordenadas por validade) e a fila é **FIFO pura** (a prioridade URGENTE é apenas um dado da requisição; ela não altera a ordem nesta unidade). Nenhuma das estruturas desta entrega chama `FilaFEFO`, `IndiceEstoque`, `GrafoRotas` ou `TipoSanguineo.compativel`.

---

## 2. Modelagem de domínio

### 2.1 Entidades

As structs em C foram derivadas das entidades JPA que a aplicação de POO já usa, com os mesmos nomes de campos e de enums, para que a tradução seja direta:

| Java (`com.hemorede.domain`) | C (`aed/u1/c/dominio.h`) | Observação |
|---|---|---|
| `Bolsa` (id, hemoComponente, tipoSanguineo, dataColeta, dataValidade, status) | `typedef struct { ... } Bolsa;` | `LocalDate` vira `struct Data {ano, mes, dia}`. Os relacionamentos JPA (doador, estoque) não entram: não são usados pelas estruturas. |
| `Requisicao` + `ItemRequisicao` | `typedef struct { ... } Requisicao;` | Em C, a requisição tem **um item** (componente, tipo, quantidade) embutido. Em Java a fila guarda a entidade `Requisicao` completa, com sua lista de itens. |
| enums `HemoComponente`, `TipoSanguineo`, `StatusBolsa`, `Prioridade` | enums C com as mesmas constantes | Mesma ordem e mesmos nomes (`HEMACIAS`, `O_NEG`, `DISPONIVEL`, `URGENTE`...). |

### 2.2 Por que cada estrutura

- **Estoque → lista encadeada.** O estoque muda de tamanho o tempo todo (doações entram, bolsas saem para hospitais ou são descartadas) e não tem um máximo conhecido em tempo de compilação. Um vetor exigiria `realloc` e cópia de todos os elementos ao crescer, e deslocamento ao remover do meio. Na lista, cada bolsa ocupa um nó alocado individualmente; remover do meio é só religar dois ponteiros. Com o ponteiro `fim`, a inserção no fim não precisa percorrer a lista.
- **Requisições → fila.** Pedidos de hospitais devem ser atendidos na ordem em que chegaram — exatamente a disciplina FIFO. Com `inicio` e `fim`, enfileirar e desenfileirar custam O(1).
- **Histórico → pilha.** O uso do histórico é auditar e **desfazer a última operação** lançada por engano. Desfazer sempre age sobre a mais recente: último a entrar, primeiro a sair. Empilhar/desempilhar no topo são O(1).
- **Catálogo por id → ABB.** Consultar uma bolsa pelo id na lista custa O(n). Na ABB, cada comparação descarta uma subárvore: O(h), que é O(log n) com a árvore equilibrada. O percurso em-ordem devolve as bolsas ordenadas por id para relatórios de rastreabilidade. É a estrutura onde a **recursão** aparece de forma natural (inserir, buscar, remover, percorrer, altura, destruir).

---

## 3. Como compilar e testar

**C** (gcc, padrão C99):

```bash
cd aed/u1/c
make test       # 85 verificações, compiladas com AddressSanitizer (acusa vazamento e uso após free)
make valgrind   # roda testes e demonstração no valgrind
make run        # demonstração com dados sintéticos
```

No Windows, com MinGW: `gcc -std=c99 -Wall -o testes.exe testes.c dominio.c lista_estoque.c fila_requisicoes.c pilha_historico.c gestao_estoque.c arvore_bolsas.c` e depois `testes.exe`.

**Java:**

```bash
cd rotavital
mvn test -Dtest=EstruturasU1Test
```

Resultado obtido (arquivos completos em `aed/u1/evidencias/`):

```
C:     85 verificacoes, 0 falha(s)
       valgrind: total heap usage: 43 allocs, 43 frees — All heap blocks were freed -- no leaks are possible
Java:  EstruturasU1Test — 10 testes, 0 falhas (suíte completa do projeto: 96 testes, 0 falhas)
```

O número de `malloc` é igual ao de `free` (43 = 43): cada nó criado nos testes foi liberado.

---

## 4. Tradução comentada — estrutura por estrutura

### 4.1 Estoque — lista encadeada

**C** (`lista_estoque.c`, inserção e remoção):

```c
int lista_inserir(ListaEstoque *lista, Bolsa bolsa) {
    NoBolsa *novo;
    if (lista_buscar(lista, bolsa.id) != NULL) {
        return LISTA_DUPLICADA;
    }
    novo = (NoBolsa *) malloc(sizeof(NoBolsa));
    if (novo == NULL) {
        return LISTA_SEM_MEMORIA;
    }
    novo->bolsa = bolsa;
    novo->proximo = NULL;
    if (lista->fim == NULL) {
        lista->inicio = novo;
    } else {
        lista->fim->proximo = novo;
    }
    lista->fim = novo;
    lista->tamanho++;
    return LISTA_OK;
}

int lista_remover(ListaEstoque *lista, long id, Bolsa *removida) {
    NoBolsa *anterior = NULL;
    NoBolsa *atual = lista->inicio;
    while (atual != NULL && atual->bolsa.id != id) {
        anterior = atual;
        atual = atual->proximo;
    }
    if (atual == NULL) return 0;
    if (anterior == NULL) lista->inicio = atual->proximo;
    else                  anterior->proximo = atual->proximo;
    if (atual == lista->fim) lista->fim = anterior;
    if (removida != NULL) *removida = atual->bolsa;   /* copia ANTES do free */
    free(atual);
    lista->tamanho--;
    return 1;
}
```

**Java** (`ListaEstoque.java`):

```java
public boolean inserir(Bolsa bolsa) {
    validar(bolsa);
    if (buscar(bolsa.getId()) != null) {
        return false;
    }
    NoBolsa novo = new NoBolsa(bolsa);
    if (fim == null) {
        inicio = novo;
    } else {
        fim.proximo = novo;
    }
    fim = novo;
    tamanho++;
    return true;
}

public Bolsa remover(long id) {
    NoBolsa anterior = null;
    NoBolsa atual = inicio;
    while (atual != null && atual.bolsa.getId() != id) {
        anterior = atual;
        atual = atual.proximo;
    }
    if (atual == null) return null;
    if (anterior == null) inicio = atual.proximo;
    else                  anterior.proximo = atual.proximo;
    if (atual == fim) fim = anterior;
    atual.proximo = null;          // sem referências, o GC recolhe o nó
    tamanho--;
    return atual.bolsa;
}
```

**Por que a versão Java corresponde à versão C.** A classe interna `private static final class NoBolsa { Bolsa bolsa; NoBolsa proximo; }` é o `struct NoBolsa`, e os três campos de `ListaEstoque` (`inicio`, `fim`, `tamanho`) são os mesmos três campos do `struct ListaEstoque`. Em `inserir`, o `new NoBolsa(bolsa)` faz o papel do `malloc(sizeof(NoBolsa))` seguido de `novo->bolsa = bolsa; novo->proximo = NULL` (o construtor atribui a bolsa e o Java inicializa `proximo` com `null`); o teste `if (fim == null)` e as duas atribuições `fim.proximo = novo; fim = novo;` são, linha a linha, o encadeamento no fim da versão C, e a checagem de id duplicado via `buscar` antes de alocar é a mesma (C devolve `LISTA_DUPLICADA`, Java devolve `false`). Em `remover`, o laço `while` com os dois ponteiros `anterior`/`atual` é idêntico, assim como os dois casos de religação (primeiro nó → move `inicio`; nó do meio ou do fim → `anterior.proximo = atual.proximo`) e o ajuste de `fim` quando o removido era o último — esse ajuste é o detalhe que o teste `lista_remover(l, 30, ...)` seguido de `l->fim->bolsa.id == 10` verifica nas duas linguagens. A diferença real está na liberação: em C o nó precisa de `free(atual)`, e por isso o conteúdo é copiado para `*removida` **antes** do `free` (depois dele o acesso seria uso de memória liberada); em Java não existe `free` — ao religar `anterior.proximo` e fazer `atual.proximo = null`, o nó fica sem nenhuma referência e o coletor de lixo o recolhe, então a bolsa pode ser devolvida diretamente. O código de erro `LISTA_SEM_MEMORIA` (retorno `NULL` do `malloc`) não tem equivalente explícito: em Java a falta de memória vira `OutOfMemoryError`, lançado pela própria JVM.

**Consulta e recursão.** `lista_buscar` devolve `&atual->bolsa`, um ponteiro para dentro do nó, de modo que `lista_buscar(l, 20)->status = RESERVADA` altera a bolsa guardada no estoque; `buscar` em Java devolve a referência ao mesmo objeto `Bolsa` guardado, e `l.buscar(20).setStatus(StatusBolsa.RESERVADA)` tem o mesmo efeito — os dois testes conferem isso contando as disponíveis logo em seguida. A contagem `lista_contar_disponiveis_rec(no->proximo, ...)` e `contarDisponiveisRec(no.proximo, ...)` têm o mesmo caso base (`no == NULL` → 0) e o mesmo passo (1 se o nó atual casa, mais a contagem do resto).

**Uma diferença de semântica que foi mantida conscientemente.** Em C o nó guarda uma **cópia** da bolsa (`novo->bolsa = bolsa`, atribuição de struct por valor); em Java o nó guarda a **referência** recebida. Isso significa que, em Java, quem inseriu a bolsa continua apontando para o mesmo objeto do estoque. Escolhemos não clonar a entidade em Java porque ela é gerenciada pelo JPA: a aplicação precisa que o estoque em memória e a entidade persistida sejam o mesmo objeto, para que uma mudança de status salva pelo repositório seja vista pela estrutura.

**Destruição.** `lista_destruir` percorre a lista guardando `proximo` antes de cada `free(atual)` e, por fim, faz `free(lista)`. Em Java não há método equivalente: quando a última referência à `ListaEstoque` deixa de existir, todos os nós ficam inalcançáveis e são coletados juntos.

### 4.2 Requisições — fila encadeada FIFO

**C** (`fila_requisicoes.c`):

```c
int fila_enfileirar(FilaRequisicoes *fila, Requisicao requisicao) {
    NoRequisicao *novo = (NoRequisicao *) malloc(sizeof(NoRequisicao));
    if (novo == NULL) return 0;
    novo->requisicao = requisicao;
    novo->proximo = NULL;
    if (fila->fim == NULL) fila->inicio = novo;
    else                   fila->fim->proximo = novo;
    fila->fim = novo;
    fila->tamanho++;
    return 1;
}

int fila_desenfileirar(FilaRequisicoes *fila, Requisicao *saida) {
    NoRequisicao *removido;
    if (fila->inicio == NULL) return 0;             /* underflow */
    removido = fila->inicio;
    if (saida != NULL) *saida = removido->requisicao;
    fila->inicio = removido->proximo;
    if (fila->inicio == NULL) fila->fim = NULL;     /* esvaziou */
    free(removido);
    fila->tamanho--;
    return 1;
}
```

**Java** (`FilaRequisicoes.java`):

```java
public void enfileirar(Requisicao requisicao) {
    Objects.requireNonNull(requisicao, "requisição não pode ser nula");
    NoRequisicao novo = new NoRequisicao(requisicao);
    if (fim == null) inicio = novo;
    else             fim.proximo = novo;
    fim = novo;
    tamanho++;
}

public Requisicao desenfileirar() {
    if (inicio == null) return null;
    NoRequisicao removido = inicio;
    inicio = removido.proximo;
    if (inicio == null) fim = null;
    removido.proximo = null;
    tamanho--;
    return removido.requisicao;
}
```

**Por que a versão Java corresponde à versão C.** As duas filas são a mesma lista encadeada com acesso restrito às pontas: inserção só por `fim`, remoção só por `inicio`, e por isso as duas operações são O(1) — nenhum laço percorre a fila. `enfileirar` repete a lógica de `fila_enfileirar` (fila vazia → o novo nó é ao mesmo tempo `inicio` e `fim`; caso contrário, `fim.proximo = novo`). Em `desenfileirar`, o ponto mais delicado da versão C é a linha `if (fila->inicio == NULL) fila->fim = NULL;`: sem ela, depois de esvaziar a fila o ponteiro `fim` continuaria apontando para um nó já liberado com `free(removido)`, e o próximo `fila_enfileirar` escreveria em memória inválida (`fila->fim->proximo = novo`). A versão Java mantém exatamente a mesma linha (`if (inicio == null) fim = null;`) — em Java não haveria escrita em memória liberada, mas sem ela o nó antigo continuaria sendo referenciado por `fim` e a próxima inserção seria ligada a ele em vez de virar o novo `inicio`, perdendo a requisição. Os testes das duas versões cobrem exatamente esse caminho: esvaziam a fila, verificam `fim == NULL` (C) / `vazia()` (Java) e enfileiram de novo, conferindo que a nova requisição é a frente. O *underflow* é sinalizado em C pelo retorno `0` com o dado copiado por um parâmetro de saída (`Requisicao *saida`), e em Java pelo retorno `null`, já que um método Java devolve diretamente a referência à requisição. `fila_consultar`/`consultar` e `fila_posicao`/`posicao` são o mesmo percurso sequencial do `inicio` ao fim, usado só para consulta, sem alterar a ordem.

### 4.3 Histórico — pilha encadeada LIFO

**C** (`pilha_historico.c`):

```c
int pilha_empilhar(PilhaHistorico *pilha, OperacaoEstoque operacao) {
    NoOperacao *novo = (NoOperacao *) malloc(sizeof(NoOperacao));
    if (novo == NULL) return 0;
    novo->operacao = operacao;
    novo->abaixo = pilha->topo;
    pilha->topo = novo;
    pilha->tamanho++;
    return 1;
}

int pilha_desempilhar(PilhaHistorico *pilha, OperacaoEstoque *saida) {
    NoOperacao *removido;
    if (pilha->topo == NULL) return 0;
    removido = pilha->topo;
    if (saida != NULL) *saida = removido->operacao;
    pilha->topo = removido->abaixo;
    free(removido);
    pilha->tamanho--;
    return 1;
}
```

**Java** (`PilhaHistorico.java`):

```java
public void empilhar(OperacaoEstoque operacao) {
    Objects.requireNonNull(operacao, "operação não pode ser nula");
    topo = new NoOperacao(operacao, topo);
    tamanho++;
}

public OperacaoEstoque desempilhar() {
    if (topo == null) return null;
    NoOperacao removido = topo;
    topo = removido.abaixo;
    tamanho--;
    return removido.operacao;
}
```

**Por que a versão Java corresponde à versão C.** A pilha é uma lista encadeada em que só se mexe na cabeça, chamada `topo`, e cada nó aponta para o nó `abaixo`. Em C, empilhar são três passos explícitos: alocar o nó, fazer `novo->abaixo = pilha->topo` e depois `pilha->topo = novo`. Em Java, a linha `topo = new NoOperacao(operacao, topo);` faz os mesmos três passos: o `new` aloca, o construtor recebe o **topo antigo** como argumento e o guarda em `abaixo` (a expressão do lado direito é avaliada antes da atribuição), e só então `topo` passa a referenciar o novo nó. Por isso o campo `abaixo` pôde ser `final` em Java: depois de empilhado, um nó nunca muda de vizinho. Desempilhar é o espelho: guardar o nó do topo, mover `topo` para `removido.abaixo` e liberar o nó — `free(removido)` em C, nenhuma referência restante (e coleta pelo GC) em Java. A operação registrada é um `struct OperacaoEstoque { TipoOperacao tipo; Bolsa bolsa; }` em C e um `record OperacaoEstoque(TipoOperacao tipo, Bolsa bolsa)` em Java; o `record` é imutável, o que corresponde ao fato de que, em C, a operação é copiada por valor para dentro do nó e ninguém mais a altera.

### 4.4 Gestão do estoque — lista + pilha, com desfazer

**C** (`gestao_estoque.c`):

```c
int estoque_registrar_saida(GestaoEstoque *g, long id, Bolsa *saiu) {
    OperacaoEstoque op;
    Bolsa removida;
    if (!lista_remover(g->estoque, id, &removida)) return 0;
    op.tipo = OP_SAIDA;
    op.bolsa = removida;                         /* snapshot para poder desfazer */
    if (!pilha_empilhar(g->historico, op)) {
        lista_inserir(g->estoque, removida);     /* rollback */
        return 0;
    }
    if (saiu != NULL) *saiu = removida;
    return 1;
}

int estoque_desfazer(GestaoEstoque *g, OperacaoEstoque *desfeita) {
    OperacaoEstoque op;
    if (!pilha_desempilhar(g->historico, &op)) return 0;
    if (op.tipo == OP_ENTRADA) lista_remover(g->estoque, op.bolsa.id, NULL);
    else                       lista_inserir(g->estoque, op.bolsa);
    if (desfeita != NULL) *desfeita = op;
    return 1;
}
```

**Java** (`GestaoEstoque.java`):

```java
public Bolsa registrarSaida(long id) {
    Bolsa removida = estoque.remover(id);
    if (removida == null) return null;
    historico.empilhar(new OperacaoEstoque(TipoOperacao.SAIDA, removida));
    return removida;
}

public OperacaoEstoque desfazer() {
    OperacaoEstoque op = historico.desempilhar();
    if (op == null) return null;
    if (op.tipo() == TipoOperacao.ENTRADA) estoque.remover(op.bolsa().getId());
    else                                   estoque.inserir(op.bolsa());
    return op;
}
```

**Por que a versão Java corresponde à versão C.** As duas versões aplicam a mesma regra: toda mudança no estoque passa pela gestão, que primeiro altera a lista e só registra na pilha se a alteração aconteceu (entrada duplicada ou saída de id inexistente não geram histórico — os dois conjuntos de testes conferem que o histórico fica com 2 operações depois da tentativa duplicada). Desfazer desempilha a operação mais recente e aplica a inversa: ENTRADA → remover da lista; SAIDA → reinserir a bolsa guardada na operação. Para a SAIDA poder ser desfeita, a bolsa precisa sobreviver à remoção do nó: em C isso exige copiar o conteúdo (`lista_remover(..., &removida)` preenche `removida` antes do `free` do nó, e `op.bolsa = removida` guarda essa cópia na pilha); em Java basta guardar a referência devolvida por `estoque.remover(id)` dentro do `OperacaoEstoque` — enquanto a operação estiver na pilha, a bolsa continua alcançável e não é coletada. Os dois blocos de *rollback* da versão C (quando `pilha_empilhar` falha por falta de memória, desfazer a alteração na lista para não deixar lista e pilha incoerentes) não têm equivalente em Java porque `new` não devolve `null`: se faltar memória, a JVM lança `OutOfMemoryError` e a aplicação não continua em estado parcial silencioso. Pelo mesmo motivo, `gestao_criar` em C precisa liberar o que já alocou se um dos dois `malloc` falhar, enquanto em Java os campos `estoque` e `historico` são simplesmente inicializados na declaração.

### 4.5 Catálogo por id — árvore binária de busca (recursiva)

**C** (`arvore_bolsas.c`, inserção e remoção):

```c
static NoArvore *inserir_rec(NoArvore *no, Bolsa bolsa, int *resultado) {
    if (no == NULL) {
        NoArvore *novo = (NoArvore *) malloc(sizeof(NoArvore));
        if (novo == NULL) { *resultado = -1; return NULL; }
        novo->bolsa = bolsa;
        novo->esquerda = NULL;
        novo->direita = NULL;
        *resultado = 1;
        return novo;
    }
    if (bolsa.id < no->bolsa.id)      no->esquerda = inserir_rec(no->esquerda, bolsa, resultado);
    else if (bolsa.id > no->bolsa.id) no->direita  = inserir_rec(no->direita, bolsa, resultado);
    else                              *resultado = 0;   /* duplicado */
    return no;
}

static NoArvore *remover_rec(NoArvore *no, long id, int *removeu) {
    if (no == NULL) return NULL;
    if (id < no->bolsa.id)      no->esquerda = remover_rec(no->esquerda, id, removeu);
    else if (id > no->bolsa.id) no->direita  = remover_rec(no->direita, id, removeu);
    else {
        NoArvore *filho;
        if (no->esquerda == NULL || no->direita == NULL) {      /* 0 ou 1 filho */
            filho = no->esquerda != NULL ? no->esquerda : no->direita;
            free(no);
            *removeu = 1;
            return filho;
        }
        filho = menor_no(no->direita);                          /* 2 filhos */
        no->bolsa = filho->bolsa;
        no->direita = remover_rec(no->direita, filho->bolsa.id, removeu);
    }
    return no;
}
```

**Java** (`ArvoreBolsas.java`):

```java
private NoArvore inserirRec(NoArvore no, Bolsa bolsa) {
    if (no == null) {
        alterou = true;
        return new NoArvore(bolsa);
    }
    long id = bolsa.getId();
    long chave = no.bolsa.getId();
    if (id < chave)      no.esquerda = inserirRec(no.esquerda, bolsa);
    else if (id > chave) no.direita  = inserirRec(no.direita, bolsa);
    return no;   // id igual: não insere
}

private NoArvore removerRec(NoArvore no, long id) {
    if (no == null) return null;
    long chave = no.bolsa.getId();
    if (id < chave)      no.esquerda = removerRec(no.esquerda, id);
    else if (id > chave) no.direita  = removerRec(no.direita, id);
    else {
        if (no.esquerda == null || no.direita == null) {
            alterou = true;
            return no.esquerda != null ? no.esquerda : no.direita;
        }
        NoArvore sucessor = menorNo(no.direita);
        no.bolsa = sucessor.bolsa;
        no.direita = removerRec(no.direita, sucessor.bolsa.getId());
    }
    return no;
}
```

**Por que a versão Java corresponde à versão C.** As duas versões usam o mesmo padrão recursivo: a função recebe a raiz de uma subárvore e **devolve a nova raiz dessa subárvore**, e quem chama religa o ponteiro (`no->esquerda = inserir_rec(no->esquerda, ...)` em C, `no.esquerda = inserirRec(no.esquerda, ...)` em Java). Escolhemos esse padrão em C justamente porque ele se traduz sem mudanças para Java: a alternativa clássica em C, passar um ponteiro para ponteiro (`NoArvore **`) e escrever `*pno = novo`, não existe em Java, que não tem ponteiro para variável. O caso base da inserção é o mesmo (subárvore vazia → cria o nó com `malloc`/`new` e o devolve para ser pendurado no pai), e a remoção trata os mesmos três casos na mesma ordem: folha ou nó com um filho (o filho, ou `NULL`/`null`, sobe para o lugar do nó) e nó com dois filhos (copia a bolsa do sucessor em-ordem — o menor da subárvore direita, achado por `menor_no`/`menorNo` — e remove recursivamente o sucessor da direita). Os testes das duas linguagens removem a raiz 50 com dois filhos e conferem que o 60 subiu para a raiz. Duas diferenças são só de mecanismo: (1) em C a informação "inseriu/removeu?" volta por um parâmetro de saída `int *resultado`, porque o retorno da função já é usado para devolver o nó; em Java, sem ponteiros para `int`, a mesma informação vai no campo privado `alterou`, zerado antes de cada operação pública e lido depois dela; (2) no caso de 0 ou 1 filho, C precisa do `free(no)` antes de devolver o filho, enquanto em Java o nó retirado deixa de ser referenciado pelo pai (que recebe o filho no lugar) e é coletado. A destruição em C precisa ser em **pós-ordem** (`destruir_rec(esquerda); destruir_rec(direita); free(no);`), porque liberar o pai primeiro perderia os ponteiros para os filhos; em Java não existe esse método — basta perder a referência à raiz.

**Recursão nos demais métodos.** `buscar_rec`/`buscarRec`, `em_ordem_rec`/`emOrdemRec` e `altura_rec`/`alturaRec` têm o mesmo caso base (`no == NULL`) e o mesmo passo nas duas linguagens. No percurso em-ordem, o "visitante" é um ponteiro para função com contexto genérico em C (`void (*VisitaBolsa)(const Bolsa *, void *)`) e um `Consumer<Bolsa>` em Java; nos dois casos o percurso visita esquerda, nó e direita, e os testes conferem que a saída sai em ids crescentes (20, 30, 40, 50, 60, 70, 80).

**Pior caso documentado nos testes.** Inserindo os ids 1 a 10 já em ordem, cada novo nó vai sempre para a direita e a árvore degenera numa lista: altura 10 = n. Os dois testes (`teste_arvore` em C, `piorCasoIdsOrdenadosDegeneramEmLista` em Java) verificam isso, deixando explícito por que a complexidade é O(h) e não O(log n) garantido. Balanceamento (AVL) não faz parte desta unidade.

---

## 5. Resumo das correspondências

| Conceito | C | Java |
|---|---|---|
| Nó | `struct NoBolsa { Bolsa bolsa; struct NoBolsa *proximo; }` | `private static final class NoBolsa { Bolsa bolsa; NoBolsa proximo; }` |
| Criar nó | `malloc(sizeof(NoBolsa))` + teste de `NULL` | `new NoBolsa(bolsa)` (falha vira `OutOfMemoryError`) |
| Liberar nó | `free(no)` explícito, após copiar o que for preciso | Tirar todas as referências; o GC libera |
| Ponteiro nulo | `NULL` | `null` |
| Acesso a campo | `no->proximo`, `lista->fim` | `no.proximo`, `fim` |
| Dado no nó | cópia por valor do `struct` | referência à entidade JPA |
| "Não encontrado" / vazio | retorno `0` ou `NULL`, dado via parâmetro de saída | retorno `null` ou `false` |
| Sinalizar resultado em recursão | parâmetro `int *resultado` | campo privado `alterou` |
| Visitante do percurso | ponteiro para função + `void *contexto` | `Consumer<Bolsa>` |
| Destruir a estrutura | `*_destruir` percorre e libera cada nó | não existe; última referência perdida → tudo coletado |

### Custo das operações (o mesmo nas duas versões, porque o algoritmo é o mesmo)

| Estrutura | Inserir | Remover | Consultar |
|---|---|---|---|
| Lista (estoque) | O(n) — a checagem de id duplicado percorre a lista; o encadeamento em si é O(1) pelo ponteiro `fim` | O(n) — busca do nó | O(n) por id; contagem recursiva O(n) |
| Fila (requisições) | O(1) | O(1) | O(1) na frente; O(n) por id |
| Pilha (histórico) | O(1) | O(1) | O(1) no topo |
| ABB (catálogo) | O(h) | O(h) | O(h); em-ordem O(n) |

---

## 6. Como a aplicação de POO consome as estruturas

As classes Java não usam anotações nem injeção do Spring e não dependem de repositório ou banco: recebem e devolvem as entidades `Bolsa` e `Requisicao` já usadas pelos services. Um service pode criá-las diretamente ou declará-las como bean:

```java
@Service
public class PainelEstoqueService {

    private final GestaoEstoque gestao = new GestaoEstoque();
    private final FilaRequisicoes pendentes = new FilaRequisicoes();
    private final ArvoreBolsas catalogo = new ArvoreBolsas();

    public PainelEstoqueService(BolsaRepository bolsas) {
        bolsas.findAll().forEach(b -> { gestao.registrarEntrada(b); catalogo.inserir(b); });
    }

    public List<Bolsa> estoqueAtual()      { return gestao.getEstoque().paraLista(); }
    public OperacaoEstoque desfazer()      { return gestao.desfazer(); }
    public List<Bolsa> catalogoOrdenado()  { return catalogo.paraListaOrdenada(); }
}
```

Os métodos `paraLista()` / `paraListaOrdenada()` existem só para a saída (controller → JSON) e devolvem uma cópia; eles não são usados internamente — por dentro, todas as estruturas são encadeadas à mão com referências, sem `ArrayList`, `LinkedList`, `ArrayDeque` ou `TreeMap`. As classes não são *thread-safe*; se forem compartilhadas entre requisições HTTP concorrentes, o service deve sincronizar o acesso.

---

## 7. Arquivos

```
aed/u1/
├── c/
│   ├── dominio.h / dominio.c                 tipos de domínio (Bolsa, Requisicao, enums)
│   ├── lista_estoque.h / .c                  estoque — lista encadeada
│   ├── fila_requisicoes.h / .c               requisições — fila FIFO
│   ├── pilha_historico.h / .c                histórico — pilha LIFO
│   ├── gestao_estoque.h / .c                 lista + pilha (entrada, saída, desfazer)
│   ├── arvore_bolsas.h / .c                  catálogo — ABB recursiva
│   ├── testes.c                              85 verificações
│   ├── demo.c                                cenário com dados sintéticos
│   └── Makefile
└── evidencias/                               saídas de testes, valgrind e JUnit

rotavital/src/main/java/com/hemorede/estruturas/
├── ListaEstoque.java
├── FilaRequisicoes.java
├── PilhaHistorico.java  (+ OperacaoEstoque.java, TipoOperacao.java)
├── GestaoEstoque.java
└── ArvoreBolsas.java

rotavital/src/test/java/com/hemorede/estruturas/EstruturasU1Test.java
```
