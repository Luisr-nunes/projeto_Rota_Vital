package com.hemorede.service;

import com.hemorede.dto.LeituraTemperaturaResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.DoubleAdder;
import java.util.concurrent.atomic.LongAdder;

/**
 * Agregador sincronizado de leituras de telemetria por veículo (PI2-169 / SO U2).
 * <p>
 * Utiliza {@link ConcurrentHashMap} para indexação concorrente por {@code veiculoId}
 * e {@link LongAdder} / {@link DoubleAdder} para contadores e somas sob contenção de
 * múltiplas threads, além de operações atômicas CAS ({@link AtomicLong}) para
 * extremos de temperatura. Classifica alertas fora da faixa de 2 °C a 6 °C.
 * </p>
 */
@Service
public class AgregadorTelemetria {

    public static final double TEMPERATURA_MIN_SEGURA_C = 2.0;
    public static final double TEMPERATURA_MAX_SEGURA_C = 6.0;
    public static final String STATUS_OK = "OK";
    public static final String STATUS_ALERTA = "ALERTA";

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

    private static final class AcumuladorVeiculo {
        private final LongAdder totalLeituras = new LongAdder();
        private final LongAdder totalAlertas = new LongAdder();
        private final DoubleAdder somaTemperaturaC = new DoubleAdder();
        private final AtomicLong menorTemperaturaBits =
                new AtomicLong(Double.doubleToLongBits(Double.POSITIVE_INFINITY));
        private final AtomicLong maiorTemperaturaBits =
                new AtomicLong(Double.doubleToLongBits(Double.NEGATIVE_INFINITY));
        private final AtomicLong ultimaTemperaturaBits =
                new AtomicLong(Double.doubleToLongBits(0.0));

        void registrar(double temperaturaC, boolean alerta) {
            totalLeituras.increment();
            if (alerta) {
                totalAlertas.increment();
            }
            somaTemperaturaC.add(temperaturaC);
            atualizarMinimo(temperaturaC);
            atualizarMaximo(temperaturaC);
            ultimaTemperaturaBits.set(Double.doubleToLongBits(temperaturaC));
        }

        private void atualizarMinimo(double valor) {
            long novoBits = Double.doubleToLongBits(valor);
            while (true) {
                long atualBits = menorTemperaturaBits.get();
                double atual = Double.longBitsToDouble(atualBits);
                if (valor >= atual) {
                    return;
                }
                if (menorTemperaturaBits.compareAndSet(atualBits, novoBits)) {
                    return;
                }
            }
        }

        private void atualizarMaximo(double valor) {
            long novoBits = Double.doubleToLongBits(valor);
            while (true) {
                long atualBits = maiorTemperaturaBits.get();
                double atual = Double.longBitsToDouble(atualBits);
                if (valor <= atual) {
                    return;
                }
                if (maiorTemperaturaBits.compareAndSet(atualBits, novoBits)) {
                    return;
                }
            }
        }
    }

    private final ConcurrentHashMap<Long, AcumuladorVeiculo> porVeiculo = new ConcurrentHashMap<>();
    private final LongAdder totalLeiturasGlobal = new LongAdder();
    private final LongAdder totalAlertasGlobal = new LongAdder();

    /**
     * Verifica se a temperatura está fora da faixa segura da cadeia fria padrão (2 °C a 6 °C, limites inclusos).
     */
    public static boolean isForaDaFaixa2a6(double temperaturaC) {
        return temperaturaC < TEMPERATURA_MIN_SEGURA_C || temperaturaC > TEMPERATURA_MAX_SEGURA_C;
    }

    /**
     * Classifica a temperatura segundo a faixa de 2 °C a 6 °C ({@code OK} ou {@code ALERTA}).
     */
    public static String classificarFaixa2a6(double temperaturaC) {
        return isForaDaFaixa2a6(temperaturaC) ? STATUS_ALERTA : STATUS_OK;
    }

    /**
     * Registra uma leitura de temperatura de forma thread-safe para o veículo informado.
     *
     * @return {@code true} se a leitura gerou alerta (fora de 2–6 °C), {@code false} caso contrário
     */
    public boolean registrarLeitura(long veiculoId, double temperaturaC) {
        boolean alerta = isForaDaFaixa2a6(temperaturaC);
        AcumuladorVeiculo acumulador = porVeiculo.computeIfAbsent(veiculoId, id -> new AcumuladorVeiculo());
        acumulador.registrar(temperaturaC, alerta);
        totalLeiturasGlobal.increment();
        if (alerta) {
            totalAlertasGlobal.increment();
        }
        return alerta;
    }

