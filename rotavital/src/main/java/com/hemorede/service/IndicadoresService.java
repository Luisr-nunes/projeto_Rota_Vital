package com.hemorede.service;

import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.StatusRequisicao;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.domain.model.Requisicao;
import com.hemorede.dto.IndicadoresResponse;
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

    @Autowired
    public IndicadoresService(BolsaRepository bolsaRepository, RequisicaoRepository requisicaoRepository) {
        this.bolsaRepository = bolsaRepository;
        this.requisicaoRepository = requisicaoRepository;
    }

    public IndicadoresService() {
        this(null, null);
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
