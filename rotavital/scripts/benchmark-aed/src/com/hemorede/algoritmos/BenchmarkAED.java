package com.hemorede.algoritmos;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Supplier;
import java.util.function.ToLongFunction;

/**
 * Medicao empirica (PI2 · L1, relatorio U2) das estruturas de AED do Rota Vital com 1 mil,
 * 10 mil e 100 mil bolsas: {@link MatrizCompatibilidade}, {@link FilaFEFO}, {@link IndiceEstoque},
 * o fluxo de matching/alocacao da HU08 e {@link GrafoRotas} (Dijkstra).
 *
 * <p>Fica <b>fora</b> de {@code src/main/java} de proposito (mesma ideia de
 * {@code scripts/virtual-threads}): nao entra no {@code mvn package} nem no CI. Usa apenas a API
 * publica das classes reais, compiladas em {@code target/classes}. Veja o README desta pasta.</p>
 *
 * <p><b>Protocolo.</b> Cada medicao roda {@code aquecimento} execucoes descartadas (o JIT compila
 * os caminhos quentes) e depois {@code medicoes} execucoes cronometradas com
 * {@link System#nanoTime()}; reporta mediana, p95 e minimo. O estado mutavel (fila, indice) e
 * recriado a cada execucao <i>fora</i> do trecho cronometrado. Os resultados de cada execucao
 * alimentam um acumulador {@code volatile}, para o JIT nao eliminar o trabalho. Dados sinteticos
 * com semente fixa, para reproduzir.</p>
 *
 * <p><b>Matching/alocacao.</b> {@code AlocacaoService.alocar} depende de banco e do Spring, e o seu
 * {@code alocarProximaCompativel} e privado. Aqui as mesmas etapas sao repetidas passo a passo
 * sobre as classes publicas reais (filtro por validade e compatibilidade, indexacao em
 * {@link IndiceEstoque}, escolha FEFO entre os doadores de {@link MatrizCompatibilidade#matching}).
 * Fica de fora so o acesso ao banco.</p>
 */
public final class BenchmarkAED {

    private static final int[] TAMANHOS = {1_000, 10_000, 100_000};
    private static final TipoSanguineo RECEPTOR = TipoSanguineo.A_POS;
    private static final int DIAS_VALIDADE = 42; // validade do concentrado de hemacias
    private static final long SEMENTE = 42L;

    private static final Comparator<Bolsa> ORDEM_FEFO = Comparator
            .comparing(Bolsa::getDataValidade)
            .thenComparing(Bolsa::getDataColeta)
            .thenComparing(Bolsa::getId);

    private static int aquecimento = 8;
    private static int medicoes = 31;
    private static volatile long sorvedouro;

    /** Uma linha de resultado. Tempos por execucao (ja divididos pelas repeticoes internas). */
    private record Resultado(String grupo, String operacao, int n, int ops,
                             double medianaMs, double p95Ms, double minMs) {
        double nsPorOp() {
            return medianaMs * 1e6 / ops;
        }
    }

    private static final List<Resultado> RESULTADOS = new ArrayList<>();

