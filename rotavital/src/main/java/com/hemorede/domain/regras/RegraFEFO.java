package com.hemorede.domain.regras;

import com.hemorede.domain.enums.TipoSanguineo;

import java.time.LocalDate;
import java.util.Collection;

/**
 * RN-02 - FEFO (First-Expired, First-Out): a bolsa que vence primeiro sai primeiro.
 * <p>
 * Critérios de desempate, nesta ordem:
 * <ol>
 *   <li>menor data de validade;</li>
 *   <li>tipo sanguíneo idêntico ao pedido (preserva O- e outros tipos universais);</li>
 *   <li>coleta mais antiga.</li>
 * </ol>
 * Classe pura (sem Spring/JPA).
 */
public final class RegraFEFO {

    private RegraFEFO() {
    }

    /**
     * Compara duas candidatas para o mesmo pedido. Negativo = {@code a} deve sair antes de {@code b}.
     * Datas de coleta nulas são tratadas como "mais recentes" (perdem o desempate).
     */
    public static int comparar(LocalDate validadeA, TipoSanguineo tipoA, LocalDate coletaA,
                               LocalDate validadeB, TipoSanguineo tipoB, LocalDate coletaB,
                               TipoSanguineo pedido) {
        int porValidade = validadeA.compareTo(validadeB);
        if (porValidade != 0) {
            return porValidade;
        }
        boolean aIdentica = tipoA == pedido;
        boolean bIdentica = tipoB == pedido;
        if (aIdentica != bIdentica) {
            return aIdentica ? -1 : 1;
        }
        if (coletaA == null || coletaB == null) {
            return coletaA == null ? (coletaB == null ? 0 : 1) : -1;
        }
        return coletaA.compareTo(coletaB);
    }

    /**
     * Auditoria de saída: a seleção respeita o FEFO se nenhuma bolsa que ficou no estoque
     * vence antes da bolsa de validade mais longa que saiu. O(s + r).
     */
    public static boolean respeitaFEFO(Collection<LocalDate> validadesQueSairam,
                                       Collection<LocalDate> validadesQueFicaram) {
        if (validadesQueSairam.isEmpty() || validadesQueFicaram.isEmpty()) {
            return true;
        }
        LocalDate maisLongaQueSaiu = validadesQueSairam.stream().max(LocalDate::compareTo).orElseThrow();
        LocalDate maisCurtaQueFicou = validadesQueFicaram.stream().min(LocalDate::compareTo).orElseThrow();
        return !maisCurtaQueFicou.isBefore(maisLongaQueSaiu);
    }
}
