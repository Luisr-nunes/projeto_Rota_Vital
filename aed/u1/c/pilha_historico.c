/*
 * pilha_historico.c — Implementação da pilha encadeada do histórico.
 */
#include <stdio.h>
#include <stdlib.h>
#include "pilha_historico.h"

PilhaHistorico *pilha_criar(void) {
    PilhaHistorico *pilha = (PilhaHistorico *) malloc(sizeof(PilhaHistorico));
    if (pilha == NULL) {
        return NULL;
    }
    pilha->topo = NULL;
    pilha->tamanho = 0;
    return pilha;
}

int pilha_empilhar(PilhaHistorico *pilha, OperacaoEstoque operacao) {
    NoOperacao *novo = (NoOperacao *) malloc(sizeof(NoOperacao));
    if (novo == NULL) {
        return 0;
    }
    novo->operacao = operacao;
    novo->abaixo = pilha->topo;   /* o novo nó aponta para o antigo topo */
    pilha->topo = novo;           /* e passa a ser o topo */
    pilha->tamanho++;
    return 1;
}

int pilha_desempilhar(PilhaHistorico *pilha, OperacaoEstoque *saida) {
    NoOperacao *removido;
    if (pilha->topo == NULL) {
        return 0; /* underflow */
    }
    removido = pilha->topo;
    if (saida != NULL) {
        *saida = removido->operacao;
    }
    pilha->topo = removido->abaixo;
    free(removido);
    pilha->tamanho--;
    return 1;
}

const OperacaoEstoque *pilha_topo(const PilhaHistorico *pilha) {
    return pilha->topo == NULL ? NULL : &pilha->topo->operacao;
}

int pilha_tamanho(const PilhaHistorico *pilha) {
    return pilha->tamanho;
}

int pilha_vazia(const PilhaHistorico *pilha) {
    return pilha->topo == NULL;
}

void pilha_imprimir(const PilhaHistorico *pilha) {
    NoOperacao *atual;
    printf("Historico (%d operacoes, topo primeiro):\n", pilha->tamanho);
    for (atual = pilha->topo; atual != NULL; atual = atual->abaixo) {
        printf("  %s bolsa %ld\n",
               atual->operacao.tipo == OP_ENTRADA ? "ENTRADA" : "SAIDA  ",
               atual->operacao.bolsa.id);
    }
}

void pilha_destruir(PilhaHistorico *pilha) {
    NoOperacao *atual;
    NoOperacao *abaixo;
    if (pilha == NULL) {
        return;
    }
    atual = pilha->topo;
    while (atual != NULL) {
        abaixo = atual->abaixo;
        free(atual);
        atual = abaixo;
    }
    free(pilha);
}
