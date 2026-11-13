package com.hemorede.service;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.StatusRequisicao;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.ItemRequisicao;
import com.hemorede.domain.model.Requisicao;
import com.hemorede.dto.PrevisaoDemandaResponse;
import com.hemorede.exception.DiasPrevisaoInvalidoException;
import com.hemorede.exception.ModeloProbabilisticoIndisponivelException;
import com.hemorede.indicadores.ModeloProbabilisticoDemanda;
import com.hemorede.repository.RequisicaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

/** Estima a demanda diária de hemácias e fornece lambdas ao indicador de risco. */
@Service
public class PrevisaoDemandaService {

    private static final int JANELA_PREVISAO_DIAS = 7;
    private static final EnumSet<StatusRequisicao> STATUS_QUE_CONTAM_COMO_DEMANDA = EnumSet.of(
            StatusRequisicao.PENDENTE,
            StatusRequisicao.ALOCADA,
            StatusRequisicao.APROVADA,
            StatusRequisicao.EM_ROTA,
            StatusRequisicao.ENTREGUE);

    private final RequisicaoRepository requisicaoRepository;
    private final ModeloProbabilisticoDemanda modeloProbabilisticoDemanda;

    public PrevisaoDemandaService(
            RequisicaoRepository requisicaoRepository,
            ModeloProbabilisticoDemanda modeloProbabilisticoDemanda) {
        this.requisicaoRepository = requisicaoRepository;
        this.modeloProbabilisticoDemanda = modeloProbabilisticoDemanda;
    }

    @Transactional(readOnly = true)
    public PrevisaoDemandaResponse calcularPrevisaoDemanda(int dias) {
        validarHorizontePrevisao(dias);
        Map<TipoSanguineo, Long> observadas = carregarDemandaRecente();
        Map<TipoSanguineo, Double> lambdasReferencia = obterLambdasDeReferenciaSeNecessario(observadas);

        List<PrevisaoDemandaResponse.PrevisaoPorTipo> previsoes = Arrays.stream(TipoSanguineo.values())
                .sorted(Comparator.comparing(TipoSanguineo::name))
                .map(tipo -> {
                    long quantidadeObservada = observadas.getOrDefault(tipo, 0L);
                    boolean temHistorico = quantidadeObservada > 0;
                    double lambdaDiario = temHistorico
                            ? (double) quantidadeObservada / JANELA_PREVISAO_DIAS
                            : lambdasReferencia.get(tipo);
                    return new PrevisaoDemandaResponse.PrevisaoPorTipo(
                            tipo,
                            quantidadeObservada,
                            arredondar(lambdaDiario),
                            arredondar(lambdaDiario * dias),
                            temHistorico ? "MEDIA_MOVEL_7_DIAS" : "LAMBDA_DE_REFERENCIA_PI2_177");
                })
                .toList();

        LocalDate hoje = LocalDate.now();
        return new PrevisaoDemandaResponse(
                dias,
                JANELA_PREVISAO_DIAS,
                hoje.minusDays(JANELA_PREVISAO_DIAS - 1L),
                hoje,
                LocalDateTime.now(),
                previsoes);
    }

    /** Retorna os lambdas estimados pela janela móvel para o cálculo de risco. */
    @Transactional(readOnly = true)
    public Map<TipoSanguineo, Double> estimarLambdasDiarios() {
        Map<TipoSanguineo, Long> observadas = carregarDemandaRecente();
        Map<TipoSanguineo, Double> lambdasReferencia = obterLambdasDeReferenciaSeNecessario(observadas);
        Map<TipoSanguineo, Double> lambdas = new EnumMap<>(TipoSanguineo.class);
        for (TipoSanguineo tipo : TipoSanguineo.values()) {
            long quantidade = observadas.getOrDefault(tipo, 0L);
            double lambda = quantidade > 0
                    ? (double) quantidade / JANELA_PREVISAO_DIAS
                    : lambdasReferencia.get(tipo);
            lambdas.put(tipo, lambda);
        }
        return lambdas;
    }

    private Map<TipoSanguineo, Long> carregarDemandaRecente() {
        if (requisicaoRepository == null) {
            throw new ModeloProbabilisticoIndisponivelException(
                    "Repositorio de requisicoes nao configurado para estimar demanda");
        }
        LocalDate hoje = LocalDate.now();
        LocalDate inicio = hoje.minusDays(JANELA_PREVISAO_DIAS - 1L);
        return contarDemandaPorTipo(requisicaoRepository.findAll(), inicio, hoje);
    }

    private Map<TipoSanguineo, Long> contarDemandaPorTipo(
            List<Requisicao> requisicoes, LocalDate inicio, LocalDate fim) {
        Map<TipoSanguineo, Long> quantidades = new EnumMap<>(TipoSanguineo.class);
        if (requisicoes == null) {
            return quantidades;
        }

        for (Requisicao requisicao : requisicoes) {
            if (requisicao == null
                    || requisicao.getDataSolicitacao() == null
                    || requisicao.getStatus() == null
                    || !STATUS_QUE_CONTAM_COMO_DEMANDA.contains(requisicao.getStatus())) {
                continue;
            }
            LocalDate data = requisicao.getDataSolicitacao().toLocalDate();
            if (data.isBefore(inicio) || data.isAfter(fim) || requisicao.getItens() == null) {
                continue;
            }
            for (ItemRequisicao item : requisicao.getItens()) {
                if (item == null
                        || item.getHemoComponente() != HemoComponente.HEMACIAS
                        || item.getTipoSanguineo() == null
                        || item.getQuantidade() == null
                        || item.getQuantidade() <= 0) {
                    continue;
                }
                quantidades.merge(item.getTipoSanguineo(), item.getQuantidade().longValue(), Long::sum);
            }
        }
        return quantidades;
    }

    private Map<TipoSanguineo, Double> obterLambdasDeReferenciaSeNecessario(
            Map<TipoSanguineo, Long> observadas) {
        boolean haTipoSemHistorico = Arrays.stream(TipoSanguineo.values())
                .anyMatch(tipo -> observadas.getOrDefault(tipo, 0L) == 0L);
        if (!haTipoSemHistorico) {
            return Map.of();
        }
        if (modeloProbabilisticoDemanda == null) {
            throw new ModeloProbabilisticoIndisponivelException(
                    "Modelo de referencia PI2-177 nao configurado para tipos sem historico");
        }
        return modeloProbabilisticoDemanda.obterLambdasValidados();
    }

    private void validarHorizontePrevisao(int dias) {
        if (dias < 1 || dias > JANELA_PREVISAO_DIAS) {
            throw new DiasPrevisaoInvalidoException(
                    "O horizonte da previsao deve estar entre 1 e 7 dias");
        }
    }

    private double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
