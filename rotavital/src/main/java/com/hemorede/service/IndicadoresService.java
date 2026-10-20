package com.hemorede.service;

import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.StatusRequisicao;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.domain.model.Requisicao;
import com.hemorede.dto.IndicadoresResponse;
import com.hemorede.dto.RiscoDesabastecimentoResponse;
import com.hemorede.exception.DiasPrevisaoInvalidoException;
import com.hemorede.exception.ModeloProbabilisticoIndisponivelException;
import com.hemorede.indicadores.ModeloProbabilisticoDemanda;
import com.hemorede.indicadores.DadosIndicadoresSinteticos.AtendimentoSintetico;
import com.hemorede.repository.BolsaRepository;
import com.hemorede.repository.RequisicaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Calcula os indicadores estatísticos e operacionais do Rota Vital (HU07).
 * Integra-se diretamente com os repositórios JPA de {@link Bolsa} e {@link Requisicao},
 * refletindo tanto a massa inicial do {@code DataInitializer} quanto cenários de banco vazio.
 */
@Service
public class IndicadoresService {

    private final BolsaRepository bolsaRepository;
    private final RequisicaoRepository requisicaoRepository;
    private final ModeloProbabilisticoDemanda modeloProbabilisticoDemanda;

    @Autowired
    public IndicadoresService(
            BolsaRepository bolsaRepository,
            RequisicaoRepository requisicaoRepository,
            ModeloProbabilisticoDemanda modeloProbabilisticoDemanda) {
        this.bolsaRepository = bolsaRepository;
        this.requisicaoRepository = requisicaoRepository;
        this.modeloProbabilisticoDemanda = modeloProbabilisticoDemanda;
    }

    public IndicadoresService(BolsaRepository bolsaRepository, RequisicaoRepository requisicaoRepository) {
        this(bolsaRepository, requisicaoRepository, null);
    }

    public IndicadoresService() {
        this(null, null, null);
    }

    /**
     * Calcula os indicadores consultando os dados persistidos nos repositórios JPA.
     * Retorna estado vazio e neutro caso o banco não possua registros (Cenário 2 da HU07).
     *
     * @param diasProximoVencimento Janela temporal futura em dias para alerta de validade.
     * @return Resumo estatístico consolidado.
     */
    public IndicadoresResponse calcular(int diasProximoVencimento) {
        List<Bolsa> bolsas = (bolsaRepository != null) ? bolsaRepository.findAll() : List.of();
        List<Requisicao> requisicoes = (requisicaoRepository != null) ? requisicaoRepository.findAll() : List.of();
        return calcularComEntidades(diasProximoVencimento, bolsas, requisicoes);
    }

