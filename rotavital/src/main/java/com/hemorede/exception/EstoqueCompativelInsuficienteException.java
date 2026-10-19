package com.hemorede.exception;

public class EstoqueCompativelInsuficienteException extends RuntimeException {

    private final int quantidadeNecessaria;
    private final int quantidadeDisponivel;

    public EstoqueCompativelInsuficienteException(int quantidadeNecessaria, int quantidadeDisponivel) {
        super("Estoque compativel insuficiente. Necessario: " + quantidadeNecessaria
                + ", disponivel: " + quantidadeDisponivel);
        this.quantidadeNecessaria = quantidadeNecessaria;
        this.quantidadeDisponivel = quantidadeDisponivel;
    }

    public int getQuantidadeNecessaria() {
        return quantidadeNecessaria;
    }

    public int getQuantidadeDisponivel() {
        return quantidadeDisponivel;
    }
}
