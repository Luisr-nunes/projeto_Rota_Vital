/*
 * dominio.h — Tipos de domínio do Rota Vital usados pelas estruturas da U1 (AED).
 *
 * Espelha as entidades/enums Java de com.hemorede.domain:
 *   - HemoComponente, TipoSanguineo, StatusBolsa, Prioridade  -> enums Java de mesmo nome
 *   - Bolsa                                                    -> domain/model/Bolsa.java
 *   - Requisicao (cabeçalho + 1 item)                          -> Requisicao.java + ItemRequisicao.java
 *
 * Os registros são guardados POR VALOR dentro dos nós das estruturas
 * (cada nó alocado com malloc carrega sua própria cópia do struct).
 */
#ifndef DOMINIO_H
#define DOMINIO_H

typedef enum { HEMACIAS, PLASMA, PLAQUETAS, CRIOPRECIPITADO } HemoComponente;

typedef enum { A_POS, A_NEG, B_POS, B_NEG, AB_POS, AB_NEG, O_POS, O_NEG } TipoSanguineo;

typedef enum { DISPONIVEL, RESERVADA, EM_TRANSITO, UTILIZADA, DESCARTADA } StatusBolsa;

typedef enum { URGENTE, NORMAL } Prioridade;

/* Data simples (Java usa java.time.LocalDate). */
typedef struct {
    int ano;
    int mes;
    int dia;
} Data;

/* Bolsa de hemocomponente — chave de identificação: id. */
typedef struct {
    long id;
    HemoComponente hemoComponente;
    TipoSanguineo tipoSanguineo;
    Data dataColeta;
    Data dataValidade;
    StatusBolsa status;
} Bolsa;

/* Requisição hospitalar (versão U1: um item por requisição). */
typedef struct {
    long id;
    long hospitalId;
    Prioridade prioridade;
    HemoComponente hemoComponente;
    TipoSanguineo tipoSanguineo;
    int quantidade;
} Requisicao;

/* Construtores de conveniência (devolvem o struct por valor). */
Bolsa bolsa_nova(long id, HemoComponente componente, TipoSanguineo tipo,
                 Data coleta, Data validade);
Requisicao requisicao_nova(long id, long hospitalId, Prioridade prioridade,
                           HemoComponente componente, TipoSanguineo tipo, int quantidade);

/* Nomes legíveis para impressão. */
const char *nome_componente(HemoComponente c);
const char *nome_tipo(TipoSanguineo t);
const char *nome_status(StatusBolsa s);
const char *nome_prioridade(Prioridade p);

#endif
