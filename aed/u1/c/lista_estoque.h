/*
 * lista_estoque.h — ESTOQUE como lista simplesmente encadeada de bolsas.
 *
 * Por que lista? O estoque cresce e encolhe o tempo todo (doações entram,
 * bolsas saem para hospitais ou são descartadas) e não tem tamanho máximo
 * conhecido em tempo de compilação. A lista encadeada aloca um nó por bolsa
 * (malloc) e libera na saída (free), sem realocar/copiar um vetor inteiro.
 * Inserção no fim é O(1) graças ao ponteiro `fim`.
 *
 * Equivalente Java: com.hemorede.estruturas.ListaEstoque
 */
#ifndef LISTA_ESTOQUE_H
#define LISTA_ESTOQUE_H

#include "dominio.h"

typedef struct NoBolsa {
    Bolsa bolsa;              /* dado guardado por valor */
    struct NoBolsa *proximo;  /* NULL no último nó */
} NoBolsa;

typedef struct {
    NoBolsa *inicio;  /* primeiro nó (NULL se vazia) */
    NoBolsa *fim;     /* último nó  (NULL se vazia) — permite inserir no fim em O(1) */
    int tamanho;
} ListaEstoque;

/* Códigos de retorno das operações de inserção. */
#define LISTA_OK          1
#define LISTA_DUPLICADA   0
#define LISTA_SEM_MEMORIA (-1)

ListaEstoque *lista_criar(void);

/* Insere no fim. Rejeita id repetido (LISTA_DUPLICADA). O(n) pela checagem de id. */
int lista_inserir(ListaEstoque *lista, Bolsa bolsa);

/* Remove a bolsa com o id dado. Copia o conteúdo em *removida (se não-NULL)
 * antes do free. Retorna 1 se removeu, 0 se não encontrou. O(n). */
int lista_remover(ListaEstoque *lista, long id, Bolsa *removida);

/* Consulta por id. Devolve ponteiro para a bolsa DENTRO do nó (permite
 * atualizar status) ou NULL. O ponteiro é inválido após remover o nó. O(n). */
Bolsa *lista_buscar(const ListaEstoque *lista, long id);

int lista_tamanho(const ListaEstoque *lista);
int lista_vazia(const ListaEstoque *lista);

/* RECURSÃO: conta bolsas DISPONIVEIS de um tipo/componente a partir de `no`.
 * Caso base: no == NULL. Passo: (no atual casa ? 1 : 0) + contar(no->proximo). */
int lista_contar_disponiveis_rec(const NoBolsa *no, TipoSanguineo tipo, HemoComponente componente);

/* Conveniência: chama a versão recursiva a partir do início. */
int lista_contar_disponiveis(const ListaEstoque *lista, TipoSanguineo tipo, HemoComponente componente);

void lista_imprimir(const ListaEstoque *lista);

/* Libera todos os nós e a própria lista (free em cada nó). */
void lista_destruir(ListaEstoque *lista);

#endif
