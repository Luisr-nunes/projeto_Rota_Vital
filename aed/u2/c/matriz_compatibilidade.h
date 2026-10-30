/*
 * matriz_compatibilidade.h — Matriz de compatibilidade ABO/Rh 8x8 (doador x receptor) da U2 (AED).
 *
 * Espelha com.hemorede.algoritmos.MatrizCompatibilidade (Java). A ordem do enum e a mesma de
 * TipoSanguineo.java e de aed/u1/c/dominio.h, para o indice ser intercambiavel.
 *
 * Regra: cada tipo e um conjunto de antigenos (A, B, Rh); o doador so e compativel quando
 * antigenos(doador) esta contido em antigenos(receptor).
 *
 * Complexidade: matriz_compativel() e matriz_matching() sao O(1) (a tabela e montada uma unica vez
 * em matriz_inicializar(), O(k^2) com k = 8 fixo).
 */
#ifndef MATRIZ_COMPATIBILIDADE_H
#define MATRIZ_COMPATIBILIDADE_H

typedef enum { A_POS, A_NEG, B_POS, B_NEG, AB_POS, AB_NEG, O_POS, O_NEG } TipoSanguineo;

#define TOTAL_TIPOS 8

typedef struct {
    int compativel[TOTAL_TIPOS][TOTAL_TIPOS];     /* [doador][receptor], 1 = compativel */
    int receptores_atendidos[TOTAL_TIPOS];        /* soma da coluna de cada doador */
    TipoSanguineo matching[TOTAL_TIPOS][TOTAL_TIPOS]; /* [receptor] -> doadores em ordem de preferencia */
    int qtd_matching[TOTAL_TIPOS];                /* quantos doadores validos em matching[receptor] */
} MatrizCompatibilidade;

/* Monta a matriz e as listas ordenadas. Chamar uma vez antes das consultas. */
void matriz_inicializar(MatrizCompatibilidade *m);

/* 1 se uma bolsa do tipo doador pode ser transfundida no receptor; 0 caso contrario (ou tipo invalido). */
int matriz_compativel(const MatrizCompatibilidade *m, TipoSanguineo doador, TipoSanguineo receptor);

/*
 * Doadores compativeis com o receptor em ordem de preferencia: tipo identico primeiro, depois do
 * mais especifico ao mais universal, O- por ultimo. Devolve um ponteiro para a lista interna
 * (somente leitura) e grava a quantidade em *qtd. Receptor invalido: devolve NULL e *qtd = 0.
 */
const TipoSanguineo *matriz_matching(const MatrizCompatibilidade *m, TipoSanguineo receptor, int *qtd);

/* Nome legivel ("A+", "O-", ...). */
const char *tipo_nome(TipoSanguineo t);

#endif
