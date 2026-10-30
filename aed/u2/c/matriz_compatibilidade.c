#include "matriz_compatibilidade.h"

#include <stddef.h>

#define ANTIGENO_A  1
#define ANTIGENO_B  2
#define ANTIGENO_RH 4

static int antigenos(TipoSanguineo t) {
    switch (t) {
        case A_POS:  return ANTIGENO_A | ANTIGENO_RH;
        case A_NEG:  return ANTIGENO_A;
        case B_POS:  return ANTIGENO_B | ANTIGENO_RH;
        case B_NEG:  return ANTIGENO_B;
        case AB_POS: return ANTIGENO_A | ANTIGENO_B | ANTIGENO_RH;
        case AB_NEG: return ANTIGENO_A | ANTIGENO_B;
        case O_POS:  return ANTIGENO_RH;
        case O_NEG:  return 0;
    }
    return 0;
}

static int tipo_valido(TipoSanguineo t) {
    return (int)t >= 0 && (int)t < TOTAL_TIPOS;
}

/* Verdadeiro se a deve vir antes de b na lista de preferencia do receptor. */
static int vem_antes(const MatrizCompatibilidade *m, TipoSanguineo receptor, TipoSanguineo a, TipoSanguineo b) {
    int a_ident = (a == receptor), b_ident = (b == receptor);
    if (a_ident != b_ident) return a_ident;                      /* 1) identico primeiro */
    if (m->receptores_atendidos[a] != m->receptores_atendidos[b])
        return m->receptores_atendidos[a] < m->receptores_atendidos[b]; /* 2) mais especifico primeiro */
    return a < b;                                                /* 3) desempate pela ordem do enum */
}

void matriz_inicializar(MatrizCompatibilidade *m) {
    for (int d = 0; d < TOTAL_TIPOS; d++) {
        m->receptores_atendidos[d] = 0;
        for (int r = 0; r < TOTAL_TIPOS; r++) {
            m->compativel[d][r] = (antigenos((TipoSanguineo)d) & ~antigenos((TipoSanguineo)r)) == 0;
            m->receptores_atendidos[d] += m->compativel[d][r];
        }
    }
    for (int r = 0; r < TOTAL_TIPOS; r++) {
        int n = 0;
        for (int d = 0; d < TOTAL_TIPOS; d++) {
            if (!m->compativel[d][r]) continue;
            /* insercao ordenada (n <= 8) */
            int i = n++;
            while (i > 0 && vem_antes(m, (TipoSanguineo)r, (TipoSanguineo)d, m->matching[r][i - 1])) {
                m->matching[r][i] = m->matching[r][i - 1];
                i--;
            }
            m->matching[r][i] = (TipoSanguineo)d;
        }
        m->qtd_matching[r] = n;
    }
}

int matriz_compativel(const MatrizCompatibilidade *m, TipoSanguineo doador, TipoSanguineo receptor) {
    if (!tipo_valido(doador) || !tipo_valido(receptor)) return 0;
    return m->compativel[doador][receptor];
}

const TipoSanguineo *matriz_matching(const MatrizCompatibilidade *m, TipoSanguineo receptor, int *qtd) {
    if (!tipo_valido(receptor)) {
        if (qtd) *qtd = 0;
        return NULL;
    }
    if (qtd) *qtd = m->qtd_matching[receptor];
    return m->matching[receptor];
}

const char *tipo_nome(TipoSanguineo t) {
    static const char *nomes[TOTAL_TIPOS] = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
    return tipo_valido(t) ? nomes[t] : "?";
}
