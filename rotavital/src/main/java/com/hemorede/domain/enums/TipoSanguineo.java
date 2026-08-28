package com.hemorede.domain.enums;

import java.util.List;
import java.util.Map;

/**
 * Tipos sanguíneos com a matriz de compatibilidade para transfusão de
 * hemácias (regra de negócio 1: compatibilidade sanguínea).
 */
public enum TipoSanguineo {
    A_POS, A_NEG, B_POS, B_NEG, AB_POS, AB_NEG, O_POS, O_NEG;

    private static final Map<TipoSanguineo, List<TipoSanguineo>> DOADORES_COMPATIVEIS = Map.of(
            A_POS, List.of(A_POS, A_NEG, O_POS, O_NEG),
            A_NEG, List.of(A_NEG, O_NEG),
            B_POS, List.of(B_POS, B_NEG, O_POS, O_NEG),
            B_NEG, List.of(B_NEG, O_NEG),
            AB_POS, List.of(A_POS, A_NEG, B_POS, B_NEG, AB_POS, AB_NEG, O_POS, O_NEG),
            AB_NEG, List.of(A_NEG, B_NEG, AB_NEG, O_NEG),
            O_POS, List.of(O_POS, O_NEG),
            O_NEG, List.of(O_NEG)
    );

    /**
     * Verifica se uma bolsa do tipo {@code doador} pode ser transfundida
     * em um paciente do tipo {@code receptor}.
     */
    public static boolean compativel(TipoSanguineo doador, TipoSanguineo receptor) {
        return DOADORES_COMPATIVEIS.get(receptor).contains(doador);
    }
}
