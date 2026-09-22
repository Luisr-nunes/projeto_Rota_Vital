package com.hemorede.relatorio;

import com.hemorede.dto.RelatorioHistoricoResponse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Operação escolhida para a entrega de SO (concorrência): agregação
 * estatística do histórico nacional de requisições, o mesmo cálculo que
 * hoje o {@code IndicadoresService} faz numa amostra pequena, aqui
 * escalado para centenas de milhares/milhões de registros e oferecido em
 * duas versões que devolvem <b>exatamente o mesmo</b>
 * {@link RelatorioHistoricoResponse.Resultado}; a diferença entre elas é
 * só quanto tempo levam. Compila e roda em Java 17 (a versão do projeto),
 * sem depender de nenhuma API do Java 21.
 *
 * <ul>
 *   <li>{@link #processarSequencial(List)}, uma única passada pelos n
 *       registros, O(n), numa thread só.</li>
 *   <li>{@link #processarComThreads(List, int)}, divide os n registros em
 *       {@code threads} fatias contíguas e independentes, processa cada
 *       fatia num pool de threads de plataforma ({@link ExecutorService}),
 *       e funde os acumuladores parciais no final.</li>
 * </ul>
 *
 * <p>A comparação opcional com virtual threads (Java 21) fica fora deste
 * serviço, isolada em {@code scripts/virtual-threads/}, ver o README lá
 * para como compilar e rodar só aquela parte com um JDK 21, sem exigir
 * Java 21 para o resto do projeto.</p>
 */
@Service
public class RelatorioHistoricoService {

    public RelatorioHistoricoResponse.Resultado processarSequencial(List<RegistroHistoricoRequisicao> registros) {
        AgregadoHistorico agregado = AgregadoHistorico.vazio();
        for (RegistroHistoricoRequisicao registro : registros) {
            agregado.acumular(registro);
        }
        return agregado.paraResultado();
    }

    public RelatorioHistoricoResponse.Resultado processarComThreads(
            List<RegistroHistoricoRequisicao> registros, int numeroDeThreads) {
        ExecutorService executor = Executors.newFixedThreadPool(numeroDeThreads);
        return processarParticionado(registros, numeroDeThreads, executor);
    }

    private RelatorioHistoricoResponse.Resultado processarParticionado(
            List<RegistroHistoricoRequisicao> registros, int numeroDeFatias, ExecutorService executor) {
        if (numeroDeFatias < 1) {
            throw new IllegalArgumentException("O número de fatias/threads deve ser >= 1");
        }
        try {
            List<List<RegistroHistoricoRequisicao>> fatias = particionar(registros, numeroDeFatias);

            List<Callable<AgregadoHistorico>> tarefas = new ArrayList<>(fatias.size());
            for (List<RegistroHistoricoRequisicao> fatia : fatias) {
                tarefas.add(() -> {
                    AgregadoHistorico parcial = AgregadoHistorico.vazio();
                    for (RegistroHistoricoRequisicao registro : fatia) {
                        parcial.acumular(registro);
                    }
                    return parcial;
                });
            }

            // invokeAll só retorna depois que TODAS as tarefas terminaram, o join
            // implícito que garante que a fusão abaixo só acontece com os parciais
            // completos, sem condição de corrida entre "ler o resultado" e "a thread
            // ainda estar escrevendo nele".
            List<Future<AgregadoHistorico>> futuros = executor.invokeAll(tarefas);

            AgregadoHistorico total = AgregadoHistorico.vazio();
            for (Future<AgregadoHistorico> futuro : futuros) {
                total.mesclar(futuro.get());
            }
            return total.paraResultado();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Processamento do histórico interrompido", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Falha ao processar uma fatia do histórico", e.getCause());
        } finally {
            executor.shutdown();
        }
    }

    /**
     * Divide {@code registros} em até {@code numeroDeFatias} sublistas
     * contíguas, de tamanho o mais equilibrado possível (as primeiras
     * fatias recebem um registro a mais quando a divisão não é exata) e
     * sem sobreposição, cada registro pertence a exatamente uma fatia, o
     * que é o que garante que não existe estado compartilhado entre as
     * tarefas durante o processamento. Não copia elementos: cada fatia é
     * uma view ({@link List#subList}) sobre a lista original, só de
     * leitura durante o cálculo.
     */
    private List<List<RegistroHistoricoRequisicao>> particionar(
            List<RegistroHistoricoRequisicao> registros, int numeroDeFatias) {
        int total = registros.size();
        int fatiasReais = Math.max(1, Math.min(numeroDeFatias, Math.max(total, 1)));
        int tamanhoBase = total / fatiasReais;
        int resto = total % fatiasReais;

        List<List<RegistroHistoricoRequisicao>> fatias = new ArrayList<>(fatiasReais);
        int inicio = 0;
        for (int i = 0; i < fatiasReais; i++) {
            int tamanhoFatia = tamanhoBase + (i < resto ? 1 : 0);
            int fim = inicio + tamanhoFatia;
            fatias.add(registros.subList(inicio, fim));
            inicio = fim;
        }
        return fatias;
    }
}
