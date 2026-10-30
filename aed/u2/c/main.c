/*
 * main.c — testes e demonstracao da matriz ABO/Rh 8x8 em C.
 * Espelha MatrizCompatibilidadeTest.java: 64 pares contra a tabela de referencia
 * (doc/aed-u2-compatibilidade.md, secao 2.3), ordem do matching e casos de borda.
 * Saida 0 = tudo passou; diferente de 0 = alguma verificacao falhou.
 */
#include <stdio.h>
#include <string.h>

#include "matriz_compatibilidade.h"

static int falhas = 0;
static int verificacoes = 0;

#define CHECK(cond, ...)                                  \
    do {                                                  \
        verificacoes++;                                   \
        if (!(cond)) {                                    \
            falhas++;                                     \
            printf("  FALHOU: ");                         \
            printf(__VA_ARGS__);                          \
            printf("\n");                                 \
        }                                                 \
    } while (0)

/* Tabela de referencia escrita a mao: esperado[receptor][doador]. */
static const int ESPERADO[TOTAL_TIPOS][TOTAL_TIPOS] = {
    /* doador:    A+ A- B+ B- AB+ AB- O+ O- */
    /* A+  */ {   1, 1, 0, 0, 0,  0,  1, 1 },
    /* A-  */ {   0, 1, 0, 0, 0,  0,  0, 1 },
    /* B+  */ {   0, 0, 1, 1, 0,  0,  1, 1 },
    /* B-  */ {   0, 0, 0, 1, 0,  0,  0, 1 },
    /* AB+ */ {   1, 1, 1, 1, 1,  1,  1, 1 },
    /* AB- */ {   0, 1, 0, 1, 0,  1,  0, 1 },
    /* O+  */ {   0, 0, 0, 0, 0,  0,  1, 1 },
    /* O-  */ {   0, 0, 0, 0, 0,  0,  0, 1 },
};

/* Ordem exata esperada do matching por receptor (terminada por -1). */
static const int ORDEM[TOTAL_TIPOS][TOTAL_TIPOS + 1] = {
    /* A+  */ { A_POS, A_NEG, O_POS, O_NEG, -1 },
    /* A-  */ { A_NEG, O_NEG, -1 },
    /* B+  */ { B_POS, B_NEG, O_POS, O_NEG, -1 },
    /* B-  */ { B_NEG, O_NEG, -1 },
    /* AB+ */ { AB_POS, A_POS, B_POS, AB_NEG, A_NEG, B_NEG, O_POS, O_NEG, -1 },
    /* AB- */ { AB_NEG, A_NEG, B_NEG, O_NEG, -1 },
    /* O+  */ { O_POS, O_NEG, -1 },
    /* O-  */ { O_NEG, -1 },
};

static void testar_64_pares(const MatrizCompatibilidade *m) {
    printf("[1] 8 receptores x 8 doadores (64 pares)\n");
    int compativeis = 0;
    for (int r = 0; r < TOTAL_TIPOS; r++) {
        for (int d = 0; d < TOTAL_TIPOS; d++) {
            int obtido = matriz_compativel(m, (TipoSanguineo)d, (TipoSanguineo)r);
            CHECK(obtido == ESPERADO[r][d], "doador %s -> receptor %s: esperado %d, obtido %d",
                  tipo_nome((TipoSanguineo)d), tipo_nome((TipoSanguineo)r), ESPERADO[r][d], obtido);
            compativeis += obtido;
        }
    }
    CHECK(compativeis == 27, "esperados 27 pares compativeis, obtidos %d", compativeis);
    for (int t = 0; t < TOTAL_TIPOS; t++) {
        CHECK(matriz_compativel(m, (TipoSanguineo)t, (TipoSanguineo)t), "%s deve ser compativel com ele mesmo", tipo_nome((TipoSanguineo)t));
        CHECK(matriz_compativel(m, O_NEG, (TipoSanguineo)t), "O- deve doar para %s", tipo_nome((TipoSanguineo)t));
        CHECK(matriz_compativel(m, (TipoSanguineo)t, AB_POS), "%s deve doar para AB+", tipo_nome((TipoSanguineo)t));
    }
    CHECK(!matriz_compativel(m, A_POS, A_NEG), "Rh+ nao doa para Rh-");
    CHECK(!matriz_compativel(m, AB_POS, O_POS), "AB+ nao doa para O+");
    CHECK(!matriz_compativel(m, (TipoSanguineo)-1, A_POS), "doador invalido e incompativel");
    CHECK(!matriz_compativel(m, A_POS, (TipoSanguineo)TOTAL_TIPOS), "receptor invalido e incompativel");
}

static void testar_matching(const MatrizCompatibilidade *m) {
    printf("[2] matching: doadores compativeis em ordem de preferencia\n");
    for (int r = 0; r < TOTAL_TIPOS; r++) {
        int qtd = -1;
        const TipoSanguineo *lista = matriz_matching(m, (TipoSanguineo)r, &qtd);
        int esperado_qtd = 0;
        while (ORDEM[r][esperado_qtd] != -1) esperado_qtd++;

        CHECK(lista != NULL && qtd == esperado_qtd, "receptor %s: esperados %d doadores, obtidos %d",
              tipo_nome((TipoSanguineo)r), esperado_qtd, qtd);
        if (lista == NULL || qtd != esperado_qtd) continue;

        for (int i = 0; i < qtd; i++) {
            CHECK((int)lista[i] == ORDEM[r][i], "receptor %s, posicao %d: esperado %s, obtido %s",
                  tipo_nome((TipoSanguineo)r), i, tipo_nome((TipoSanguineo)ORDEM[r][i]), tipo_nome(lista[i]));
        }
        CHECK(lista[0] == (TipoSanguineo)r, "receptor %s: o tipo identico deve vir primeiro", tipo_nome((TipoSanguineo)r));
        CHECK(lista[qtd - 1] == O_NEG, "receptor %s: O- deve vir por ultimo", tipo_nome((TipoSanguineo)r));
    }
    int qtd = 99;
    CHECK(matriz_matching(m, (TipoSanguineo)-1, &qtd) == NULL && qtd == 0, "receptor invalido devolve NULL e qtd 0");
}

static void demonstrar(const MatrizCompatibilidade *m) {
    printf("\nMatching (receptor <- doadores em ordem de preferencia)\n");
    for (int r = 0; r < TOTAL_TIPOS; r++) {
        int qtd;
        const TipoSanguineo *lista = matriz_matching(m, (TipoSanguineo)r, &qtd);
        printf("  %-3s <-", tipo_nome((TipoSanguineo)r));
        for (int i = 0; i < qtd; i++) printf(" %s", tipo_nome(lista[i]));
        printf("\n");
    }
}

int main(void) {
    MatrizCompatibilidade m;
    matriz_inicializar(&m);

    testar_64_pares(&m);
    testar_matching(&m);
    demonstrar(&m);

    printf("\n%d verificacoes, %d falha(s)\n", verificacoes, falhas);
    printf(falhas == 0 ? "OK\n" : "FALHOU\n");
    return falhas == 0 ? 0 : 1;
}
