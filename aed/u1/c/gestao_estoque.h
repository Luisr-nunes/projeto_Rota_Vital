/*
 * gestao_estoque.h — Combina a LISTA do estoque com a PILHA de histórico.
 *
 * Toda entrada/saída de bolsa passa por aqui: a operação é aplicada na lista
 * e registrada na pilha. `estoque_desfazer` desempilha a última operação e
 * aplica a operação inversa na lista (ENTRADA -> remove; SAIDA -> reinsere).
 *
 * Equivalente Java: com.hemorede.estruturas.GestaoEstoque
 */
#ifndef GESTAO_ESTOQUE_H
#define GESTAO_ESTOQUE_H

#include "lista_estoque.h"
#include "pilha_historico.h"

typedef struct {
    ListaEstoque *estoque;
    PilhaHistorico *historico;
} GestaoEstoque;

/* Retorna NULL se faltar memória para qualquer uma das partes. */
GestaoEstoque *gestao_criar(void);

/* Entrada de bolsa no estoque. Retorna os mesmos códigos de lista_inserir. */
int estoque_registrar_entrada(GestaoEstoque *g, Bolsa bolsa);

/* Saída de bolsa (envio/descarte). Retorna 1 se saiu, 0 se o id não existe. */
int estoque_registrar_saida(GestaoEstoque *g, long id, Bolsa *saiu);

/* Desfaz a última operação. Retorna 1 se desfez, 0 se não há histórico. */
int estoque_desfazer(GestaoEstoque *g, OperacaoEstoque *desfeita);

void gestao_destruir(GestaoEstoque *g);

#endif
