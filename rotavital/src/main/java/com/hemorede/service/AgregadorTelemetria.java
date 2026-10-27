package com.hemorede.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

/**
 * Agregador de leituras de telemetria por veículo (PI2-169).
 * Versão inicial sem sincronização para evidenciar condição de corrida (TDD - Red).
 */
@Service
public class AgregadorTelemetria {

    public static final double TEMPERATURA_MIN_SEGURA_C = 2.0;
    public static final double TEMPERATURA_MAX_SEGURA_C = 6.0;

    public record Leitura(long veiculoId, double temperaturaC) {
    }

    public record ResumoVeiculo(
            long veiculoId,
            long totalLeituras,
            long totalAlertas,
            double somaTemperaturaC,
            double mediaTemperaturaC,
            double menorTemperaturaC,
            double maiorTemperaturaC,
            double ultimaTemperaturaC,
            boolean emAlerta
    ) {
    }

    private static class AcumuladorVeiculo {
        long totalLeituras;
        long totalAlertas;
        double somaTemperaturaC;
        double menorTemperaturaC = Double.POSITIVE_INFINITY;
        double maiorTemperaturaC = Double.NEGATIVE_INFINITY;
        double ultimaTemperaturaC;

        void registrar(double temperaturaC, boolean alerta) {
            totalLeituras++;
            if (alerta) {
                totalAlertas++;
            }
            somaTemperaturaC += temperaturaC;
            if (temperaturaC < menorTemperaturaC) {
                menorTemperaturaC = temperaturaC;
            }
            if (temperaturaC > maiorTemperaturaC) {
                maiorTemperaturaC = temperaturaC;
            }
            ultimaTemperaturaC = temperaturaC;
        }
    }

    private final Map<Long, AcumuladorVeiculo> porVeiculo = new HashMap<>();
    private long totalLeiturasGlobal;
    private long totalAlertasGlobal;

    public static boolean isForaDaFaixa2a6(double temperaturaC) {
        return temperaturaC < TEMPERATURA_MIN_SEGURA_C || temperaturaC > TEMPERATURA_MAX_SEGURA_C;
    }

    public boolean registrarLeitura(long veiculoId, double temperaturaC) {
        boolean alerta = isForaDaFaixa2a6(temperaturaC);
        AcumuladorVeiculo acumulador = porVeiculo.get(veiculoId);
        if (acumulador == null) {
            acumulador = new AcumuladorVeiculo();
            porVeiculo.put(veiculoId, acumulador);
        }
        acumulador.registrar(temperaturaC, alerta);
        totalLeiturasGlobal++;
        if (alerta) {
            totalAlertasGlobal++;
        }
        return alerta;
    }

    public long getTotalLeituras() {
        return totalLeiturasGlobal;
    }

    public long getTotalAlertas() {
        return totalAlertasGlobal;
    }

    public long getTotalLeiturasVeiculo(long veiculoId) {
        AcumuladorVeiculo acumulador = porVeiculo.get(veiculoId);
        return acumulador == null ? 0L : acumulador.totalLeituras;
    }

    public long getTotalAlertasVeiculo(long veiculoId) {
        AcumuladorVeiculo acumulador = porVeiculo.get(veiculoId);
        return acumulador == null ? 0L : acumulador.totalAlertas;
    }

    public Optional<ResumoVeiculo> obterResumoVeiculo(long veiculoId) {
        AcumuladorVeiculo acc = porVeiculo.get(veiculoId);
        if (acc == null || acc.totalLeituras == 0) {
            return Optional.empty();
        }
        double media = acc.somaTemperaturaC / acc.totalLeituras;
        return Optional.of(new ResumoVeiculo(
                veiculoId,
                acc.totalLeituras,
                acc.totalAlertas,
                acc.somaTemperaturaC,
                media,
                acc.menorTemperaturaC,
                acc.maiorTemperaturaC,
                acc.ultimaTemperaturaC,
                isForaDaFaixa2a6(acc.ultimaTemperaturaC)
        ));
    }

    public void limpar() {
        porVeiculo.clear();
        totalLeiturasGlobal = 0L;
        totalAlertasGlobal = 0L;
    }
}
