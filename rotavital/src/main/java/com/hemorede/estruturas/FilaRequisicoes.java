package com.hemorede.estruturas;

import com.hemorede.domain.model.Requisicao;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * REQUISIÇÕES hospitalares em fila encadeada FIFO.
 *
 * <p>Tradução de {@code aed/u1/c/fila_requisicoes.c}: mesmos ponteiros
 * {@code inicio}/{@code fim}, mesmas operações O(1) nas pontas. Na U1 a fila é
 * FIFO pura (sem FEFO nem compatibilidade ABO/Rh, reservados para a U2).</p>
 */
public class FilaRequisicoes {

    /** Nó da fila — equivale a {@code struct NoRequisicao}. */
    private static final class NoRequisicao {
        private final Requisicao requisicao;
        private NoRequisicao proximo;

        private NoRequisicao(Requisicao requisicao) {
            this.requisicao = requisicao;
        }
    }

    private NoRequisicao inicio;
    private NoRequisicao fim;
    private int tamanho;

    /** Insere no fim. Equivale a {@code fila_enfileirar}. */
    public void enfileirar(Requisicao requisicao) {
        Objects.requireNonNull(requisicao, "requisição não pode ser nula");
        NoRequisicao novo = new NoRequisicao(requisicao);
        if (fim == null) {
            inicio = novo;
        } else {
            fim.proximo = novo;
        }
        fim = novo;
        tamanho++;
    }

    /**
     * Remove e devolve a requisição do início. Equivale a {@code fila_desenfileirar}.
     *
     * @return a próxima requisição, ou {@code null} se a fila estiver vazia
     */
    public Requisicao desenfileirar() {
        if (inicio == null) {
            return null;
        }
        NoRequisicao removido = inicio;
        inicio = removido.proximo;
        if (inicio == null) {
            fim = null;
        }
        removido.proximo = null;
        tamanho--;
        return removido.requisicao;
    }

    /** Consulta o início sem remover. Equivale a {@code fila_frente}. */
    public Requisicao frente() {
        return inicio == null ? null : inicio.requisicao;
    }

    /** Consulta pelo id sem remover. Equivale a {@code fila_consultar}. */
    public Requisicao consultar(long id) {
        for (NoRequisicao atual = inicio; atual != null; atual = atual.proximo) {
            if (Objects.equals(atual.requisicao.getId(), id)) {
                return atual.requisicao;
            }
        }
        return null;
    }

    /** Posição na fila (0 = próxima), ou -1. Equivale a {@code fila_posicao}. */
    public int posicao(long id) {
        int posicao = 0;
        for (NoRequisicao atual = inicio; atual != null; atual = atual.proximo) {
            if (Objects.equals(atual.requisicao.getId(), id)) {
                return posicao;
            }
            posicao++;
        }
        return -1;
    }

    public int tamanho() {
        return tamanho;
    }

    public boolean vazia() {
        return inicio == null;
    }

    /** Cópia em ordem de atendimento, para serialização. */
    public List<Requisicao> paraLista() {
        List<Requisicao> copia = new ArrayList<>(tamanho);
        for (NoRequisicao atual = inicio; atual != null; atual = atual.proximo) {
            copia.add(atual.requisicao);
        }
        return copia;
    }
}
