/*
 * fila_requisicoes.c — Implementação da fila encadeada de requisições.
 */
#include <stdio.h>
#include <stdlib.h>
#include "fila_requisicoes.h"

FilaRequisicoes *fila_criar(void) {
    FilaRequisicoes *fila = (FilaRequisicoes *) malloc(sizeof(FilaRequisicoes));
    if (fila == NULL) {
        return NULL;
    }
    fila->inicio = NULL;
    fila->fim = NULL;
    fila->tamanho = 0;
    return fila;
}

int fila_enfileirar(FilaRequisicoes *fila, Requisicao requisicao) {
    NoRequisicao *novo = (NoRequisicao *) malloc(sizeof(NoRequisicao));
    if (novo == NULL) {
        return 0;
    }
    novo->requisicao = requisicao;
    novo->proximo = NULL;

    if (fila->fim == NULL) {
        fila->inicio = novo;          /* fila vazia: o novo é início e fim */
    } else {
        fila->fim->proximo = novo;    /* liga o antigo último ao novo */
    }
    fila->fim = novo;
    fila->tamanho++;
    return 1;
}

int fila_desenfileirar(FilaRequisicoes *fila, Requisicao *saida) {
    NoRequisicao *removido;
    if (fila->inicio == NULL) {
        return 0; /* underflow: fila vazia */
    }
    removido = fila->inicio;
    if (saida != NULL) {
        *saida = removido->requisicao;
    }
    fila->inicio = removido->proximo;
    if (fila->inicio == NULL) {
        fila->fim = NULL;             /* esvaziou: fim também precisa voltar a NULL */
    }
    free(removido);
    fila->tamanho--;
    return 1;
}

const Requisicao *fila_frente(const FilaRequisicoes *fila) {
    return fila->inicio == NULL ? NULL : &fila->inicio->requisicao;
}

const Requisicao *fila_consultar(const FilaRequisicoes *fila, long id) {
    NoRequisicao *atual;
    for (atual = fila->inicio; atual != NULL; atual = atual->proximo) {
        if (atual->requisicao.id == id) {
            return &atual->requisicao;
        }
    }
    return NULL;
}

int fila_posicao(const FilaRequisicoes *fila, long id) {
    NoRequisicao *atual;
    int posicao = 0;
    for (atual = fila->inicio; atual != NULL; atual = atual->proximo) {
        if (atual->requisicao.id == id) {
            return posicao;
        }
        posicao++;
    }
    return -1;
}

int fila_tamanho(const FilaRequisicoes *fila) {
    return fila->tamanho;
}

int fila_vazia(const FilaRequisicoes *fila) {
    return fila->inicio == NULL;
}

void fila_imprimir(const FilaRequisicoes *fila) {
    NoRequisicao *atual;
    int posicao = 0;
    printf("Fila de requisicoes (%d):\n", fila->tamanho);
    for (atual = fila->inicio; atual != NULL; atual = atual->proximo) {
        printf("  %d) req %ld hospital %ld %s %dx %s %s\n", posicao++,
               atual->requisicao.id, atual->requisicao.hospitalId,
               nome_prioridade(atual->requisicao.prioridade),
               atual->requisicao.quantidade,
               nome_componente(atual->requisicao.hemoComponente),
               nome_tipo(atual->requisicao.tipoSanguineo));
    }
}

void fila_destruir(FilaRequisicoes *fila) {
    NoRequisicao *atual;
    NoRequisicao *proximo;
    if (fila == NULL) {
        return;
    }
    atual = fila->inicio;
    while (atual != NULL) {
        proximo = atual->proximo;
        free(atual);
        atual = proximo;
    }
    free(fila);
}
