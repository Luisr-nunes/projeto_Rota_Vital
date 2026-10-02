/*
 * pilha_historico.h — HISTÓRICO de operações do estoque em pilha encadeada (LIFO).
 *
 * Por que pilha? O histórico serve para auditoria e para DESFAZER a última
 * operação lançada por engano (entrada ou saída de bolsa). Desfazer sempre
 * age sobre a operação mais recente: último a entrar, primeiro a sair.
 * Empilhar e desempilhar no topo são O(1).
 *
 * Equivalente Java: com.hemorede.estruturas.PilhaHistorico (+ OperacaoEstoque, TipoOperacao)
 */
#ifndef PILHA_HISTORICO_H
#define PILHA_HISTORICO_H

#include "dominio.h"

typedef enum { OP_ENTRADA, OP_SAIDA } TipoOperacao;

/* Guarda uma cópia da bolsa no momento da operação (snapshot),
 * o que permite reconstruí-la ao desfazer uma SAIDA. */
typedef struct {
    TipoOperacao tipo;
    Bolsa bolsa;
} OperacaoEstoque;

typedef struct NoOperacao {
    OperacaoEstoque operacao;
    struct NoOperacao *abaixo;  /* nó imediatamente abaixo do topo */
} NoOperacao;

typedef struct {
    NoOperacao *topo;  /* NULL se vazia */
    int tamanho;
} PilhaHistorico;

PilhaHistorico *pilha_criar(void);

/* Retorna 1 se ok, 0 se faltou memória. O(1). */
int pilha_empilhar(PilhaHistorico *pilha, OperacaoEstoque operacao);

/* Retira o topo copiando em *saida. Retorna 1 se ok, 0 se vazia. O(1). */
int pilha_desempilhar(PilhaHistorico *pilha, OperacaoEstoque *saida);

/* Consulta o topo sem remover (NULL se vazia). O(1). */
const OperacaoEstoque *pilha_topo(const PilhaHistorico *pilha);

int pilha_tamanho(const PilhaHistorico *pilha);
int pilha_vazia(const PilhaHistorico *pilha);

void pilha_imprimir(const PilhaHistorico *pilha);
void pilha_destruir(PilhaHistorico *pilha);

#endif