    private BenchmarkAED() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length > 0) {
            aquecimento = Integer.parseInt(args[0]);
        }
        if (args.length > 1) {
            medicoes = Integer.parseInt(args[1]);
        }

        validarCorretude();

        MatrizCompatibilidade matriz = new MatrizCompatibilidade();
        medir("Matriz ABO/Rh 8x8", "construir a matriz (uma vez)", 8, 1, 10_000,
                () -> null, x -> new MatrizCompatibilidade().compativel(TipoSanguineo.O_NEG, RECEPTOR) ? 1 : 0);

        for (int n : TAMANHOS) {
            List<Bolsa> bolsas = gerarBolsas(n);
            medirMatriz(matriz, bolsas, n);
            medirFefo(bolsas, n);
            medirIndice(bolsas, n);
            medirMatching(matriz, bolsas, n);
            System.gc();
        }

        medirDijkstra();

        imprimirMarkdown();
        salvarCsv();
    }

    // ------------------------------------------------------------------ matriz

    private static void medirMatriz(MatrizCompatibilidade matriz, List<Bolsa> bolsas, int n) {
        String g = "Matriz ABO/Rh 8x8";
        TipoSanguineo[] tipos = TipoSanguineo.values();
        TipoSanguineo[] doadores = new TipoSanguineo[n];
        for (int i = 0; i < n; i++) {
            doadores[i] = bolsas.get(i).getTipoSanguineo();
        }
        // So a matriz: o receptor de cada chamada depende do resultado da anterior, para o JIT
        // nao conseguir eliminar, vetorizar ou reordenar as consultas.
        medir(g, "compativel(doador, receptor), n chamadas (so a matriz)", n, n, 1, () -> doadores, arr -> {
            long c = 0;
            int k = 0;
            for (int i = 0; i < arr.length; i++) {
                boolean ok = matriz.compativel(arr[i], tipos[k]);
                if (ok) {
                    c++;
                }
                k = (k + (ok ? 3 : 1)) & 7;
            }
            return c;
        });
        medir(g, "compativel sobre List<Bolsa> (inclui ler o tipo da bolsa)", n, n, 1, () -> bolsas, lista -> {
            long c = 0;
            for (Bolsa b : lista) {
                if (matriz.compativel(b.getTipoSanguineo(), RECEPTOR)) {
                    c++;
                }
            }
            return c;
        });
        medir(g, "matching(receptor), n chamadas", n, n, 1, () -> bolsas, lista -> {
            long s = 0;
            int k = 0;
            for (int i = 0; i < lista.size(); i++) {
                int tam = matriz.matching(tipos[k]).size();
                s += tam;
                k = (k + tam) & 7;
            }
            return s;
        });
    }

    // -------------------------------------------------------------------- FEFO

    private static void medirFefo(List<Bolsa> aleatorias, int n) {
        String g = "FilaFEFO (min-heap)";
        List<Bolsa> crescente = new ArrayList<>(aleatorias);
        crescente.sort(ORDEM_FEFO);
        List<Bolsa> decrescente = new ArrayList<>(crescente);
        java.util.Collections.reverse(decrescente);

        medir(g, "inserir n (ordem aleatoria)", n, n, 1, FilaFEFO::new, f -> inserirTodas(f, aleatorias));
        medir(g, "inserir n (melhor caso: ja em ordem FEFO)", n, n, 1, FilaFEFO::new, f -> inserirTodas(f, crescente));
        medir(g, "inserir n (pior caso: ordem FEFO inversa)", n, n, 1, FilaFEFO::new, f -> inserirTodas(f, decrescente));

        medir(g, "alocar n (esvaziar a fila)", n, n, 1, () -> filaCom(aleatorias), f -> {
            long s = 0;
            while (!f.estaVazia()) {
                s += f.alocar().getId();
            }
            return s;
        });

        // As 8 filas por tipo, como no IndiceEstoque. O indice da proxima fila depende do id lido
        // agora: sem isso o JIT tira o peek() do laco e o tempo cai a ~0 (foi o que aconteceu
        // na primeira versao desta medicao).
        IndiceEstoque porTipo = indiceCom(aleatorias);
        FilaFEFO[] filas = new FilaFEFO[8];
        for (TipoSanguineo t : TipoSanguineo.values()) {
            filas[t.ordinal()] = porTipo.consultarEstoque(t);
        }
        FilaFEFO fixa = filaCom(aleatorias);
        medir(g, "peek() encadeado em 8 filas, n chamadas", n, n, 1, () -> filas, fs -> {
            long s = 0;
            int k = 0;
            for (int i = 0; i < n; i++) {
                long id = fs[k].peek().getId();
                s += id;
                k = (int) ((k + id) & 7);
            }
            return s;
        });
        medir(g, "disponiveisOrdenadas()", n, n, 1, () -> fixa, f -> f.disponiveisOrdenadas().size());
        medir(g, "proximasAoVencimento(7)", n, n, 1, () -> fixa, f -> f.proximasAoVencimento(7).size());
    }

    private static long inserirTodas(FilaFEFO fila, List<Bolsa> bolsas) {
        for (Bolsa b : bolsas) {
            fila.inserir(b);
        }
        return fila.tamanho();
    }

    private static FilaFEFO filaCom(List<Bolsa> bolsas) {
        FilaFEFO fila = new FilaFEFO();
        for (Bolsa b : bolsas) {
            fila.inserir(b);
        }
        return fila;
    }

    // ------------------------------------------------------------------ indice

    private static void medirIndice(List<Bolsa> bolsas, int n) {
        String g = "IndiceEstoque (8 filas FEFO)";
        medir(g, "adicionarBolsa, n bolsas", n, n, 1, () -> bolsas, lista -> {
            IndiceEstoque indice = new IndiceEstoque();
            for (Bolsa b : lista) {
                indice.adicionarBolsa(b);
            }
            return indice.totalGeral();
        });

        IndiceEstoque pronto = indiceCom(bolsas);
        TipoSanguineo[] tipos = TipoSanguineo.values();
        medir(g, "totalDisponivelPorTipo(tipo), n chamadas", n, n, 1, () -> pronto, indice -> {
            long s = 0;
            int k = 0;
            for (int i = 0; i < n; i++) {
                int t = indice.totalDisponivelPorTipo(tipos[k]);
                s += t;
                k = (k + t + 1) & 7;
            }
            return s;
        });
        medir(g, "consultarEstoque(tipo), n chamadas", n, n, 1, () -> pronto, indice -> {
            long s = 0;
            int k = 0;
            for (int i = 0; i < n; i++) {
                int t = indice.consultarEstoque(tipos[k]).tamanho();
                s += t;
                k = (k + t + 1) & 7;
            }
            return s;
        });
    }

    private static IndiceEstoque indiceCom(List<Bolsa> bolsas) {
        IndiceEstoque indice = new IndiceEstoque();
        for (Bolsa b : bolsas) {
            indice.adicionarBolsa(b);
        }
        return indice;
    }

    // ---------------------------------------------------------------- matching

    private static void medirMatching(MatrizCompatibilidade matriz, List<Bolsa> bolsas, int n) {
        String g = "Matching/alocacao HU08 (sem banco)";
        LocalDate hoje = LocalDate.now();
        List<TipoSanguineo> doadores = matriz.matching(RECEPTOR);

        List<Bolsa> candidatas = filtrarCandidatas(matriz, bolsas, hoje);
        int q10 = 10;
        int qPct = Math.max(1, candidatas.size() / 10);

        medir(g, "1. filtrar validade + compatibilidade (N bolsas)", n, n, 1, () -> bolsas,
                lista -> filtrarCandidatas(matriz, lista, hoje).size());
        medir(g, "2. indexar as candidatas no IndiceEstoque", n, candidatas.size(), 1, () -> candidatas,
                lista -> indiceCom(lista).totalGeral());

        for (int q : new int[] {q10, qPct}) {
            String rotulo = q == q10 ? "q = 10" : "q = 10% das candidatas";
            medir(g, "3. alocar " + rotulo, n, q, 1, () -> indiceCom(candidatas), indice -> {
                long s = 0;
                for (int i = 0; i < q; i++) {
                    s += alocarProximaCompativel(indice, doadores).getId();
                }
                return s;
            });
            medir(g, "total (1+2+3), " + rotulo, n, q, 1, () -> bolsas, lista -> {
                IndiceEstoque indice = indiceCom(filtrarCandidatas(matriz, lista, hoje));
                long s = 0;
                for (int i = 0; i < q; i++) {
                    s += alocarProximaCompativel(indice, doadores).getId();
                }
                return s;
            });
        }
    }

    private static List<Bolsa> filtrarCandidatas(MatrizCompatibilidade matriz, List<Bolsa> bolsas, LocalDate hoje) {
        return bolsas.stream()
                .filter(b -> b.getDataValidade() != null)
                .filter(b -> !b.getDataValidade().isBefore(hoje))
                .filter(b -> matriz.compativel(b.getTipoSanguineo(), RECEPTOR))
                .toList();
    }

    /** Copia de {@code AlocacaoService.alocarProximaCompativel} (privado no servico). */
    private static Bolsa alocarProximaCompativel(IndiceEstoque indice, List<TipoSanguineo> tiposCompativeis) {
        TipoSanguineo melhorTipo = null;
        Bolsa melhorBolsa = null;
        for (TipoSanguineo doador : tiposCompativeis) {
            FilaFEFO fila = indice.consultarEstoque(doador);
            Bolsa topo = fila.peek();
            if (topo != null && (melhorBolsa == null
                    || topo.getDataValidade().isBefore(melhorBolsa.getDataValidade()))) {
                melhorBolsa = topo;
                melhorTipo = doador;
            }
        }
        return melhorTipo != null ? indice.alocarBolsa(melhorTipo) : null;
    }

    // ---------------------------------------------------------------- dijkstra

    private static void medirDijkstra() {
        String g = "Dijkstra (GrafoRotas)";
        GrafoRotas real = DadosSinteticos.criarGrafoRotaVital();
        medir(g, "rede real V=6, E=15: N0 -> N4", 6, 1, 20_000, () -> real,
                grafo -> (long) grafo.calcularMenorRota("N0", "N4").custoTotal());

        for (int v : TAMANHOS) {
            GrafoRotas esparso = grafoEsparso(v);
            String destino = "N" + (v / 2);
            int rep = Math.max(1, 50_000 / v);
            medir(g, "grafo esparso (E ~ 3V): destino alcancavel", v, 1, rep, () -> esparso,
                    grafo -> grafo.calcularMenorRota("N0", destino).caminho().size());
            medir(g, "grafo esparso (E ~ 3V): destino inalcancavel (explora tudo)", v, 1, rep, () -> esparso,
                    grafo -> Double.isInfinite(grafo.calcularMenorRota("N0", "ISO").custoTotal()) ? 1 : 0);
        }
    }

    /** Anel (conectividade garantida) + 2 cordas aleatorias por no; mais um no isolado "ISO". */
    private static GrafoRotas grafoEsparso(int v) {
        Random rnd = new Random(SEMENTE + v);
        GrafoRotas grafo = new GrafoRotas();
        for (int i = 0; i < v; i++) {
            grafo.adicionarNo("N" + i);
        }
        for (int i = 0; i < v; i++) {
            grafo.adicionarAresta("N" + i, "N" + ((i + 1) % v), 1 + rnd.nextInt(100));
            for (int c = 0; c < 2; c++) {
                int j = rnd.nextInt(v);
                if (j != i) {
                    grafo.adicionarAresta("N" + i, "N" + j, 1 + rnd.nextInt(100));
                }
            }
        }
        grafo.adicionarNo("ISO");
        return grafo;
    }

    // ------------------------------------------------------------------- dados

    private static List<Bolsa> gerarBolsas(int n) {
        Random rnd = new Random(SEMENTE + n);
        LocalDate hoje = LocalDate.now();
        TipoSanguineo[] tipos = TipoSanguineo.values();
        List<Bolsa> lista = new ArrayList<>(n);
        for (int i = 0; i < n; i++) {
            LocalDate validade = hoje.plusDays(rnd.nextInt(DIAS_VALIDADE + 1));
            lista.add(Bolsa.builder()
                    .id((long) i + 1)
                    .hemoComponente(HemoComponente.HEMACIAS)
                    .tipoSanguineo(tipos[rnd.nextInt(tipos.length)])
                    .dataColeta(validade.minusDays(DIAS_VALIDADE))
                    .dataValidade(validade)
                    .status(StatusBolsa.DISPONIVEL)
                    .build());
        }
        return lista;
    }

    // ------------------------------------------------------------ cronometragem

    /**
     * @param ops numero de operacoes elementares por execucao (para o ns/op)
     * @param rep repeticoes da operacao dentro do trecho cronometrado (para operacoes muito rapidas
     *            e que nao alteram o estado); os tempos reportados ja sao por execucao
     */
    private static <T> void medir(String grupo, String operacao, int n, int ops, int rep,
                                  Supplier<T> preparar, ToLongFunction<T> funcao) {
        // Aquecimento: no minimo "aquecimento" execucoes E pelo menos 400 ms no total. O criterio
        // de tempo evita medir antes do JIT estabilizar nas operacoes muito rapidas.
        long inicioAquecimento = System.nanoTime();
        for (int i = 0; i < aquecimento || System.nanoTime() - inicioAquecimento < 400_000_000L; i++) {
            T estado = preparar.get();
            for (int k = 0; k < rep; k++) {
                sorvedouro += funcao.applyAsLong(estado);
            }
        }
        double[] ms = new double[medicoes];
        for (int i = 0; i < medicoes; i++) {
            T estado = preparar.get();
            long acumulado = 0;
            long inicio = System.nanoTime();
            for (int k = 0; k < rep; k++) {
                acumulado += funcao.applyAsLong(estado);
            }
            long fim = System.nanoTime();
            sorvedouro += acumulado;
            ms[i] = (fim - inicio) / 1e6 / rep;
        }
        Arrays.sort(ms);
        double mediana = ms[ms.length / 2];
        double p95 = ms[Math.min(ms.length - 1, (int) Math.ceil(0.95 * ms.length) - 1)];
        RESULTADOS.add(new Resultado(grupo, operacao, n, ops, mediana, p95, ms[0]));
        System.err.printf("  medido: %-34s | %-58s | n=%d%n", grupo, operacao, n);
    }

    // --------------------------------------------------------------- corretude

    /** Confere o comportamento antes de medir: medir codigo errado nao serve de evidencia. */
    private static void validarCorretude() {
        MatrizCompatibilidade matriz = new MatrizCompatibilidade();
        int pares = 0;
        for (TipoSanguineo d : TipoSanguineo.values()) {
            for (TipoSanguineo r : TipoSanguineo.values()) {
                if (matriz.compativel(d, r)) {
                    pares++;
                }
            }
        }
        exigir(pares == 27, "a matriz deveria ter 27 pares compativeis de 64, tem " + pares);
        exigir(matriz.matching(RECEPTOR).size() == 4, "A+ deveria ter 4 doadores compativeis");
        exigir(matriz.matching(RECEPTOR).get(0) == RECEPTOR, "o tipo identico deve vir primeiro");
        exigir(matriz.matching(TipoSanguineo.AB_POS).get(7) == TipoSanguineo.O_NEG, "O- deve ser o ultimo");

        List<Bolsa> bolsas = gerarBolsas(10_000);
        FilaFEFO fila = filaCom(bolsas);
        Bolsa anterior = null;
        while (!fila.estaVazia()) {
            Bolsa atual = fila.alocar();
            exigir(anterior == null || ORDEM_FEFO.compare(anterior, atual) <= 0, "a fila FEFO saiu fora de ordem");
            anterior = atual;
        }

        LocalDate hoje = LocalDate.now();
        IndiceEstoque indice = indiceCom(filtrarCandidatas(matriz, bolsas, hoje));
        List<TipoSanguineo> doadores = matriz.matching(RECEPTOR);
        Set<Long> ids = new HashSet<>();
        for (int i = 0; i < 50; i++) {
            Bolsa b = alocarProximaCompativel(indice, doadores);
            exigir(b != null && ids.add(b.getId()), "alocacao repetiu ou devolveu bolsa nula");
            exigir(matriz.compativel(b.getTipoSanguineo(), RECEPTOR), "alocou bolsa incompativel");
        }

        GrafoRotas real = DadosSinteticos.criarGrafoRotaVital();
        exigir(real.calcularMenorRota("N0", "N4").custoTotal() == 17.0, "custo N0->N4 da rede real deveria ser 17");
        GrafoRotas esparso = grafoEsparso(1_000);
        exigir(esparso.calcularMenorRota("N0", "N500").custoTotal() < Double.POSITIVE_INFINITY,
                "destino alcancavel deveria ter custo finito");
        exigir(esparso.calcularMenorRota("N0", "ISO").caminho().isEmpty(),
                "destino inalcancavel deveria devolver caminho vazio");
    }

    private static void exigir(boolean condicao, String mensagem) {
        if (!condicao) {
            throw new IllegalStateException("Verificacao falhou antes da medicao: " + mensagem);
        }
    }

    // ------------------------------------------------------------------- saida

    private static void imprimirMarkdown() {
        Locale loc = "pt".equals(System.getProperty("idioma")) ? new Locale("pt", "BR") : Locale.ROOT;
        System.out.printf(loc, "# Medicao AED (PI2 L1)%n%n");
        System.out.printf(loc, "- Java: %s (%s)%n", System.getProperty("java.version"), System.getProperty("java.vm.name"));
        System.out.printf(loc, "- SO/arquitetura: %s / %s%n", System.getProperty("os.name"), System.getProperty("os.arch"));
        System.out.printf(loc, "- Processadores logicos: %d | heap maximo: %d MB%n",
                Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().maxMemory() / (1024 * 1024));
        System.out.printf(loc, "- Aquecimento: >= %d execucoes e >= 400 ms | medicoes: %d execucoes | mediana, p95 e minimo%n%n",
                aquecimento, medicoes);

        Map<String, List<Resultado>> porGrupo = new LinkedHashMap<>();
        for (Resultado r : RESULTADOS) {
            porGrupo.computeIfAbsent(r.grupo(), k -> new ArrayList<>()).add(r);
        }
        for (Map.Entry<String, List<Resultado>> grupo : porGrupo.entrySet()) {
            System.out.printf(loc, "## %s%n%n", grupo.getKey());
            System.out.printf(loc, "| Operacao | n | mediana (ms) | p95 (ms) | minimo (ms) | ns/op | razao vs n anterior |%n");
            System.out.printf(loc, "|---|---:|---:|---:|---:|---:|---:|%n");
            Map<String, Resultado> anterior = new LinkedHashMap<>();
            for (Resultado r : grupo.getValue()) {
                Resultado a = anterior.get(r.operacao());
                String razao = (a == null || a.n() == r.n()) ? "-"
                        : String.format(loc, "%.2f", r.medianaMs() / a.medianaMs());
                System.out.printf(loc, "| %s | %d | %.4f | %.4f | %.4f | %.1f | %s |%n",
                        r.operacao(), r.n(), r.medianaMs(), r.p95Ms(), r.minMs(), r.nsPorOp(), razao);
                anterior.put(r.operacao(), r);
            }
            System.out.println();
        }
        System.out.println("(controle anti-otimizacao: " + sorvedouro + ")");
    }

    private static void salvarCsv() throws IOException {
        Path destino = Path.of(System.getProperty("saida", "."), "benchmark_aed.csv");
        StringBuilder csv = new StringBuilder("grupo,operacao,n,ops_por_execucao,mediana_ms,p95_ms,minimo_ms,ns_por_op\n");
        for (Resultado r : RESULTADOS) {
            csv.append(String.format(Locale.ROOT, "\"%s\",\"%s\",%d,%d,%.6f,%.6f,%.6f,%.2f%n",
                    r.grupo(), r.operacao(), r.n(), r.ops(), r.medianaMs(), r.p95Ms(), r.minMs(), r.nsPorOp()));
        }
        Files.writeString(destino, csv.toString(), StandardCharsets.UTF_8);
        System.err.println("CSV salvo em " + destino.toAbsolutePath());
    }
}
