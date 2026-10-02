/*
 * testes.c — Testes das estruturas da U1 (inserir, remover, consultar e casos de borda).
 *
 * Os mesmos cenários existem em Java (EstruturasU1Test.java), para que as duas
 * versões sejam validadas pelas mesmas entradas e saídas esperadas.
 *
 * Rodar:  make test        (compila com AddressSanitizer: acusa vazamento/uso após free)
 *         make valgrind    (confirma "All heap blocks were freed")
 */
#include <stdio.h>
#include "lista_estoque.h"
#include "fila_requisicoes.h"
#include "pilha_historico.h"
#include "gestao_estoque.h"
#include "arvore_bolsas.h"

static int total = 0;
static int falhas = 0;

#define CHECK(cond) do { \
        total++; \
        if (!(cond)) { falhas++; printf("  FALHOU (linha %d): %s\n", __LINE__, #cond); } \
    } while (0)

static Data data(int a, int m, int d) {
    Data x;
    x.ano = a; x.mes = m; x.dia = d;
    return x;
}

static Bolsa bolsa(long id, HemoComponente c, TipoSanguineo t) {
    return bolsa_nova(id, c, t, data(2026, 9, 1), data(2026, 10, 13));
}

/* ------------------------------------------------------------------ */

static void teste_lista(void) {
    ListaEstoque *l = lista_criar();
    Bolsa removida;
    printf("Lista (estoque)\n");

    CHECK(lista_vazia(l));
    CHECK(lista_buscar(l, 1) == NULL);
    CHECK(lista_remover(l, 1, NULL) == 0);                        /* remover de lista vazia */

    CHECK(lista_inserir(l, bolsa(10, HEMACIAS, O_NEG)) == LISTA_OK);
    CHECK(lista_inserir(l, bolsa(20, HEMACIAS, O_NEG)) == LISTA_OK);
    CHECK(lista_inserir(l, bolsa(30, PLASMA, A_POS)) == LISTA_OK);
    CHECK(lista_inserir(l, bolsa(20, PLASMA, A_POS)) == LISTA_DUPLICADA);
    CHECK(lista_tamanho(l) == 3);
    CHECK(l->inicio->bolsa.id == 10 && l->fim->bolsa.id == 30);   /* ordem de chegada */

    CHECK(lista_buscar(l, 30) != NULL && lista_buscar(l, 30)->hemoComponente == PLASMA);
    lista_buscar(l, 20)->status = RESERVADA;                      /* consulta devolve ponteiro editável */
    CHECK(lista_contar_disponiveis(l, O_NEG, HEMACIAS) == 1);     /* recursão: só a 10 está disponível */

    CHECK(lista_remover(l, 20, &removida) == 1);                  /* meio */
    CHECK(removida.id == 20 && removida.status == RESERVADA);
    CHECK(lista_remover(l, 30, NULL) == 1);                       /* fim: atualiza ponteiro fim */
    CHECK(l->fim->bolsa.id == 10);
    CHECK(lista_inserir(l, bolsa(40, PLAQUETAS, B_POS)) == LISTA_OK);
    CHECK(l->fim->bolsa.id == 40 && l->inicio->proximo == l->fim);
    CHECK(lista_remover(l, 10, NULL) == 1);                       /* início */
    CHECK(lista_remover(l, 40, NULL) == 1);
    CHECK(lista_vazia(l) && l->fim == NULL && lista_tamanho(l) == 0);

    lista_destruir(l);
}

static void teste_fila(void) {
    FilaRequisicoes *f = fila_criar();
    Requisicao r;
    printf("Fila (requisicoes)\n");

    CHECK(fila_vazia(f));
    CHECK(fila_frente(f) == NULL);
    CHECK(fila_desenfileirar(f, &r) == 0);                        /* underflow */

    CHECK(fila_enfileirar(f, requisicao_nova(1, 100, NORMAL, HEMACIAS, A_POS, 2)));
    CHECK(fila_enfileirar(f, requisicao_nova(2, 200, URGENTE, PLASMA, O_NEG, 1)));
    CHECK(fila_enfileirar(f, requisicao_nova(3, 100, NORMAL, PLAQUETAS, B_POS, 4)));
    CHECK(fila_tamanho(f) == 3);
    CHECK(fila_frente(f)->id == 1);
    CHECK(fila_consultar(f, 3) != NULL && fila_consultar(f, 3)->quantidade == 4);
    CHECK(fila_consultar(f, 99) == NULL);
    CHECK(fila_posicao(f, 3) == 2 && fila_posicao(f, 99) == -1);

    CHECK(fila_desenfileirar(f, &r) == 1 && r.id == 1);           /* FIFO: sai o primeiro */
    CHECK(fila_desenfileirar(f, &r) == 1 && r.id == 2);
    CHECK(fila_enfileirar(f, requisicao_nova(4, 300, NORMAL, HEMACIAS, AB_POS, 1)));
    CHECK(fila_desenfileirar(f, &r) == 1 && r.id == 3);
    CHECK(fila_desenfileirar(f, &r) == 1 && r.id == 4);
    CHECK(fila_vazia(f) && f->fim == NULL);                       /* fim volta a NULL ao esvaziar */
    CHECK(fila_enfileirar(f, requisicao_nova(5, 100, NORMAL, HEMACIAS, A_POS, 1)));
    CHECK(fila_frente(f)->id == 5 && f->inicio == f->fim);        /* reaproveita após esvaziar */

    fila_destruir(f);
}

static void teste_pilha(void) {
    PilhaHistorico *p = pilha_criar();
    OperacaoEstoque op;
    OperacaoEstoque saida;
    printf("Pilha (historico)\n");

    CHECK(pilha_vazia(p));
    CHECK(pilha_topo(p) == NULL);
    CHECK(pilha_desempilhar(p, &saida) == 0);                     /* underflow */

    op.tipo = OP_ENTRADA; op.bolsa = bolsa(1, HEMACIAS, A_POS);
    CHECK(pilha_empilhar(p, op));
    op.tipo = OP_SAIDA;   op.bolsa = bolsa(2, PLASMA, O_POS);
    CHECK(pilha_empilhar(p, op));
    CHECK(pilha_tamanho(p) == 2);
    CHECK(pilha_topo(p)->bolsa.id == 2);                          /* LIFO */

    CHECK(pilha_desempilhar(p, &saida) == 1 && saida.bolsa.id == 2 && saida.tipo == OP_SAIDA);
    CHECK(pilha_desempilhar(p, &saida) == 1 && saida.bolsa.id == 1 && saida.tipo == OP_ENTRADA);
    CHECK(pilha_vazia(p));

    pilha_destruir(p);
}

static void teste_gestao(void) {
    GestaoEstoque *g = gestao_criar();
    OperacaoEstoque desfeita;
    Bolsa saiu;
    printf("Gestao (lista + pilha, desfazer)\n");

    CHECK(estoque_registrar_entrada(g, bolsa(1, HEMACIAS, O_NEG)) == LISTA_OK);
    CHECK(estoque_registrar_entrada(g, bolsa(2, PLASMA, A_POS)) == LISTA_OK);
    CHECK(estoque_registrar_entrada(g, bolsa(2, PLASMA, A_POS)) == LISTA_DUPLICADA);
    CHECK(pilha_tamanho(g->historico) == 2);                      /* duplicada não entra no histórico */
    CHECK(estoque_registrar_saida(g, 1, &saiu) == 1 && saiu.id == 1);
    CHECK(estoque_registrar_saida(g, 99, NULL) == 0);
    CHECK(lista_tamanho(g->estoque) == 1 && pilha_tamanho(g->historico) == 3);

    CHECK(estoque_desfazer(g, &desfeita) == 1 && desfeita.tipo == OP_SAIDA);
    CHECK(lista_buscar(g->estoque, 1) != NULL);                   /* bolsa 1 voltou */
    CHECK(estoque_desfazer(g, &desfeita) == 1 && desfeita.tipo == OP_ENTRADA);
    CHECK(lista_buscar(g->estoque, 2) == NULL);                   /* entrada da 2 desfeita */
    CHECK(estoque_desfazer(g, NULL) == 1);
    CHECK(lista_vazia(g->estoque));
    CHECK(estoque_desfazer(g, NULL) == 0);                        /* nada mais a desfazer */

    gestao_destruir(g);
}

/* Visitante do percurso em-ordem: grava os ids num vetor. */
typedef struct {
    long ids[16];
    int n;
} ColetorIds;

static void coletar(const Bolsa *b, void *ctx) {
    ColetorIds *c = (ColetorIds *) ctx;
    c->ids[c->n++] = b->id;
}

static void teste_arvore(void) {
    ArvoreBolsas *a = arvore_criar();
    long ordem[] = {50, 30, 70, 20, 40, 60, 80};
    ColetorIds c;
    int i;
    int crescente = 1;
    printf("Arvore binaria de busca (catalogo por id)\n");

    CHECK(arvore_tamanho(a) == 0 && arvore_altura(a) == 0);
    CHECK(arvore_buscar(a, 50) == NULL);
    CHECK(arvore_remover(a, 50) == 0);

    for (i = 0; i < 7; i++) {
        CHECK(arvore_inserir(a, bolsa(ordem[i], HEMACIAS, A_POS)) == 1);
    }
    CHECK(arvore_inserir(a, bolsa(40, PLASMA, O_NEG)) == 0);     /* duplicado */
    CHECK(arvore_tamanho(a) == 7 && arvore_altura(a) == 3);
    CHECK(arvore_buscar(a, 60) != NULL && arvore_buscar(a, 65) == NULL);

    c.n = 0;
    arvore_em_ordem(a, coletar, &c);
    for (i = 1; i < c.n; i++) {
        if (c.ids[i - 1] >= c.ids[i]) crescente = 0;
    }
    CHECK(c.n == 7 && crescente && c.ids[0] == 20 && c.ids[6] == 80);

    CHECK(arvore_remover(a, 20) == 1);                            /* folha */
    CHECK(arvore_remover(a, 30) == 1);                            /* um filho (40) */
    CHECK(arvore_buscar(a, 40) != NULL);
    CHECK(arvore_remover(a, 50) == 1);                            /* raiz com dois filhos */
    CHECK(a->raiz->bolsa.id == 60);                               /* sucessor em-ordem subiu */
    CHECK(arvore_tamanho(a) == 4 && arvore_buscar(a, 50) == NULL);

    c.n = 0;
    arvore_em_ordem(a, coletar, &c);
    CHECK(c.n == 4 && c.ids[0] == 40 && c.ids[1] == 60 && c.ids[2] == 70 && c.ids[3] == 80);

    arvore_destruir(a);

    /* Pior caso: ids em ordem crescente degeneram a ABB numa "lista" (altura = n). */
    a = arvore_criar();
    for (i = 1; i <= 10; i++) {
        arvore_inserir(a, bolsa(i, HEMACIAS, A_POS));
    }
    CHECK(arvore_altura(a) == 10);
    arvore_destruir(a);
}

int main(void) {
    teste_lista();
    teste_fila();
    teste_pilha();
    teste_gestao();
    teste_arvore();
    printf("\n%d verificacoes, %d falha(s)\n", total, falhas);
    return falhas == 0 ? 0 : 1;
}
