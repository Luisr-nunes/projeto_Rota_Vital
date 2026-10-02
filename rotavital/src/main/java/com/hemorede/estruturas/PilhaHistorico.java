package com.hemorede.estruturas;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * HISTÓRICO de operações do estoque em pilha encadeada (LIFO).
 *
 * <p>Tradução de {@code aed/u1/c/pilha_historico.c}: um único ponteiro
 * {@code topo}; cada nó aponta para o que está {@code abaixo}.</p>
 */
public class PilhaHistorico {

    /** Nó da pilha — equivale a {@code struct NoOperacao}. */
    private static final class NoOperacao {
        private final OperacaoEstoque operacao;
        private final NoOperacao abaixo;

        private NoOperacao(OperacaoEstoque operacao, NoOperacao abaixo) {
            this.operacao = operacao;
            this.abaixo = abaixo;
        }
    }

    private NoOperacao topo;
    private int tamanho;

    /** Empilha no topo. Equivale a {@code pilha_empilhar}. */
    public void empilhar(OperacaoEstoque operacao) {
        Objects.requireNonNull(operacao, "operação não pode ser nula");
        topo = new NoOperacao(operacao, topo);
        tamanho++;
    }

    /**
     * Retira o topo. Equivale a {@code pilha_desempilhar}.
     *
     * @return a operação do topo, ou {@code null} se a pilha estiver vazia
     */
    public OperacaoEstoque desempilhar() {
        if (topo == null) {
            return null;
        }
        NoOperacao removido = topo;
        topo = removido.abaixo;
        tamanho--;
        return removido.operacao;
    }

    /** Consulta o topo sem remover. Equivale a {@code pilha_topo}. */
    public OperacaoEstoque topo() {
        return topo == null ? null : topo.operacao;
    }

    public int tamanho() {
        return tamanho;
    }

    public boolean vazia() {
        return topo == null;
    }

    /** Cópia do topo para a base (mais recente primeiro), para serialização. */
    public List<OperacaoEstoque> paraLista() {
        List<OperacaoEstoque> copia = new ArrayList<>(tamanho);
        for (NoOperacao atual = topo; atual != null; atual = atual.abaixo) {
            copia.add(atual.operacao);
        }
        return copia;
    }
}
