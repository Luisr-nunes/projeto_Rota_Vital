package com.hemorede.relatorio;

import com.hemorede.dto.RelatorioHistoricoResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * Comparação OPCIONAL (item "para destaque" da entrega de SO): a mesma
 * operação de {@link RelatorioHistoricoService}, agregação do histórico
 * de requisições, rodando com <b>virtual threads do Java 21</b>, para
 * comparar com as threads de plataforma.
 *
 * <p><b>Por que este arquivo fica fora de {@code src/main/java}:</b> o
 * professor confirmou que o Java 21 é opcional, só para essa comparação,
 * o resto do projeto (a entrega em si, o CI, o build normal com
 * {@code mvn package}) continua em Java 17. Se esta classe estivesse em
 * {@code src/main/java}, o {@code Executors.newVirtualThreadPerTaskExecutor()}
 * (API que só existe no Java 21) quebraria o build normal do projeto, que
 * roda com {@code --release 17}. Por isso ela vive só aqui, num diretório
 * à parte, compilada e executada manualmente com um JDK 21, nunca entra
 * no {@code mvn compile}/{@code mvn test}/{@code mvn package} nem no CI.</p>
 *
 * <p>Está no mesmo pacote {@code com.hemorede.relatorio} do serviço para
 * poder reaproveitar {@link AgregadoHistorico} (pacote-privado) e
 * {@link GeradorHistoricoRequisicoes} sem duplicar nada além da divisão em
 * fatias, a mesma lógica de {@code RelatorioHistoricoService
 * #processarComThreads}, só trocando o {@link ExecutorService} de um pool
 * fixo de threads de plataforma por
 * {@code Executors.newVirtualThreadPerTaskExecutor()}.</p>
 *
 * <h2>Como compilar e rodar (precisa de um JDK 21 instalado)</h2>
 * <pre>
 * cd rotavital
 * mvn -DskipTests package                     # gera target/classes com o resto do projeto (Java 17)
 *
 * javac --release 21 -cp target/classes \
 *     -d /tmp/vt-out \
 *     scripts/virtual-threads/src/com/hemorede/relatorio/ComparacaoVirtualThreads.java
 *
 * java --class-path "target/classes:/tmp/vt-out" \
 *     com.hemorede.relatorio.ComparacaoVirtualThreads 100000 1000000
 * </pre>
 *
 * O programa gera os históricos sintéticos (mesma semente/determinismo do
 * {@link GeradorHistoricoRequisicoes} usado pelo endpoint), roda
 * sequencial, threads de plataforma (4 e 8) e virtual threads (8 e 64
 * fatias), confere que todos batem byte a byte com o resultado sequencial
 * e imprime os tempos. Cada configuração passa por algumas chamadas de
 * aquecimento (JIT/GC) descartadas antes de {@code REPETICOES} chamadas
 * medidas, reportando a mediana, mesma metodologia do benchmark HTTP em
 * Python (evita números artificialmente altos por causa de JIT tier 0).
 */
public final class ComparacaoVirtualThreads {

    private ComparacaoVirtualThreads() {
    }

    private static final int AQUECIMENTO = 8;
    private static final int REPETICOES = 11;

    public static void main(String[] args) throws Exception {
        System.setOut(new java.io.PrintStream(System.out, true, java.nio.charset.StandardCharsets.UTF_8));
        int[] tamanhos = args.length > 0
                ? java.util.Arrays.stream(args).mapToInt(Integer::parseInt).toArray()
                : new int[]{100_000, 1_000_000};

        GeradorHistoricoRequisicoes gerador = new GeradorHistoricoRequisicoes();
        RelatorioHistoricoService servicoPadrao = new RelatorioHistoricoService();

        for (int tamanho : tamanhos) {
            System.out.println("\n=== tamanho=" + tamanho + " (aquecimento=" + AQUECIMENTO
                    + ", repeticoes=" + REPETICOES + ", mediana reportada) ===");
            List<RegistroHistoricoRequisicao> registros = gerador.obter(tamanho);

            RelatorioHistoricoResponse.Resultado referencia = aquecerEMedir(
                    "sequencial       ", () -> servicoPadrao.processarSequencial(registros));

            for (int threads : new int[]{4, 8}) {
                int t = threads;
                var resultado = aquecerEMedir(
                        "plataforma(" + t + ")   ",
                        () -> servicoPadrao.processarComThreads(registros, t));
                conferir(referencia, resultado, "plataforma(" + t + ")");
            }

            for (int fatias : new int[]{8, 64}) {
                int f = fatias;
                var resultado = aquecerEMedir(
                        "virtual(" + f + ")      ",
                        () -> processarComVirtualThreads(registros, f));
                conferir(referencia, resultado, "virtual(" + f + ")");
            }
        }

        System.out.println("\nTodas as versões bateram com a sequencial, sem race condition.");
    }

    /**
     * Mesmíssima lógica de particionamento e fusão de
     * {@code RelatorioHistoricoService#processarComThreads}, só trocando o
     * pool fixo de threads de plataforma por virtual threads, daí a
     * necessidade do Java 21 só aqui.
     */
    static RelatorioHistoricoResponse.Resultado processarComVirtualThreads(
            List<RegistroHistoricoRequisicao> registros, int numeroDeFatias) throws Exception {
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            int total = registros.size();
            int fatiasReais = Math.max(1, Math.min(numeroDeFatias, Math.max(total, 1)));
            int tamanhoBase = total / fatiasReais;
            int resto = total % fatiasReais;

            List<Callable<AgregadoHistorico>> tarefas = new ArrayList<>(fatiasReais);
            int inicio = 0;
            for (int i = 0; i < fatiasReais; i++) {
                int tamanhoFatia = tamanhoBase + (i < resto ? 1 : 0);
                int fim = inicio + tamanhoFatia;
                List<RegistroHistoricoRequisicao> fatia = registros.subList(inicio, fim);
                tarefas.add(() -> {
                    AgregadoHistorico parcial = AgregadoHistorico.vazio();
                    for (RegistroHistoricoRequisicao registro : fatia) {
                        parcial.acumular(registro);
                    }
                    return parcial;
                });
                inicio = fim;
            }

            List<Future<AgregadoHistorico>> futuros = executor.invokeAll(tarefas);
            AgregadoHistorico totalAgregado = AgregadoHistorico.vazio();
            for (Future<AgregadoHistorico> futuro : futuros) {
                totalAgregado.mesclar(futuro.get());
            }
            return totalAgregado.paraResultado();
        } finally {
            executor.shutdown();
        }
    }

    private interface Operacao {
        RelatorioHistoricoResponse.Resultado executar() throws Exception;
    }

    /**
     * Roda {@code AQUECIMENTO} chamadas descartadas (para o JIT compilar os
     * hot paths e a JVM estabilizar, igual ao {@code aquecer()} do benchmark
     * HTTP em Python) e depois {@code REPETICOES} chamadas medidas,
     * reportando a mediana em ms. Uma única chamada fria é dominada por
     * interpretação/JIT tier 0 e GC inicial, não pelo custo real da
     * operação, por isso não é usada isoladamente.
     */
    private static RelatorioHistoricoResponse.Resultado aquecerEMedir(String rotulo, Operacao operacao) throws Exception {
        RelatorioHistoricoResponse.Resultado ultimoResultado = null;
        for (int i = 0; i < AQUECIMENTO; i++) {
            ultimoResultado = operacao.executar();
        }
        System.gc();

        List<Long> temposMs = new ArrayList<>(REPETICOES);
        for (int i = 0; i < REPETICOES; i++) {
            long inicio = System.nanoTime();
            ultimoResultado = operacao.executar();
            temposMs.add((System.nanoTime() - inicio) / 1_000_000);
        }

        long mediana = mediana(temposMs);
        System.out.printf("  %s: %d ms (mediana de %d medições: %s)%n",
                rotulo, mediana, REPETICOES,
                temposMs.stream().map(String::valueOf).collect(Collectors.joining(", ")));
        return ultimoResultado;
    }

    private static long mediana(List<Long> valores) {
        List<Long> ordenados = new ArrayList<>(valores);
        ordenados.sort(Long::compareTo);
        int meio = ordenados.size() / 2;
        if (ordenados.size() % 2 == 0) {
            return (ordenados.get(meio - 1) + ordenados.get(meio)) / 2;
        }
        return ordenados.get(meio);
    }

    private static void conferir(
            RelatorioHistoricoResponse.Resultado referencia,
            RelatorioHistoricoResponse.Resultado obtido,
            String rotulo) {
        if (!referencia.equals(obtido)) {
            throw new AssertionError("Resultado de " + rotulo + " divergiu da versão sequencial!");
        }
    }
}
