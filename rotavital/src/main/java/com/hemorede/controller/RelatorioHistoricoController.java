package com.hemorede.controller;

import com.hemorede.dto.RelatorioHistoricoResponse;
import com.hemorede.relatorio.GeradorHistoricoRequisicoes;
import com.hemorede.relatorio.RegistroHistoricoRequisicao;
import com.hemorede.relatorio.RelatorioHistoricoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

/**
 * Endpoint real da entrega de SO/concorrência: relatório nacional de
 * estatísticas do histórico de requisições (contagens por status, tipo
 * sanguíneo, hemocomponente e prioridade, mais estatísticas do tempo de
 * atendimento), disponível em dois modos de processamento do mesmo
 * cálculo, sequencial e com threads de plataforma. Compila e roda em
 * Java 17, a versão do projeto (a comparação opcional com virtual threads
 * do Java 21 é um script separado, ver {@code scripts/virtual-threads/}).
 *
 * <p>Exemplos:</p>
 * <pre>
 * GET /api/v1/relatorios/historico?tamanho=1000000&amp;modo=sequencial
 * GET /api/v1/relatorios/historico?tamanho=1000000&amp;modo=paralelo&amp;threads=8
 * </pre>
 */
@RestController
@RequestMapping("/api/v1/relatorios/historico")
public class RelatorioHistoricoController {

    private static final int TAMANHO_MAXIMO = 5_000_000;
    private static final int THREADS_MAXIMO = 4096;

    private final GeradorHistoricoRequisicoes gerador;
    private final RelatorioHistoricoService service;

    public RelatorioHistoricoController(GeradorHistoricoRequisicoes gerador, RelatorioHistoricoService service) {
        this.gerador = gerador;
        this.service = service;
    }

    @GetMapping
    public RelatorioHistoricoResponse gerar(
            @RequestParam(defaultValue = "100000") int tamanho,
            @RequestParam(defaultValue = "sequencial") String modo,
            @RequestParam(defaultValue = "1") int threads) {

        if (tamanho <= 0 || tamanho > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException("tamanho deve estar entre 1 e " + TAMANHO_MAXIMO);
        }

        // Fora da janela medida: só entra no tempo de resposta na primeira
        // chamada para cada tamanho (ver GeradorHistoricoRequisicoes).
        List<RegistroHistoricoRequisicao> registros = gerador.obter(tamanho);

        long inicioNanos = System.nanoTime();
        RelatorioHistoricoResponse.Resultado resultado;
        int threadsUtilizadas;

        switch (modo.toLowerCase(Locale.ROOT)) {
            case "sequencial" -> {
                resultado = service.processarSequencial(registros);
                threadsUtilizadas = 1;
            }
            case "paralelo", "threads" -> {
                validarThreads(threads);
                resultado = service.processarComThreads(registros, threads);
                threadsUtilizadas = threads;
            }
            default -> throw new IllegalArgumentException(
                    "modo inválido: '" + modo + "' (use sequencial ou paralelo)");
        }

        long tempoProcessamentoNanos = System.nanoTime() - inicioNanos;
        long tempoProcessamentoMs = tempoProcessamentoNanos / 1_000_000;

        return new RelatorioHistoricoResponse(
                tamanho, modo, threadsUtilizadas, tempoProcessamentoMs, tempoProcessamentoNanos, resultado);
    }

    private void validarThreads(int threads) {
        if (threads < 1 || threads > THREADS_MAXIMO) {
            throw new IllegalArgumentException("threads deve estar entre 1 e " + THREADS_MAXIMO);
        }
    }
}
