package com.hemorede.algoritmos;

import com.hemorede.domain.enums.TipoSanguineo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Matriz de compatibilidade ABO/Rh 8x8 (doador x receptor) para concentrado de hemacias.
 *
 * <p>A matriz e montada uma unica vez, no construtor, a partir da regra biologica: cada tipo e
 * representado pelo conjunto de antigenos de superficie das hemacias (A, B e D/Rh) e um doador so
 * e compativel com um receptor quando todos os antigenos do doador ja existem no receptor, isto
 * e, {@code antigenos(doador) ⊆ antigenos(receptor)}. Isso cobre ao mesmo tempo ABO
 * (O nao tem antigenos, AB tem os dois) e Rh (Rh+ nao doa para Rh-).</p>
 *
 * <p>Depois de montada, a consulta {@link #compativel} e um acesso direto a
 * {@code matriz[doador.ordinal()][receptor.ordinal()]}: O(1) no tempo e no espaco
 * (64 posicoes). A classe e imutavel e thread-safe.</p>
 *
 * <p><b>Matching ordenado.</b> Alem da consulta pontual, a matriz guarda, para cada receptor, a lista
 * de doadores compativeis em ordem de preferencia, calculada uma unica vez: (1) o tipo identico ao
 * do receptor primeiro; (2) depois os demais, dos mais especificos para os mais universais, isto e,
 * pelo numero de receptores que cada doador atende (soma da coluna da matriz), em ordem crescente;
 * (3) empate desfeito pela ordem do enum, para o resultado ser deterministico. O O-, que atende
 * os 8 receptores, e sempre o ultimo: preserva-se o doador universal para quem so ele serve.
 * {@link #matching} e O(1) (devolve a lista pronta, de no maximo 8 elementos).</p>
 */
public final class MatrizCompatibilidade {

    private static final int TOTAL_TIPOS = TipoSanguineo.values().length;

    private static final int ANTIGENO_A = 1;
    private static final int ANTIGENO_B = 2;
    private static final int ANTIGENO_RH = 4;

    /** {@code matriz[doador][receptor]} indexada por {@link TipoSanguineo#ordinal()}. */
    private final boolean[][] matriz = new boolean[TOTAL_TIPOS][TOTAL_TIPOS];

    /** Receptores atendidos por cada doador (soma da coluna do doador). */
    private final int[] receptoresAtendidos = new int[TOTAL_TIPOS];

    /** Doadores compativeis de cada receptor, ja em ordem de preferencia (imutaveis). */
    private final List<List<TipoSanguineo>> matchingPorReceptor = new ArrayList<>(TOTAL_TIPOS);

    public MatrizCompatibilidade() {
        for (TipoSanguineo doador : TipoSanguineo.values()) {
            for (TipoSanguineo receptor : TipoSanguineo.values()) {
                int antigenosDoador = antigenos(doador);
                matriz[doador.ordinal()][receptor.ordinal()] =
                        (antigenosDoador & ~antigenos(receptor)) == 0;
                if (matriz[doador.ordinal()][receptor.ordinal()]) {
                    receptoresAtendidos[doador.ordinal()]++;
                }
            }
        }
        for (TipoSanguineo receptor : TipoSanguineo.values()) {
            matchingPorReceptor.add(ordenarDoadores(receptor));
        }
    }

    private List<TipoSanguineo> ordenarDoadores(TipoSanguineo receptor) {
        List<TipoSanguineo> doadores = new ArrayList<>(TOTAL_TIPOS);
        for (TipoSanguineo doador : TipoSanguineo.values()) {
            if (matriz[doador.ordinal()][receptor.ordinal()]) {
                doadores.add(doador);
            }
        }
        doadores.sort(Comparator
                .comparing((TipoSanguineo doador) -> doador != receptor)
                .thenComparingInt(doador -> receptoresAtendidos[doador.ordinal()])
                .thenComparingInt(TipoSanguineo::ordinal));
        return List.copyOf(doadores);
    }

    /**
     * Informa se uma bolsa do tipo {@code doador} pode ser transfundida em um paciente
     * {@code receptor}. Consulta O(1). Tipo nulo e tratado como incompativel.
     */
    public boolean compativel(TipoSanguineo doador, TipoSanguineo receptor) {
        if (doador == null || receptor == null) {
            return false;
        }
        return matriz[doador.ordinal()][receptor.ordinal()];
    }

    /**
     * Doadores compativeis com {@code receptor} em ordem de preferencia: idêntico primeiro, depois
     * do mais especifico ao mais universal, O- por ultimo. Lista imutavel; receptor nulo devolve
     * lista vazia.
     */
    public List<TipoSanguineo> matching(TipoSanguineo receptor) {
        if (receptor == null) {
            return List.of();
        }
        return matchingPorReceptor.get(receptor.ordinal());
    }

    private static int antigenos(TipoSanguineo tipo) {
        return switch (tipo) {
            case A_POS -> ANTIGENO_A | ANTIGENO_RH;
            case A_NEG -> ANTIGENO_A;
            case B_POS -> ANTIGENO_B | ANTIGENO_RH;
            case B_NEG -> ANTIGENO_B;
            case AB_POS -> ANTIGENO_A | ANTIGENO_B | ANTIGENO_RH;
            case AB_NEG -> ANTIGENO_A | ANTIGENO_B;
            case O_POS -> ANTIGENO_RH;
            case O_NEG -> 0;
        };
    }
}
