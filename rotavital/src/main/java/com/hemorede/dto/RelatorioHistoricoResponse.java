package com.hemorede.dto;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.Prioridade;
import com.hemorede.domain.enums.StatusRequisicao;
import com.hemorede.domain.enums.TipoSanguineo;

import java.util.Map;

/**
 * Resposta do relatório nacional de estatísticas do histórico de
 * requisições, o endpoint real da entrega de SO/concorrência.
 *
 * <p>{@code resultado} é a parte que precisa ser <b>idêntica</b> entre a
 * versão sequencial e a versão com threads (é o que os testes de
 * corretude comparam). Os demais campos são metadados da execução,
 * tamanho da entrada, modo e número de threads usados, tempo medido, e
 * mudam a cada chamada por natureza, não entram nessa comparação.</p>
 */
public record RelatorioHistoricoResponse(
        int tamanhoHistorico,
        String modo,
        int threadsUtilizadas,
        long tempoProcessamentoMs,
        long tempoProcessamentoNs,
        Resultado resultado
) {

    /**
     * O resultado estatístico em si: contagens e agregados sobre o
     * histórico, todos calculados a partir de reduções associativas (soma,
     * contagem, mínimo, máximo), por isso dá para particionar os dados,
     * processar cada fatia isoladamente e só depois somar os parciais,
     * chegando sempre ao mesmo resultado, em qualquer ordem de fusão.
     */
    public record Resultado(
            long totalRegistros,
            Map<StatusRequisicao, Long> porStatus,
            Map<TipoSanguineo, Long> porTipoSanguineo,
            Map<HemoComponente, Long> porHemoComponente,
            Map<Prioridade, Long> porPrioridade,
            TempoAtendimentoResumo tempoAtendimento,
            long requisicoesUrgentesPendentes
    ) {
    }

    public record TempoAtendimentoResumo(
            long registrosComTempo,
            long somaMinutos,
            double mediaMinutos,
            long minimoMinutos,
            long maximoMinutos
    ) {
    }
}
