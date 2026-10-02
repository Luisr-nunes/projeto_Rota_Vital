/*
 * dominio.c — Construtores e nomes legíveis dos tipos de domínio.
 */
#include "dominio.h"

Bolsa bolsa_nova(long id, HemoComponente componente, TipoSanguineo tipo,
                 Data coleta, Data validade) {
    Bolsa b;
    b.id = id;
    b.hemoComponente = componente;
    b.tipoSanguineo = tipo;
    b.dataColeta = coleta;
    b.dataValidade = validade;
    b.status = DISPONIVEL; /* mesmo default de Bolsa.java */
    return b;
}

Requisicao requisicao_nova(long id, long hospitalId, Prioridade prioridade,
                           HemoComponente componente, TipoSanguineo tipo, int quantidade) {
    Requisicao r;
    r.id = id;
    r.hospitalId = hospitalId;
    r.prioridade = prioridade;
    r.hemoComponente = componente;
    r.tipoSanguineo = tipo;
    r.quantidade = quantidade;
    return r;
}

const char *nome_componente(HemoComponente c) {
    static const char *nomes[] = {"HEMACIAS", "PLASMA", "PLAQUETAS", "CRIOPRECIPITADO"};
    return nomes[c];
}

const char *nome_tipo(TipoSanguineo t) {
    static const char *nomes[] = {"A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"};
    return nomes[t];
}

const char *nome_status(StatusBolsa s) {
    static const char *nomes[] = {"DISPONIVEL", "RESERVADA", "EM_TRANSITO", "UTILIZADA", "DESCARTADA"};
    return nomes[s];
}

const char *nome_prioridade(Prioridade p) {
    return p == URGENTE ? "URGENTE" : "NORMAL";
}
