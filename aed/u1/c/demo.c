/*
 * demo.c — Cenário de uso das estruturas da U1 com dados sintéticos do Rota Vital.
 *
 * Hemocentro recebe doações (lista + histórico), registra uma saída por engano
 * e desfaz (pilha), hospitais fazem requisições (fila) e o catálogo por id (ABB)
 * é consultado e listado em ordem.
 */
#include <stdio.h>
#include "gestao_estoque.h"
#include "fila_requisicoes.h"
#include "arvore_bolsas.h"

static Data data(int a, int m, int d) {
    Data x;
    x.ano = a; x.mes = m; x.dia = d;
    return x;
}

static void imprimir_bolsa(const Bolsa *b, void *ctx) {
    (void) ctx;
    printf("  [%ld] %s %s\n", b->id, nome_componente(b->hemoComponente), nome_tipo(b->tipoSanguineo));
}

int main(void) {
    GestaoEstoque *g = gestao_criar();
    FilaRequisicoes *fila = fila_criar();
    ArvoreBolsas *catalogo = arvore_criar();
    Bolsa doacoes[5];
    Requisicao atendida;
    OperacaoEstoque desfeita;
    int i;

    if (g == NULL || fila == NULL || catalogo == NULL) {
        printf("Sem memoria\n");
        gestao_destruir(g);
        fila_destruir(fila);
        arvore_destruir(catalogo);
        return 1;
    }

    doacoes[0] = bolsa_nova(1004, HEMACIAS, O_NEG, data(2026, 9, 20), data(2026, 11, 1));
    doacoes[1] = bolsa_nova(1002, PLASMA, A_POS, data(2026, 9, 21), data(2027, 9, 21));
    doacoes[2] = bolsa_nova(1005, PLAQUETAS, B_POS, data(2026, 9, 28), data(2026, 10, 3));
    doacoes[3] = bolsa_nova(1001, HEMACIAS, A_POS, data(2026, 9, 22), data(2026, 11, 3));
    doacoes[4] = bolsa_nova(1003, HEMACIAS, O_NEG, data(2026, 9, 25), data(2026, 11, 6));

    printf("== Entrada de doacoes ==\n");
    for (i = 0; i < 5; i++) {
        estoque_registrar_entrada(g, doacoes[i]);
        arvore_inserir(catalogo, doacoes[i]);
    }
    lista_imprimir(g->estoque);
    printf("Hemacias O- disponiveis (contagem recursiva): %d\n\n",
           lista_contar_disponiveis(g->estoque, O_NEG, HEMACIAS));

    printf("== Saida registrada por engano e desfeita ==\n");
    estoque_registrar_saida(g, 1002, NULL);
    pilha_imprimir(g->historico);
    estoque_desfazer(g, &desfeita);
    printf("Desfeito: %s da bolsa %ld -> estoque com %d bolsas\n\n",
           desfeita.tipo == OP_SAIDA ? "SAIDA" : "ENTRADA", desfeita.bolsa.id,
           lista_tamanho(g->estoque));

    printf("== Requisicoes hospitalares (FIFO) ==\n");
    fila_enfileirar(fila, requisicao_nova(1, 10, NORMAL, HEMACIAS, A_POS, 2));
    fila_enfileirar(fila, requisicao_nova(2, 20, URGENTE, PLASMA, AB_POS, 1));
    fila_enfileirar(fila, requisicao_nova(3, 10, NORMAL, PLAQUETAS, B_POS, 1));
    fila_imprimir(fila);
    fila_desenfileirar(fila, &atendida);
    printf("Proxima atendida: req %ld (hospital %ld). Restam %d.\n\n",
           atendida.id, atendida.hospitalId, fila_tamanho(fila));

    printf("== Catalogo ABB por id (em-ordem) ==\n");
    arvore_em_ordem(catalogo, imprimir_bolsa, NULL);
    printf("Altura da arvore: %d | busca 1003: %s\n",
           arvore_altura(catalogo), arvore_buscar(catalogo, 1003) ? "encontrada" : "ausente");

    gestao_destruir(g);
    fila_destruir(fila);
    arvore_destruir(catalogo);
    return 0;
}
