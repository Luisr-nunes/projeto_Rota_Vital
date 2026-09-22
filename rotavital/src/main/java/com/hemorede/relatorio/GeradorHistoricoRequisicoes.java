package com.hemorede.relatorio;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.Prioridade;
import com.hemorede.domain.enums.StatusRequisicao;
import com.hemorede.domain.enums.TipoSanguineo;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gera (e mantém em cache) massas de dados sintéticas que simulam o
 * histórico nacional de requisições de hemocomponentes do Rota Vital, para
 * exercitar o relatório de estatísticas em escala (100 mil, 1 milhão de
 * registros, etc.).
 *
 * <p>A geração é determinística (semente fixa por {@code tamanho}), então o
 * mesmo {@code tamanho} produz sempre exatamente os mesmos registros, é
 * essa determinação que permite comparar, campo a campo, a resposta da
 * versão sequencial com a da versão paralela do relatório e afirmar que
 * elas são idênticas.</p>
 *
 * <p>O cache evita que o custo de <b>gerar</b> a massa entre nas medições
 * de desempenho do endpoint: a primeira chamada para um {@code tamanho}
 * paga esse custo uma única vez; as chamadas seguintes (as usadas no
 * benchmark) reaproveitam a mesma lista em memória, então o tempo medido no
 * endpoint reflete só o processamento (sequencial, com threads ou com
 * threads virtuais).</p>
 */
@Component
public class GeradorHistoricoRequisicoes {

    private static final long SEMENTE_BASE = 42L;

    private final Map<Integer, List<RegistroHistoricoRequisicao>> cache = new ConcurrentHashMap<>();

    /**
     * Retorna o histórico sintético de {@code tamanho} registros, gerando-o
     * (e guardando em cache) na primeira chamada para esse tamanho.
     */
    public List<RegistroHistoricoRequisicao> obter(int tamanho) {
        if (tamanho <= 0) {
            throw new IllegalArgumentException("O tamanho do histórico deve ser positivo");
        }
        return cache.computeIfAbsent(tamanho, this::gerar);
    }

    /** Limpa o cache, útil em testes para forçar uma nova geração. */
    public void limparCache() {
        cache.clear();
    }

    private List<RegistroHistoricoRequisicao> gerar(int tamanho) {
        Random random = new Random(SEMENTE_BASE + tamanho);
        StatusRequisicao[] status = StatusRequisicao.values();
        TipoSanguineo[] tipos = TipoSanguineo.values();
        HemoComponente[] componentes = HemoComponente.values();
        Prioridade[] prioridades = Prioridade.values();

        List<RegistroHistoricoRequisicao> registros = new ArrayList<>(tamanho);
        for (int i = 0; i < tamanho; i++) {
            StatusRequisicao statusSorteado = status[random.nextInt(status.length)];
            boolean atendida = statusSorteado == StatusRequisicao.APROVADA
                    || statusSorteado == StatusRequisicao.EM_ROTA
                    || statusSorteado == StatusRequisicao.ENTREGUE;
            Long tempoAtendimento = atendida ? (long) (5 + random.nextInt(600)) : null;

            registros.add(new RegistroHistoricoRequisicao(
                    i,
                    statusSorteado,
                    tipos[random.nextInt(tipos.length)],
                    componentes[random.nextInt(componentes.length)],
                    prioridades[random.nextInt(prioridades.length)],
                    tempoAtendimento
            ));
        }
        return List.copyOf(registros);
    }
}
