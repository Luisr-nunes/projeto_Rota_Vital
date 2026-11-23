package com.hemorede.domain.regras;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.expedicao.domain.enums.TipoRefrigeracao;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/**
 * RN-06 - Cadeia fria: o veículo só transporta componentes cuja refrigeração exigida é a dele.
 * Uma requisição que mistura componentes de faixas diferentes (ex.: hemácias 2-6 °C e plasma
 * -20 °C) precisa de mais de um veículo e não pode sair num despacho único.
 * Classe pura (sem Spring/JPA).
 */
public final class RegraRefrigeracao {

    private RegraRefrigeracao() {
    }

    public static boolean veiculoCompativel(TipoRefrigeracao doVeiculo, HemoComponente componente) {
        return doVeiculo != null && componente != null && componente.getRefrigeracaoExigida() == doVeiculo;
    }

    /** Faixas de refrigeração exigidas pelo conjunto de componentes. O(n). */
    public static Set<TipoRefrigeracao> exigidas(Collection<HemoComponente> componentes) {
        Set<TipoRefrigeracao> faixas = EnumSet.noneOf(TipoRefrigeracao.class);
        for (HemoComponente c : componentes) {
            if (c != null) {
                faixas.add(c.getRefrigeracaoExigida());
            }
        }
        return faixas;
    }

    /** {@code true} se todos os componentes cabem em um único veículo. */
    public static boolean despachoUnicoPossivel(Collection<HemoComponente> componentes) {
        return exigidas(componentes).size() == 1;
    }
}
