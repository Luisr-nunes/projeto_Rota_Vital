/*
 * gestao_estoque.c — Entrada/saída de bolsas com histórico e desfazer.
 */
#include <stdlib.h>
#include "gestao_estoque.h"

GestaoEstoque *gestao_criar(void) {
    GestaoEstoque *g = (GestaoEstoque *) malloc(sizeof(GestaoEstoque));
    if (g == NULL) {
        return NULL;
    }
    g->estoque = lista_criar();
    g->historico = pilha_criar();
    if (g->estoque == NULL || g->historico == NULL) {
        /* falha parcial: libera o que foi alocado para não vazar */
        lista_destruir(g->estoque);
        pilha_destruir(g->historico);
        free(g);
        return NULL;
    }
    return g;
}

int estoque_registrar_entrada(GestaoEstoque *g, Bolsa bolsa) {
    OperacaoEstoque op;
    int resultado = lista_inserir(g->estoque, bolsa);
    if (resultado != LISTA_OK) {
        return resultado; /* duplicada ou sem memória: nada a registrar */
    }
    op.tipo = OP_ENTRADA;
    op.bolsa = bolsa;
    if (!pilha_empilhar(g->historico, op)) {
        lista_remover(g->estoque, bolsa.id, NULL); /* mantém lista e pilha coerentes */
        return LISTA_SEM_MEMORIA;
    }
    return LISTA_OK;
}

int estoque_registrar_saida(GestaoEstoque *g, long id, Bolsa *saiu) {
    OperacaoEstoque op;
    Bolsa removida;
    if (!lista_remover(g->estoque, id, &removida)) {
        return 0;
    }
    op.tipo = OP_SAIDA;
    op.bolsa = removida; /* snapshot completo: permite reinserir ao desfazer */
    if (!pilha_empilhar(g->historico, op)) {
        lista_inserir(g->estoque, removida); /* rollback */
        return 0;
    }
    if (saiu != NULL) {
        *saiu = removida;
    }
    return 1;
}

int estoque_desfazer(GestaoEstoque *g, OperacaoEstoque *desfeita) {
    OperacaoEstoque op;
    if (!pilha_desempilhar(g->historico, &op)) {
        return 0;
    }
    if (op.tipo == OP_ENTRADA) {
        lista_remover(g->estoque, op.bolsa.id, NULL);  /* inverso de entrar é sair */
    } else {
        lista_inserir(g->estoque, op.bolsa);           /* inverso de sair é voltar */
    }
    if (desfeita != NULL) {
        *desfeita = op;
    }
    return 1;
}

void gestao_destruir(GestaoEstoque *g) {
    if (g == NULL) {
        return;
    }
    lista_destruir(g->estoque);
    pilha_destruir(g->historico);
    free(g);
}
