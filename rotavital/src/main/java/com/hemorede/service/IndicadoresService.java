package com.hemorede.service;

import com.hemorede.algoritmos.DadosSinteticos;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.StatusRequisicao;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.dto.IndicadoresResponse;
import com.hemorede.indicadores.DadosIndicadoresSinteticos;
import com.hemorede.indicadores.DadosIndicadoresSinteticos.AtendimentoSintetico;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula os indicadores da sprint de Estatística sobre dados 100% sintéticos.
 * Não altera nem persiste entidades criadas por outros integrantes da equipe.
 */
@Service
public class IndicadoresService {

    public IndicadoresResponse calcular(int diasProximoVencimento) {
        return calcular(
                diasProximoVencimento,
                DadosSinteticos.criarBolsasExemplo(),
                DadosIndicadoresSinteticos.criarAtendimentosExemplo()
        );
    }

    IndicadoresResponse calcular(
            int diasProximoVencimento,
            List<Bolsa> bolsas,
            List<AtendimentoSintetico> atendimentos
    ) {
        if (diasProximoVencimento < 0) {
            throw new IllegalArgumentException("A janela de vencimento não pode ser negativa");
        }

        LocalDate hoje = LocalDate.now();
        LocalDate dataLimite = hoje.plusDays(diasProximoVencimento);

        List<Bolsa> bolsasDisponiveisValidas = bolsas.stream()
                .filter(bolsa -> bolsa.getStatus() == StatusBolsa.DISPONIVEL)
                .filter(bolsa -> bolsa.getDataValidade() != null)
                .filter(bolsa -> !bolsa.getDataValidade().isBefore(hoje))
                .toList();

        Map<TipoSanguineo, Long> estoquePorTipo = iniciarContagemPorTipo();
        for (Bolsa bolsa : bolsasDisponiveisValidas) {
            estoquePorTipo.merge(bolsa.getTipoSanguineo(), 1L, Long::sum);
        }

        List<Long> quantidadesPorTipo = new ArrayList<>(estoquePorTipo.values());
        double media = quantidadesPorTipo.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double mediana = calcularMediana(quantidadesPorTipo);
        long minimo = quantidadesPorTipo.stream().mapToLong(Long::longValue).min().orElse(0L);
        long maximo = quantidadesPorTipo.stream().mapToLong(Long::longValue).max().orElse(0L);
        double desvioPadrao = calcularDesvioPadraoPopulacional(quantidadesPorTipo, media);

        long proximasDoVencimento = bolsasDisponiveisValidas.stream()
                .filter(bolsa -> !bolsa.getDataValidade().isAfter(dataLimite))
                .count();

        double percentualProximoDoVencimento = bolsasDisponiveisValidas.isEmpty()
                ? 0.0
                : proximasDoVencimento * 100.0 / bolsasDisponiveisValidas.size();

        Map<StatusRequisicao, Long> requisicoesPorStatus = iniciarContagemPorStatus();
        for (AtendimentoSintetico atendimento : atendimentos) {
            requisicoesPorStatus.merge(atendimento.status(), 1L, Long::sum);
        }

        List<Long> temposDeAtendimento = atendimentos.stream()
                .map(AtendimentoSintetico::tempoAtendimentoEmMinutos)
                .filter(tempo -> tempo != null && tempo >= 0)
                .toList();

        Double tempoMedio = temposDeAtendimento.isEmpty()
                ? null
                : arredondar(temposDeAtendimento.stream().mapToLong(Long::longValue).average().orElse(0.0));

        var estoqueResumo = new IndicadoresResponse.EstoqueResumo(
                bolsasDisponiveisValidas.size(),
                estoquePorTipo,
                arredondar(media),
                arredondar(mediana),
                minimo,
                maximo,
                arredondar(desvioPadrao)
        );

        var vencimentoResumo = new IndicadoresResponse.VencimentoResumo(
                diasProximoVencimento,
                proximasDoVencimento,
                arredondar(percentualProximoDoVencimento)
        );

        var requisicoesResumo = new IndicadoresResponse.RequisicoesResumo(
                atendimentos.size(),
                temposDeAtendimento.size(),
                tempoMedio,
                requisicoesPorStatus
        );

        return new IndicadoresResponse(
                LocalDateTime.now(),
                estoqueResumo,
                vencimentoResumo,
                requisicoesResumo
        );
    }

    private Map<TipoSanguineo, Long> iniciarContagemPorTipo() {
        Map<TipoSanguineo, Long> contagem = new LinkedHashMap<>();
        Arrays.stream(TipoSanguineo.values()).forEach(tipo -> contagem.put(tipo, 0L));
        return contagem;
    }

    private Map<StatusRequisicao, Long> iniciarContagemPorStatus() {
        Map<StatusRequisicao, Long> contagem = new LinkedHashMap<>();
        Arrays.stream(StatusRequisicao.values()).forEach(status -> contagem.put(status, 0L));
        return contagem;
    }

    private double calcularMediana(List<Long> valores) {
        if (valores.isEmpty()) {
            return 0.0;
        }

        List<Long> ordenados = valores.stream().sorted().toList();
        int meio = ordenados.size() / 2;
        if (ordenados.size() % 2 == 1) {
            return ordenados.get(meio);
        }
        return (ordenados.get(meio - 1) + ordenados.get(meio)) / 2.0;
    }

    private double calcularDesvioPadraoPopulacional(List<Long> valores, double media) {
        if (valores.isEmpty()) {
            return 0.0;
        }

        double variancia = valores.stream()
                .mapToDouble(valor -> Math.pow(valor - media, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variancia);
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
