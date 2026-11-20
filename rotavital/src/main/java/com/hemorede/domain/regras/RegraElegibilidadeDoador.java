package com.hemorede.domain.regras;

import com.hemorede.domain.enums.Sexo;

import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * RN-04 - Elegibilidade do doador (baseada nos critérios gerais do Ministério da Saúde,
 * simplificados para o escopo acadêmico):
 * <ul>
 *   <li>idade entre 16 e 69 anos;</li>
 *   <li>peso mínimo de 50 kg;</li>
 *   <li>intervalo mínimo entre doações: 60 dias (masculino) e 90 dias (feminino);</li>
 *   <li>no máximo 4 doações em 12 meses (masculino) e 3 (feminino);</li>
 *   <li>doador inativo não doa.</li>
 * </ul>
 * Campos não informados (idade, peso, sexo) não reprovam: o sexo ausente usa o intervalo
 * de 60 dias para manter os cadastros legados. Classe pura (sem Spring/JPA).
 */
public final class RegraElegibilidadeDoador {

    public static final int IDADE_MINIMA = 16;
    public static final int IDADE_MAXIMA = 69;
    public static final double PESO_MINIMO_KG = 50.0;
    public static final int INTERVALO_MASCULINO_DIAS = 60;
    public static final int INTERVALO_FEMININO_DIAS = 90;
    public static final int MAX_DOACOES_ANO_MASCULINO = 4;
    public static final int MAX_DOACOES_ANO_FEMININO = 3;

    private RegraElegibilidadeDoador() {
    }

    /** Resultado da avaliação: apto, motivos de inaptidão e a próxima data possível pelo intervalo. */
    public record Resultado(boolean apto, List<String> motivos, LocalDate proximaDataPossivel) {
    }

    public static int intervaloMinimoDias(Sexo sexo) {
        return sexo == Sexo.FEMININO ? INTERVALO_FEMININO_DIAS : INTERVALO_MASCULINO_DIAS;
    }

    public static int maximoDoacoesPorAno(Sexo sexo) {
        return sexo == Sexo.FEMININO ? MAX_DOACOES_ANO_FEMININO : MAX_DOACOES_ANO_MASCULINO;
    }

    /**
     * Avalia se o doador pode doar em {@code dataDoacao}. O(1).
     *
     * @param doacoesUltimos12Meses quantidade de doações nos 12 meses anteriores à data
     */
    public static Resultado avaliar(boolean ativo, Sexo sexo, LocalDate dataNascimento, Double pesoKg,
                                    LocalDate dataUltimaDoacao, int doacoesUltimos12Meses,
                                    LocalDate dataDoacao) {
        List<String> motivos = new ArrayList<>();
        if (!ativo) {
            motivos.add("doador inativo");
        }
        if (dataNascimento != null) {
            int idade = Period.between(dataNascimento, dataDoacao).getYears();
            if (idade < IDADE_MINIMA || idade > IDADE_MAXIMA) {
                motivos.add("idade fora da faixa de " + IDADE_MINIMA + " a " + IDADE_MAXIMA + " anos (" + idade + ")");
            }
        }
        if (pesoKg != null && pesoKg < PESO_MINIMO_KG) {
            motivos.add("peso abaixo de " + (int) PESO_MINIMO_KG + " kg");
        }

        LocalDate proxima = dataDoacao;
        if (dataUltimaDoacao != null) {
            int intervalo = intervaloMinimoDias(sexo);
            proxima = dataUltimaDoacao.plusDays(intervalo);
            if (dataDoacao.isBefore(proxima)) {
                long faltam = java.time.temporal.ChronoUnit.DAYS.between(dataDoacao, proxima);
                motivos.add("intervalo mínimo de " + intervalo + " dias desde a última doação; faltam "
                        + faltam + " dia(s)");
            }
        }
        if (doacoesUltimos12Meses >= maximoDoacoesPorAno(sexo)) {
            motivos.add("limite de " + maximoDoacoesPorAno(sexo) + " doações em 12 meses atingido");
        }
        return new Resultado(motivos.isEmpty(), List.copyOf(motivos), proxima);
    }
}
