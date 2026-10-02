package com.hemorede.estruturas;

import com.hemorede.domain.model.Bolsa;

import java.util.Objects;

/**
 * Registro imutável de uma operação do estoque — equivale ao
 * {@code struct OperacaoEstoque} em C. Guarda a bolsa envolvida para
 * permitir desfazer a operação.
 */
public record OperacaoEstoque(TipoOperacao tipo, Bolsa bolsa) {

    public OperacaoEstoque {
        Objects.requireNonNull(tipo, "tipo não pode ser nulo");
        Objects.requireNonNull(bolsa, "bolsa não pode ser nula");
    }
}
