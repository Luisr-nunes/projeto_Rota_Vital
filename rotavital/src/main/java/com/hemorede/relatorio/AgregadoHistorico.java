package com.hemorede.relatorio;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.Prioridade;
import com.hemorede.domain.enums.StatusRequisicao;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.dto.RelatorioHistoricoResponse;

import java.util.EnumMap;
import java.util.Map;

/**
 * Acumulador mutável de uma fatia do histórico.
 *
 * <p>Cada thread, ou, na versão sequencial, a única execução, trabalha
 * sobre a sua <b>própria</b> instância, criada localmente e nunca
 * publicada para outra thread durante o processamento. Não há nenhum
 * campo compartilhado, nenhum {@code synchronized}, nenhum lock: a única
 * seção que toca estado "compartilhado" é {@link #mesclar}, chamado pela
 * thread que orquestra o cálculo (a que fez {@code executor.invokeAll} e
 * está lendo os {@code Future}s), depois que todas as fatias já
 * terminaram, ou seja, sequencialmente, sem concorrência de fato.</p>
 *
 * <p>Os acumuladores (soma, contagem, min, max, contagem por categoria)
 * são todos <b>associativos e comutativos</b>: mesclar os parciais em
 * qualquer ordem dá sempre o mesmo total. É essa propriedade que garante
 * que a versão com threads devolve exatamente a mesma resposta que a
 * versão sequencial, e que evita qualquer race condition.</p>
 */
final class AgregadoHistorico {

    private final Map<StatusRequisicao, Long> porStatus = new EnumMap<>(StatusRequisicao.class);
    private final Map<TipoSanguineo, Long> porTipoSanguineo = new EnumMap<>(TipoSanguineo.class);
    private final Map<HemoComponente, Long> porHemoComponente = new EnumMap<>(HemoComponente.class);
    private final Map<Prioridade, Long> porPrioridade = new EnumMap<>(Prioridade.class);

    private long total = 0;
    private long comTempoAtendimento = 0;
    private long somaTempoAtendimentoMinutos = 0;
    private long minimoTempoAtendimentoMinutos = Long.MAX_VALUE;
    private long maximoTempoAtendimentoMinutos = Long.MIN_VALUE;
    private long urgentesPendentes = 0;

    static AgregadoHistorico vazio() {
        return new AgregadoHistorico();
    }

    /** Incorpora um registro no acumulador. O(1) amortizado. */
    void acumular(RegistroHistoricoRequisicao registro) {
        total++;
        porStatus.merge(registro.status(), 1L, Long::sum);
        porTipoSanguineo.merge(registro.tipoSanguineo(), 1L, Long::sum);
        porHemoComponente.merge(registro.hemoComponente(), 1L, Long::sum);
        porPrioridade.merge(registro.prioridade(), 1L, Long::sum);

        if (registro.status() == StatusRequisicao.PENDENTE && registro.prioridade() == Prioridade.URGENTE) {
            urgentesPendentes++;
        }

        Long tempo = registro.tempoAtendimentoMinutos();
        if (tempo != null) {
            comTempoAtendimento++;
            somaTempoAtendimentoMinutos += tempo;
            if (tempo < minimoTempoAtendimentoMinutos) {
                minimoTempoAtendimentoMinutos = tempo;
            }
            if (tempo > maximoTempoAtendimentoMinutos) {
                maximoTempoAtendimentoMinutos = tempo;
            }
        }
    }

    /** Combina o acumulador de outra fatia neste. Operação associativa e comutativa. */
    void mesclar(AgregadoHistorico outro) {
        total += outro.total;
        outro.porStatus.forEach((chave, valor) -> porStatus.merge(chave, valor, Long::sum));
        outro.porTipoSanguineo.forEach((chave, valor) -> porTipoSanguineo.merge(chave, valor, Long::sum));
        outro.porHemoComponente.forEach((chave, valor) -> porHemoComponente.merge(chave, valor, Long::sum));
        outro.porPrioridade.forEach((chave, valor) -> porPrioridade.merge(chave, valor, Long::sum));

        urgentesPendentes += outro.urgentesPendentes;
        comTempoAtendimento += outro.comTempoAtendimento;
        somaTempoAtendimentoMinutos += outro.somaTempoAtendimentoMinutos;

        if (outro.minimoTempoAtendimentoMinutos < minimoTempoAtendimentoMinutos) {
            minimoTempoAtendimentoMinutos = outro.minimoTempoAtendimentoMinutos;
        }
        if (outro.maximoTempoAtendimentoMinutos > maximoTempoAtendimentoMinutos) {
            maximoTempoAtendimentoMinutos = outro.maximoTempoAtendimentoMinutos;
        }
    }

    RelatorioHistoricoResponse.Resultado paraResultado() {
        double media = comTempoAtendimento == 0
                ? 0.0
                : arredondar((double) somaTempoAtendimentoMinutos / comTempoAtendimento);

        var tempoAtendimento = new RelatorioHistoricoResponse.TempoAtendimentoResumo(
                comTempoAtendimento,
                somaTempoAtendimentoMinutos,
                media,
                comTempoAtendimento == 0 ? 0L : minimoTempoAtendimentoMinutos,
                comTempoAtendimento == 0 ? 0L : maximoTempoAtendimentoMinutos
        );

        return new RelatorioHistoricoResponse.Resultado(
                total,
                Map.copyOf(porStatus),
                Map.copyOf(porTipoSanguineo),
                Map.copyOf(porHemoComponente),
                Map.copyOf(porPrioridade),
                tempoAtendimento,
                urgentesPendentes
        );
    }

    private static double arredondar(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
