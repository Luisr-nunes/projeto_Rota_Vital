/*
 * fila_requisicoes.h — REQUISIÇÕES hospitalares em fila encadeada (FIFO).
 *
 * Por que fila? Requisições devem ser atendidas na ordem de chegada
 * (primeiro a entrar, primeiro a sair). Com ponteiros `inicio` e `fim`,
 * enfileirar (no fim) e desenfileirar (no início) são O(1).
 *
 * Observação de escopo U1: a fila é FIFO pura. Priorização por validade
 * (FEFO) e por compatibilidade ABO/Rh ficam para a Unidade 2.
 *
 * Equivalente Java: com.hemorede.estruturas.FilaRequisicoes
 */
#ifndef FILA_REQUISICOES_H
#define FILA_REQUISICOES_H

#include "dominio.h"

typedef struct NoRequisicao {
    Requisicao requisicao;
    struct NoRequisicao *proximo;
} NoRequisicao;

typedef struct {
    NoRequisicao *inicio;  /* próximo a ser atendido */
    NoRequisicao *fim;     /* último que chegou */
    int tamanho;
} FilaRequisicoes;

FilaRequisicoes *fila_criar(void);

/* Insere no fim. Retorna 1 se ok, 0 se faltou memória. O(1). */
int fila_enfileirar(FilaRequisicoes *fila, Requisicao requisicao);

/* Remove do início copiando em *saida. Retorna 1 se ok, 0 se vazia. O(1). */
int fila_desenfileirar(FilaRequisicoes *fila, Requisicao *saida);

/* Consulta o início sem remover (NULL se vazia). O(1). */
const Requisicao *fila_frente(const FilaRequisicoes *fila);

/* Consulta uma requisição pelo id sem removê-la (NULL se não houver). O(n). */
const Requisicao *fila_consultar(const FilaRequisicoes *fila, long id);

/* Posição (0 = próxima a ser atendida) de uma requisição, ou -1. O(n). */
int fila_posicao(const FilaRequisicoes *fila, long id);

int fila_tamanho(const FilaRequisicoes *fila);
int fila_vazia(const FilaRequisicoes *fila);

void fila_imprimir(const FilaRequisicoes *fila);
void fila_destruir(FilaRequisicoes *fila);

#endif
