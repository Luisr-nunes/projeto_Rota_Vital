package com.hemorede.domain.enums;

/**
 * Tipos de hemocomponente, com metadados usados nas regras de validade
 * e de transporte (refrigeração).
 */
public enum HemoComponente {

    HEMACIAS(42, TipoRefrigeracao.REFRIGERADO_2_6),
    PLASMA(365, TipoRefrigeracao.CONGELADO_MENOS_20),
    PLAQUETAS(5, TipoRefrigeracao.AMBIENTE_CONTROLADO_20_24),
    CRIOPRECIPITADO(365, TipoRefrigeracao.CONGELADO_MENOS_20);

    private final int validadeEmDias;
    private final TipoRefrigeracao refrigeracaoExigida;

    HemoComponente(int validadeEmDias, TipoRefrigeracao refrigeracaoExigida) {
        this.validadeEmDias = validadeEmDias;
        this.refrigeracaoExigida = refrigeracaoExigida;
    }

    public int getValidadeEmDias() {
        return validadeEmDias;
    }

    public TipoRefrigeracao getRefrigeracaoExigida() {
        return refrigeracaoExigida;
    }
}
