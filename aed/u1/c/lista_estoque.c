/*
 * lista_estoque.c — Implementação da lista encadeada do estoque.
 */
#include <stdio.h>
#include <stdlib.h>
#include "lista_estoque.h"

ListaEstoque *lista_criar(void) {
    ListaEstoque *lista = (ListaEstoque *) malloc(sizeof(ListaEstoque));
    if (lista == NULL) {
        return NULL;
    }
    lista->inicio = NULL;
    lista->fim = NULL;
    lista->tamanho = 0;
    return lista;
}

int lista_inserir(ListaEstoque *lista, Bolsa bolsa) {
    NoBolsa *novo;

    /* 1) Regra de domínio: o id da bolsa é único no estoque. */
    if (lista_buscar(lista, bolsa.id) != NULL) {
        return LISTA_DUPLICADA;
    }

    /* 2) Aloca o nó no heap e copia a bolsa para dentro dele. */
    novo = (NoBolsa *) malloc(sizeof(NoBolsa));
    if (novo == NULL) {
        return LISTA_SEM_MEMORIA;
    }
    novo->bolsa = bolsa;
    novo->proximo = NULL;

    /* 3) Encadeia no fim: lista vazia -> inicio e fim apontam para o novo. */
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

    /* Percorre mantendo dois ponteiros: o nó atual e o que veio antes dele. */
    while (atual != NULL && atual->bolsa.id != id) {
        anterior = atual;
        atual = atual->proximo;
    }
    if (atual == NULL) {
        return 0; /* não encontrou */
    }

    /* Religa os ponteiros "pulando" o nó removido. */
    if (anterior == NULL) {
        lista->inicio = atual->proximo;  /* removendo o primeiro */
    } else {
        anterior->proximo = atual->proximo;
    }
    if (atual == lista->fim) {
        lista->fim = anterior;           /* removendo o último */
    }

    if (removida != NULL) {
        *removida = atual->bolsa;        /* copia ANTES do free */
    }
    free(atual);
    lista->tamanho--;
    return 1;
}

Bolsa *lista_buscar(const ListaEstoque *lista, long id) {
    NoBolsa *atual;
    for (atual = lista->inicio; atual != NULL; atual = atual->proximo) {
        if (atual->bolsa.id == id) {
            return &atual->bolsa;
        }
    }
    return NULL;
}

int lista_tamanho(const ListaEstoque *lista) {
    return lista->tamanho;
}

int lista_vazia(const ListaEstoque *lista) {
    return lista->inicio == NULL;
}

int lista_contar_disponiveis_rec(const NoBolsa *no, TipoSanguineo tipo, HemoComponente componente) {
    int casa;
    if (no == NULL) {
        return 0; /* caso base: fim da lista */
    }
    casa = (no->bolsa.tipoSanguineo == tipo
            && no->bolsa.hemoComponente == componente
            && no->bolsa.status == DISPONIVEL) ? 1 : 0;
    return casa + lista_contar_disponiveis_rec(no->proximo, tipo, componente);
}

int lista_contar_disponiveis(const ListaEstoque *lista, TipoSanguineo tipo, HemoComponente componente) {
    return lista_contar_disponiveis_rec(lista->inicio, tipo, componente);
}

void lista_imprimir(const ListaEstoque *lista) {
    NoBolsa *atual;
    printf("Estoque (%d bolsas):\n", lista->tamanho);
    for (atual = lista->inicio; atual != NULL; atual = atual->proximo) {
        printf("  [%ld] %-15s %-3s validade %04d-%02d-%02d %s\n",
               atual->bolsa.id,
               nome_componente(atual->bolsa.hemoComponente),
               nome_tipo(atual->bolsa.tipoSanguineo),
               atual->bolsa.dataValidade.ano, atual->bolsa.dataValidade.mes,
               atual->bolsa.dataValidade.dia,
               nome_status(atual->bolsa.status));
    }
}

void lista_destruir(ListaEstoque *lista) {
    NoBolsa *atual;
    NoBolsa *proximo;
    if (lista == NULL) {
        return;
    }
    /* Guarda o próximo ANTES de liberar o atual (depois do free não se pode ler atual->proximo). */
    atual = lista->inicio;
    while (atual != NULL) {
        proximo = atual->proximo;
        free(atual);
        atual = proximo;
    }
    free(lista);
}
