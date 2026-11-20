package com.hemorede.domain.regras;

import com.hemorede.domain.enums.HemoComponente;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * RN-03 - Validade da bolsa.
 * <ul>
 *   <li>Bolsa vencida (validade antes de hoje) nunca é alocada; vence no fim do dia da validade.</li>
 *   <li>A coleta não pode estar no futuro e a validade deve ser posterior à coleta.</li>
 *   <li>A validade não pode passar do prazo máximo do hemocomponente
 *       (hemácias 42 d, plaquetas 5 d, plasma e crio 365 d).</li>
 * </ul>
 * Classe pura (sem Spring/JPA).
 */
public final class RegraValidadeBolsa {

    private RegraValidadeBolsa() {
    }

    public static boolean vencida(LocalDate validade, LocalDate hoje) {
        return validade == null || validade.isBefore(hoje);
    }

    /** Validade padrão do hemocomponente a partir da coleta. */
    public static LocalDate validadePadrao(HemoComponente componente, LocalDate dataColeta) {
        return dataColeta.plusDays(componente.getValidadeEmDias());
    }

    /** Dias até vencer (0 = vence hoje, negativo = já venceu). */
    public static long diasParaVencer(LocalDate validade, LocalDate hoje) {
        return ChronoUnit.DAYS.between(hoje, validade);
    }

    /** Lista os problemas das datas informadas; vazia = datas válidas. */
    public static List<String> validarDatas(HemoComponente componente, LocalDate dataColeta,
                                            LocalDate validade, LocalDate hoje) {
        List<String> problemas = new ArrayList<>();
        if (dataColeta == null || validade == null || componente == null) {
            problemas.add("componente, data da coleta e validade são obrigatórios");
            return problemas;
        }
        if (dataColeta.isAfter(hoje)) {
            problemas.add("a data da coleta não pode estar no futuro");
        }
        if (!validade.isAfter(dataColeta)) {
            problemas.add("a validade deve ser posterior à data da coleta");
        }
        if (validade.isAfter(validadePadrao(componente, dataColeta))) {
            problemas.add("a validade passa do prazo máximo de " + componente.getValidadeEmDias()
                    + " dias para " + componente);
        }
        return problemas;
    }
}