    /**
     * Calcula indicadores a partir das listas de entidades persistidas.
     */
    public IndicadoresResponse calcularComEntidades(
            int diasProximoVencimento,
            List<Bolsa> bolsas,
            List<Requisicao> requisicoes
    ) {
        if (diasProximoVencimento < 0) {
            throw new IllegalArgumentException("A janela de vencimento não pode ser negativa");
        }

        LocalDate hoje = LocalDate.now();
        LocalDate dataLimite = hoje.plusDays(diasProximoVencimento);

        List<Bolsa> listaBolsas = bolsas != null ? bolsas : List.of();
        List<Bolsa> bolsasDisponiveisValidas = listaBolsas.stream()
                .filter(bolsa -> bolsa.getStatus() == StatusBolsa.DISPONIVEL)
                .filter(bolsa -> bolsa.getDataValidade() != null)
                .filter(bolsa -> !bolsa.getDataValidade().isBefore(hoje))
                .toList();

        Map<TipoSanguineo, Long> estoquePorTipo = iniciarContagemPorTipo();
        for (Bolsa bolsa : bolsasDisponiveisValidas) {
            if (bolsa.getTipoSanguineo() != null) {
                estoquePorTipo.merge(bolsa.getTipoSanguineo(), 1L, Long::sum);
            }
        }

        List<Long> quantidadesPorTipo = new ArrayList<>(estoquePorTipo.values());
        boolean semEstoque = bolsasDisponiveisValidas.isEmpty();
        double media = semEstoque ? 0.0 : quantidadesPorTipo.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double mediana = semEstoque ? 0.0 : calcularMediana(quantidadesPorTipo);
        long minimo = semEstoque ? 0L : quantidadesPorTipo.stream().mapToLong(Long::longValue).min().orElse(0L);
        long maximo = semEstoque ? 0L : quantidadesPorTipo.stream().mapToLong(Long::longValue).max().orElse(0L);
        double desvioPadrao = semEstoque ? 0.0 : calcularDesvioPadraoPopulacional(quantidadesPorTipo, media);

        long proximasDoVencimento = bolsasDisponiveisValidas.stream()
                .filter(bolsa -> !bolsa.getDataValidade().isAfter(dataLimite))
                .count();

        double percentualProximoDoVencimento = semEstoque
                ? 0.0
                : proximasDoVencimento * 100.0 / bolsasDisponiveisValidas.size();

        Map<StatusRequisicao, Long> requisicoesPorStatus = iniciarContagemPorStatus();
        List<Long> temposDeAtendimento = new ArrayList<>();
        long atendidas = 0;

        List<Requisicao> listaRequisicoes = requisicoes != null ? requisicoes : List.of();
        for (Requisicao req : listaRequisicoes) {
            if (req.getStatus() != null) {
                requisicoesPorStatus.merge(req.getStatus(), 1L, Long::sum);
            }
            if (req.getStatus() == StatusRequisicao.ENTREGUE || req.getStatus() == StatusRequisicao.APROVADA) {
                atendidas++;
                Long tempo = extrairTempoAtendimentoEmMinutos(req);
                if (tempo != null && tempo >= 0) {
                    temposDeAtendimento.add(tempo);
                }
            }
        }

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
                listaRequisicoes.size(),
                atendidas,
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

    /**
     * Calcula o risco usando os lambdas configurados pelo modelo da PI2-177.
     */
    public RiscoDesabastecimentoResponse calcularRiscoDesabastecimento(int dias) {
        if (dias <= 0) {
            throw new DiasPrevisaoInvalidoException("O numero de dias deve ser maior que zero");
        }
        if (bolsaRepository == null || modeloProbabilisticoDemanda == null) {
            throw new ModeloProbabilisticoIndisponivelException(
                    "Modelo probabilistico ou repositorio de estoque nao configurado");
        }

        return calcularRiscoDesabastecimento(
                dias,
                bolsaRepository.findAll(),
                modeloProbabilisticoDemanda.obterLambdasValidados());
    }

    /**
     * Nucleo reproduzivel do calculo: para cada tipo, X segue Poisson(lambda * dias)
     * e o risco corresponde a P(X > estoque atual).
     */
    public RiscoDesabastecimentoResponse calcularRiscoDesabastecimento(
            int dias,
            List<Bolsa> bolsas,
            Map<TipoSanguineo, Double> lambdasDiarios) {
        if (dias <= 0) {
            throw new DiasPrevisaoInvalidoException("O numero de dias deve ser maior que zero");
        }
        validarLambdas(lambdasDiarios);

        LocalDate hoje = LocalDate.now();
        Map<TipoSanguineo, Long> estoquePorTipo = iniciarContagemPorTipo();
        List<Bolsa> listaBolsas = bolsas != null ? bolsas : List.of();

        listaBolsas.stream()
                .filter(bolsa -> bolsa.getStatus() == StatusBolsa.DISPONIVEL)
                .filter(bolsa -> bolsa.getTipoSanguineo() != null)
                .filter(bolsa -> bolsa.getDataValidade() != null)
                .filter(bolsa -> !bolsa.getDataValidade().isBefore(hoje))
                .forEach(bolsa -> estoquePorTipo.merge(
                        bolsa.getTipoSanguineo(), 1L, Long::sum));

        List<RiscoDesabastecimentoResponse.RiscoPorTipo> riscos = Arrays.stream(TipoSanguineo.values())
                .map(tipo -> {
                    long estoque = estoquePorTipo.get(tipo);
                    double lambdaDiario = lambdasDiarios.get(tipo);
                    double probabilidade = probabilidadePoissonMaiorQue(
                            estoque, lambdaDiario * dias);
                    return new RiscoDesabastecimentoResponse.RiscoPorTipo(
                            tipo,
                            estoque,
                            lambdaDiario,
                            arredondarProbabilidade(probabilidade));
                })
                .sorted(Comparator
                        .comparingDouble(RiscoDesabastecimentoResponse.RiscoPorTipo
                                ::probabilidadeDesabastecimento)
                        .reversed()
                        .thenComparing(risco -> risco.tipoSanguineo().name()))
                .toList();

        return new RiscoDesabastecimentoResponse(dias, LocalDateTime.now(), riscos);
    }

    private void validarLambdas(Map<TipoSanguineo, Double> lambdasDiarios) {
        if (lambdasDiarios == null) {
            throw new ModeloProbabilisticoIndisponivelException(
                    "Modelo de demanda ainda nao foi configurado");
        }

        for (TipoSanguineo tipo : TipoSanguineo.values()) {
            Double lambda = lambdasDiarios.get(tipo);
            if (lambda == null || !Double.isFinite(lambda) || lambda < 0.0) {
                throw new ModeloProbabilisticoIndisponivelException(
                        "Lambda diario ausente ou invalido para " + tipo);
            }
        }
    }

    private double probabilidadePoissonMaiorQue(long estoque, double mediaPeriodo) {
        if (mediaPeriodo == 0.0) {
            return 0.0;
        }

        if (!Double.isFinite(mediaPeriodo)) {
            throw new ModeloProbabilisticoIndisponivelException("Media do periodo excede o limite numerico");
        }
        // Soma em escala logaritmica para evitar underflow quando a media e alta.
        double logTermo = -mediaPeriodo;
        double logAcumulada = logTermo;
        for (long k = 1; k <= estoque; k++) {
            logTermo += Math.log(mediaPeriodo) - Math.log(k);
            double maior = Math.max(logAcumulada, logTermo);
            logAcumulada = maior + Math.log1p(Math.exp(-Math.abs(logAcumulada - logTermo)));
        }
        return Math.max(0.0, Math.min(1.0, -Math.expm1(logAcumulada)));
    }

    private double arredondarProbabilidade(double valor) {
        return Math.round(valor * 1_000_000.0) / 1_000_000.0;
    }

    /**
     * Sobrecarga de compatibilidade com a assinatura legada de testes que utiliza {@link AtendimentoSintetico}.
     */
    public IndicadoresResponse calcular(
            int diasProximoVencimento,
            List<Bolsa> bolsas,
            List<AtendimentoSintetico> atendimentos
    ) {
        if (diasProximoVencimento < 0) {
            throw new IllegalArgumentException("A janela de vencimento não pode ser negativa");
        }

        LocalDate hoje = LocalDate.now();
        LocalDate dataLimite = hoje.plusDays(diasProximoVencimento);

        List<Bolsa> listaBolsas = bolsas != null ? bolsas : List.of();
        List<Bolsa> bolsasDisponiveisValidas = listaBolsas.stream()
                .filter(bolsa -> bolsa.getStatus() == StatusBolsa.DISPONIVEL)
                .filter(bolsa -> bolsa.getDataValidade() != null)
                .filter(bolsa -> !bolsa.getDataValidade().isBefore(hoje))
                .toList();

        Map<TipoSanguineo, Long> estoquePorTipo = iniciarContagemPorTipo();
        for (Bolsa bolsa : bolsasDisponiveisValidas) {
            if (bolsa.getTipoSanguineo() != null) {
                estoquePorTipo.merge(bolsa.getTipoSanguineo(), 1L, Long::sum);
            }
        }

        List<Long> quantidadesPorTipo = new ArrayList<>(estoquePorTipo.values());
        boolean semEstoque = bolsasDisponiveisValidas.isEmpty();
        double media = semEstoque ? 0.0 : quantidadesPorTipo.stream().mapToLong(Long::longValue).average().orElse(0.0);
        double mediana = semEstoque ? 0.0 : calcularMediana(quantidadesPorTipo);
        long minimo = semEstoque ? 0L : quantidadesPorTipo.stream().mapToLong(Long::longValue).min().orElse(0L);
        long maximo = semEstoque ? 0L : quantidadesPorTipo.stream().mapToLong(Long::longValue).max().orElse(0L);
        double desvioPadrao = semEstoque ? 0.0 : calcularDesvioPadraoPopulacional(quantidadesPorTipo, media);

        long proximasDoVencimento = bolsasDisponiveisValidas.stream()
                .filter(bolsa -> !bolsa.getDataValidade().isAfter(dataLimite))
                .count();

        double percentualProximoDoVencimento = semEstoque
                ? 0.0
                : proximasDoVencimento * 100.0 / bolsasDisponiveisValidas.size();

        Map<StatusRequisicao, Long> requisicoesPorStatus = iniciarContagemPorStatus();
        List<AtendimentoSintetico> listaAtendimentos = atendimentos != null ? atendimentos : List.of();
        for (AtendimentoSintetico atendimento : listaAtendimentos) {
            if (atendimento.status() != null) {
                requisicoesPorStatus.merge(atendimento.status(), 1L, Long::sum);
            }
        }

        List<Long> temposDeAtendimento = listaAtendimentos.stream()
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
                listaAtendimentos.size(),
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

    private Long extrairTempoAtendimentoEmMinutos(Requisicao req) {
        if (req == null || req.getDataSolicitacao() == null) {
            return null;
        }
        if (req.getRota() != null && req.getRota().getDataChegadaPrevista() != null) {
            long minutos = Duration.between(req.getDataSolicitacao(), req.getRota().getDataChegadaPrevista()).toMinutes();
            return Math.max(0L, minutos);
        }
        if (req.getRota() != null && req.getRota().getDataSaida() != null) {
            long minutos = Duration.between(req.getDataSolicitacao(), req.getRota().getDataSaida()).toMinutes();
            return Math.max(0L, minutos);
        }
        return null;
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
        if (valores == null || valores.isEmpty()) {
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
        if (valores == null || valores.isEmpty()) {
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
