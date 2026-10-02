package com.hemorede.estruturas;

import com.hemorede.domain.model.Bolsa;

/**
 * Combina a {@link ListaEstoque} com a {@link PilhaHistorico}: toda entrada e
 * saída de bolsa é aplicada na lista e registrada na pilha, e
 * {@link #desfazer()} reverte a operação mais recente.
 *
 * <p>Tradução de {@code aed/u1/c/gestao_estoque.c}. É o ponto de entrada
 * pensado para a camada de serviço (ex.: {@code EstoqueService}) consumir.</p>
 */
public class GestaoEstoque {

    private final ListaEstoque estoque = new ListaEstoque();
    private final PilhaHistorico historico = new PilhaHistorico();

    /** Entrada de bolsa. Equivale a {@code estoque_registrar_entrada}. */
    public boolean registrarEntrada(Bolsa bolsa) {
        if (!estoque.inserir(bolsa)) {
            return false; // id duplicado: não entra no histórico
        }
        historico.empilhar(new OperacaoEstoque(TipoOperacao.ENTRADA, bolsa));
        return true;
    }

    /**
     * Saída de bolsa (envio/descarte). Equivale a {@code estoque_registrar_saida}.
     *
     * @return a bolsa que saiu, ou {@code null} se o id não estava no estoque
     */
    public Bolsa registrarSaida(long id) {
        Bolsa removida = estoque.remover(id);
        if (removida == null) {
            return null;
        }
        historico.empilhar(new OperacaoEstoque(TipoOperacao.SAIDA, removida));
        return removida;
    }

    /**
     * Desfaz a última operação. Equivale a {@code estoque_desfazer}.
     *
     * @return a operação desfeita, ou {@code null} se não há histórico
     */
    public OperacaoEstoque desfazer() {
        OperacaoEstoque op = historico.desempilhar();
        if (op == null) {
            return null;
        }
        if (op.tipo() == TipoOperacao.ENTRADA) {
            estoque.remover(op.bolsa().getId());
        } else {
            estoque.inserir(op.bolsa());
        }
        return op;
    }

    public ListaEstoque getEstoque() {
        return estoque;
    }

    public PilhaHistorico getHistorico() {
        return historico;
    }
}
