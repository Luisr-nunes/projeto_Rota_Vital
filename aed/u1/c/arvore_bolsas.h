/*
 * arvore_bolsas.h — CATÁLOGO de bolsas em Árvore Binária de Busca (ABB) por id.
 *
 * Por que ABB? A lista do estoque guarda a ordem de chegada, mas consultar
 * uma bolsa pelo id nela custa O(n). Na ABB, cada comparação descarta uma
 * subárvore inteira: busca/inserção/remoção custam O(h) — O(log n) quando a
 * árvore está equilibrada, O(n) no pior caso (ids inseridos já ordenados).
 * O percurso em-ordem devolve as bolsas ordenadas por id (rastreabilidade).
 *
 * Todas as operações são RECURSIVAS (conteúdo de recursão da AV1).
 *
 * Equivalente Java: com.hemorede.estruturas.ArvoreBolsas
 */
#ifndef ARVORE_BOLSAS_H
#define ARVORE_BOLSAS_H

#include "dominio.h"

typedef struct NoArvore {
    Bolsa bolsa;
    struct NoArvore *esquerda;  /* ids menores */
    struct NoArvore *direita;   /* ids maiores */
} NoArvore;

typedef struct {
    NoArvore *raiz;
    int tamanho;
} ArvoreBolsas;

/* Função chamada para cada bolsa no percurso em-ordem. */
typedef void (*VisitaBolsa)(const Bolsa *bolsa, void *contexto);

ArvoreBolsas *arvore_criar(void);

/* Retorna 1 se inseriu, 0 se o id já existe, -1 se faltou memória. */
int arvore_inserir(ArvoreBolsas *arvore, Bolsa bolsa);

/* Ponteiro para a bolsa dentro do nó, ou NULL. */
Bolsa *arvore_buscar(const ArvoreBolsas *arvore, long id);

/* Retorna 1 se removeu, 0 se não encontrou. */
int arvore_remover(ArvoreBolsas *arvore, long id);

/* Percurso em-ordem (esquerda, nó, direita) = ids crescentes. */
void arvore_em_ordem(const ArvoreBolsas *arvore, VisitaBolsa visitar, void *contexto);

/* Altura: árvore vazia = 0, só a raiz = 1. */
int arvore_altura(const ArvoreBolsas *arvore);

int arvore_tamanho(const ArvoreBolsas *arvore);

/* Libera todos os nós (pós-ordem) e a árvore. */
void arvore_destruir(ArvoreBolsas *arvore);

#endif