    /**
     * Registra uma leitura a partir do DTO de resposta de telemetria.
     */
    public boolean registrarLeitura(LeituraTemperaturaResponse leitura) {
        return registrarLeitura(leitura.veiculoId(), leitura.temperaturaC());
    }

    /**
     * Processa um lote de leituras de forma sequencial (1 thread).
     */
    public void processarSequencial(List<Leitura> leituras) {
        for (Leitura leitura : leituras) {
            registrarLeitura(leitura.veiculoId(), leitura.temperaturaC());
        }
    }

    /**
     * Processa um lote de leituras de forma concorrente utilizando {@code numeroDeThreads}
     * threads de plataforma gravando simultaneamente nas estruturas sincronizadas.
     */
    public void processarConcorrente(List<Leitura> leituras, int numeroDeThreads) {
        if (numeroDeThreads < 1) {
            throw new IllegalArgumentException("O número de threads deve ser >= 1");
        }
        if (leituras.isEmpty()) {
            return;
        }
        ExecutorService executor = Executors.newFixedThreadPool(numeroDeThreads);
        try {
            int total = leituras.size();
            int fatias = Math.min(numeroDeThreads, total);
            int tamanhoBase = total / fatias;
            int resto = total % fatias;

            List<Callable<Void>> tarefas = new ArrayList<>(fatias);
            int inicio = 0;
            for (int i = 0; i < fatias; i++) {
                int fim = inicio + tamanhoBase + (i < resto ? 1 : 0);
                List<Leitura> sublista = leituras.subList(inicio, fim);
                tarefas.add(() -> {
                    for (Leitura leitura : sublista) {
                        registrarLeitura(leitura.veiculoId(), leitura.temperaturaC());
                    }
                    return null;
                });
                inicio = fim;
            }

            List<Future<Void>> futuros = executor.invokeAll(tarefas);
            for (Future<Void> futuro : futuros) {
                futuro.get();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Processamento concorrente de telemetria interrompido", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Falha ao agregar leituras de telemetria concorrentemente", e.getCause());
        } finally {
            executor.shutdown();
        }
    }

    public long getTotalLeituras() {
        return totalLeiturasGlobal.sum();
    }

    public long getTotalAlertas() {
        return totalAlertasGlobal.sum();
    }

    public long getTotalLeiturasVeiculo(long veiculoId) {
        AcumuladorVeiculo acumulador = porVeiculo.get(veiculoId);
        return acumulador == null ? 0L : acumulador.totalLeituras.sum();
    }

    public long getTotalAlertasVeiculo(long veiculoId) {
        AcumuladorVeiculo acumulador = porVeiculo.get(veiculoId);
        return acumulador == null ? 0L : acumulador.totalAlertas.sum();
    }

    public int getQuantidadeVeiculosMonitorados() {
        return porVeiculo.size();
    }

    public Optional<ResumoVeiculo> obterResumoVeiculo(long veiculoId) {
        AcumuladorVeiculo acc = porVeiculo.get(veiculoId);
        if (acc == null) {
            return Optional.empty();
        }
        long leituras = acc.totalLeituras.sum();
        if (leituras == 0) {
            return Optional.empty();
        }
        long alertas = acc.totalAlertas.sum();
        double soma = acc.somaTemperaturaC.sum();
        double media = soma / leituras;
        double menor = Double.longBitsToDouble(acc.menorTemperaturaBits.get());
        double maior = Double.longBitsToDouble(acc.maiorTemperaturaBits.get());
        double ultima = Double.longBitsToDouble(acc.ultimaTemperaturaBits.get());
        return Optional.of(new ResumoVeiculo(
                veiculoId,
                leituras,
                alertas,
                soma,
                media,
                menor,
                maior,
                ultima,
                isForaDaFaixa2a6(ultima)
        ));
    }

    public Map<Long, ResumoVeiculo> obterResumoPorVeiculo() {
        Map<Long, ResumoVeiculo> resultado = new LinkedHashMap<>();
        porVeiculo.keySet().stream().sorted().forEach(veiculoId ->
                obterResumoVeiculo(veiculoId).ifPresent(resumo -> resultado.put(veiculoId, resumo))
        );
        return Collections.unmodifiableMap(resultado);
    }

    public void limpar() {
        porVeiculo.clear();
        totalLeiturasGlobal.reset();
        totalAlertasGlobal.reset();
    }
}
