/*
 * arvore_bolsas.c — ABB de bolsas por id, implementada com recursão.
 *
 * Padrão usado em inserir/remover: a função recursiva recebe a raiz de uma
 * subárvore e DEVOLVE a nova raiz dessa subárvore. O chamador religa o
 * ponteiro (no->esquerda = inserir_rec(no->esquerda, ...)). Assim não é
 * preciso ponteiro-para-ponteiro nem tratar a raiz como caso especial.
 */
#include <stdlib.h>
#include "arvore_bolsas.h"

ArvoreBolsas *arvore_criar(void) {
    ArvoreBolsas *arvore = (ArvoreBolsas *) malloc(sizeof(ArvoreBolsas));
    if (arvore == NULL) {
        return NULL;
    }
    arvore->raiz = NULL;
    arvore->tamanho = 0;
    return arvore;
}

/* ---------- inserir ---------- */

static NoArvore *inserir_rec(NoArvore *no, Bolsa bolsa, int *resultado) {
    if (no == NULL) {                       /* caso base: posição vazia encontrada */
        NoArvore *novo = (NoArvore *) malloc(sizeof(NoArvore));
        if (novo == NULL) {
            *resultado = -1;
            return NULL;
        }
        novo->bolsa = bolsa;
        novo->esquerda = NULL;
        novo->direita = NULL;
        *resultado = 1;
        return novo;
    }
    if (bolsa.id < no->bolsa.id) {
        no->esquerda = inserir_rec(no->esquerda, bolsa, resultado);
    } else if (bolsa.id > no->bolsa.id) {
        no->direita = inserir_rec(no->direita, bolsa, resultado);
    } else {
        *resultado = 0;                     /* id duplicado: não insere */
    }
    return no;
}

int arvore_inserir(ArvoreBolsas *arvore, Bolsa bolsa) {
    int resultado = 0;
    /* Se o malloc falhar, inserir_rec devolve NULL no ponto vazio onde o nó
     * entraria (que já era NULL), então a árvore continua intacta. */
    arvore->raiz = inserir_rec(arvore->raiz, bolsa, &resultado);
    if (resultado == 1) {
        arvore->tamanho++;
    }
    return resultado;
}

/* ---------- buscar ---------- */

static NoArvore *buscar_rec(NoArvore *no, long id) {
    if (no == NULL || no->bolsa.id == id) {
        return no;                          /* caso base: achou ou não existe */
    }
    if (id < no->bolsa.id) {
        return buscar_rec(no->esquerda, id);
    }
    return buscar_rec(no->direita, id);
}

Bolsa *arvore_buscar(const ArvoreBolsas *arvore, long id) {
    NoArvore *no = buscar_rec(arvore->raiz, id);
    return no == NULL ? NULL : &no->bolsa;
}

/* ---------- remover ---------- */

static NoArvore *menor_no(NoArvore *no) {
    while (no->esquerda != NULL) {
        no = no->esquerda;
    }
    return no;
}

static NoArvore *remover_rec(NoArvore *no, long id, int *removeu) {
    if (no == NULL) {
        return NULL;                        /* id não está na árvore */
    }
    if (id < no->bolsa.id) {
        no->esquerda = remover_rec(no->esquerda, id, removeu);
    } else if (id > no->bolsa.id) {
        no->direita = remover_rec(no->direita, id, removeu);
    } else {
        NoArvore *filho;
        /* Casos 1 e 2: zero ou um filho — o filho (ou NULL) sobe no lugar do nó. */
        if (no->esquerda == NULL || no->direita == NULL) {
            filho = no->esquerda != NULL ? no->esquerda : no->direita;
            free(no);
            *removeu = 1;
            return filho;
        }
        /* Caso 3: dois filhos — copia o sucessor em-ordem (menor da direita)
         * para este nó e remove o sucessor da subárvore direita. */
        filho = menor_no(no->direita);
        no->bolsa = filho->bolsa;
        no->direita = remover_rec(no->direita, filho->bolsa.id, removeu);
    }
    return no;
}

int arvore_remover(ArvoreBolsas *arvore, long id) {
    int removeu = 0;
    arvore->raiz = remover_rec(arvore->raiz, id, &removeu);
    if (removeu) {
        arvore->tamanho--;
    }
    return removeu;
}

/* ---------- percurso, altura, destruição ---------- */

static void em_ordem_rec(const NoArvore *no, VisitaBolsa visitar, void *contexto) {
    if (no == NULL) {
        return;
    }
    em_ordem_rec(no->esquerda, visitar, contexto);
    visitar(&no->bolsa, contexto);
    em_ordem_rec(no->direita, visitar, contexto);
}

void arvore_em_ordem(const ArvoreBolsas *arvore, VisitaBolsa visitar, void *contexto) {
    em_ordem_rec(arvore->raiz, visitar, contexto);
}

static int altura_rec(const NoArvore *no) {
    int he;
    int hd;
    if (no == NULL) {
        return 0;
    }
    he = altura_rec(no->esquerda);
    hd = altura_rec(no->direita);
    return 1 + (he > hd ? he : hd);
}

int arvore_altura(const ArvoreBolsas *arvore) {
    return altura_rec(arvore->raiz);
}

int arvore_tamanho(const ArvoreBolsas *arvore) {
    return arvore->tamanho;
}

static void destruir_rec(NoArvore *no) {
    if (no == NULL) {
        return;
    }
    destruir_rec(no->esquerda);   /* pós-ordem: filhos antes do pai, */
    destruir_rec(no->direita);    /* senão perderíamos os ponteiros para eles */
    free(no);
}

void arvore_destruir(ArvoreBolsas *arvore) {
    if (arvore == NULL) {
        return;
    }
    destruir_rec(arvore->raiz);
    free(arvore);
}
