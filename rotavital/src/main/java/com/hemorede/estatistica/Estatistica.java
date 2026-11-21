package com.hemorede.estatistica;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Fórmulas da disciplina de Estatística e Probabilidade, em funções puras.
 *
 * <p>Convenções, iguais às do pandas usado no Colab da disciplina, para o resultado
 * poder ser conferido com {@code df.describe()}, {@code df.skew()}, {@code df.kurt()} e
 * {@code df.corr()}:</p>
 * <ul>
 *   <li>variância, desvio padrão e covariância amostrais (divisor n − 1);</li>
 *   <li>quartis por interpolação linear (posição (n − 1)·p);</li>
 *   <li>assimetria de Fisher-Pearson ajustada (G1) e curtose em excesso ajustada (G2):
 *       0 é o valor da curva normal.</li>
 * </ul>
 */
public final class Estatistica {

    private Estatistica() {
    }

    public static double soma(double[] x) {
        double s = 0;
        for (double v : x) {
            s += v;
        }
        return s;
    }

    public static double media(double[] x) {
        exigir(x, 1);
        return soma(x) / x.length;
    }

    /** Média ponderada: Σ(x·p) / Σp. */
    public static double mediaPonderada(double[] x, double[] pesos) {
        if (x.length != pesos.length || x.length == 0) {
            throw new IllegalArgumentException("valores e pesos precisam ter o mesmo tamanho, maior que zero");
        }
        double num = 0;
        double den = 0;
        for (int i = 0; i < x.length; i++) {
            num += x[i] * pesos[i];
            den += pesos[i];
        }
        if (den == 0) {
            throw new IllegalArgumentException("a soma dos pesos não pode ser zero");
        }
        return num / den;
    }

    public static double mediana(double[] x) {
        return quantil(x, 0.5);
    }

    /** Quantil por interpolação linear entre as posições ordenadas (método padrão do pandas). */
    public static double quantil(double[] x, double p) {
        exigir(x, 1);
        double[] o = ordenado(x);
        double pos = (o.length - 1) * p;
        int i = (int) Math.floor(pos);
        int j = Math.min(i + 1, o.length - 1);
        return o[i] + (o[j] - o[i]) * (pos - i);
    }

    /** Valores mais frequentes; lista vazia quando todos aparecem o mesmo número de vezes (amodal). */
    public static List<Double> modas(double[] x) {
        exigir(x, 1);
        Map<Double, Integer> freq = new TreeMap<>();
        for (double v : x) {
            freq.merge(v, 1, Integer::sum);
        }
        int max = freq.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        int min = freq.values().stream().mapToInt(Integer::intValue).min().orElse(0);
        if (max == min) {
            return List.of();
        }
        List<Double> modas = new ArrayList<>();
        freq.forEach((v, f) -> {
            if (f == max) {
                modas.add(v);
            }
        });
        return modas;
    }

    /** Variância amostral: Σ(x − x̄)² / (n − 1). */
    public static double variancia(double[] x) {
        exigir(x, 2);
        return somaQuadradosDesvios(x) / (x.length - 1);
    }

    /** Variância populacional: Σ(x − x̄)² / n. */
    public static double varianciaPopulacional(double[] x) {
        exigir(x, 1);
        return somaQuadradosDesvios(x) / x.length;
    }

    public static double desvioPadrao(double[] x) {
        return Math.sqrt(variancia(x));
    }

    /** Coeficiente de variação em %: s / x̄ · 100. Nulo quando a média é zero. */
    public static Double coeficienteVariacao(double[] x) {
        double m = media(x);
        return m == 0 ? null : desvioPadrao(x) / Math.abs(m) * 100;
    }

    /** Assimetria G1 (ajustada pelo tamanho da amostra). Nula com n &lt; 3 ou dados constantes. */
    public static Double assimetria(double[] x) {
        int n = x.length;
        if (n < 3) {
            return null;
        }
        double m2 = momentoCentral(x, 2);
        if (m2 == 0) {
            return null;
        }
        double g1 = momentoCentral(x, 3) / Math.pow(m2, 1.5);
        return g1 * Math.sqrt((double) n * (n - 1)) / (n - 2);
    }

    /** Curtose em excesso G2 (0 = normal). Nula com n &lt; 4 ou dados constantes. */
    public static Double curtose(double[] x) {
        int n = x.length;
        if (n < 4) {
            return null;
        }
        double m2 = momentoCentral(x, 2);
        if (m2 == 0) {
            return null;
        }
        double g2 = momentoCentral(x, 4) / (m2 * m2) - 3;
        return ((n + 1) * g2 + 6) * (n - 1) / ((double) (n - 2) * (n - 3));
    }

    /** Covariância amostral: Σ(x − x̄)(y − ȳ) / (n − 1). */
    public static double covariancia(double[] x, double[] y) {
        exigirPar(x, y);
        return somaProdutosDesvios(x, y) / (x.length - 1);
    }

    /** r de Pearson pela fórmula dos desvios. Nulo se uma das variáveis for constante. */
    public static Double pearson(double[] x, double[] y) {
        exigirPar(x, y);
        double sxx = somaQuadradosDesvios(x);
        double syy = somaQuadradosDesvios(y);
        if (sxx == 0 || syy == 0) {
            return null;
        }
        return somaProdutosDesvios(x, y) / Math.sqrt(sxx * syy);
    }

    /** r de Pearson pela fórmula dos somatórios; dá o mesmo valor da fórmula dos desvios. */
    public static Double pearsonPorSomatorios(double[] x, double[] y) {
        exigirPar(x, y);
        int n = x.length;
        double sx = 0;
        double sy = 0;
        double sxy = 0;
        double sx2 = 0;
        double sy2 = 0;
        for (int i = 0; i < n; i++) {
            sx += x[i];
            sy += y[i];
            sxy += x[i] * y[i];
            sx2 += x[i] * x[i];
            sy2 += y[i] * y[i];
        }
        double den = Math.sqrt((n * sx2 - sx * sx) * (n * sy2 - sy * sy));
        return den == 0 ? null : (n * sxy - sx * sy) / den;
    }

    /** Reta de mínimos quadrados ŷ = a·x + b. Nula se x for constante. */
    public static Reta regressao(double[] x, double[] y) {
        exigirPar(x, y);
        double sxx = somaQuadradosDesvios(x);
        if (sxx == 0) {
            return null;
        }
        double a = somaProdutosDesvios(x, y) / sxx;
        return new Reta(a, media(y) - a * media(x));
    }

    /** Força pela escala usada na disciplina: fraca abaixo de 0,3, moderada até 0,7, forte a partir dela. */
    public static String forca(Double r) {
        if (r == null) {
            return "indefinida";
        }
        double a = Math.abs(r);
        if (a < 0.1) {
            return "praticamente nula";
        }
        if (a < 0.3) {
            return "fraca";
        }
        return a < 0.7 ? "moderada" : "forte";
    }

    public static String direcao(Double r) {
        if (r == null || Math.abs(r) < 0.1) {
            return "sem direção";
        }
        return r > 0 ? "positiva" : "negativa";
    }

    public static String leituraAssimetria(Double g1) {
        if (g1 == null) {
            return "indefinida";
        }
        if (Math.abs(g1) < 0.5) {
            return "aproximadamente simétrica";
        }
        return g1 > 0 ? "assimetria positiva (cauda à direita)" : "assimetria negativa (cauda à esquerda)";
    }

    public static String leituraCurtose(Double g2) {
        if (g2 == null) {
            return "indefinida";
        }
        if (Math.abs(g2) < 0.5) {
            return "mesocúrtica (próxima da normal)";
        }
        return g2 > 0 ? "leptocúrtica (caudas pesadas)" : "platicúrtica (caudas leves)";
    }

    /** Tabela de frequência com o número de classes pela regra de Sturges: k = 1 + log2(n), arredondado para cima. */
    public static List<Classe> histograma(double[] x) {
        exigir(x, 1);
        double min = Arrays.stream(x).min().orElse(0);
        double max = Arrays.stream(x).max().orElse(0);
        if (max == min) {
            return List.of(new Classe(min, max, x.length));
        }
        int k = (int) Math.ceil(1 + Math.log(x.length) / Math.log(2));
        double h = (max - min) / k;
        int[] freq = new int[k];
        for (double v : x) {
            int i = (int) Math.floor((v - min) / h);
            freq[Math.min(i, k - 1)]++;
        }
        List<Classe> classes = new ArrayList<>(k);
        for (int i = 0; i < k; i++) {
            classes.add(new Classe(min + i * h, min + (i + 1) * h, freq[i]));
        }
        return classes;
    }

    private static double somaQuadradosDesvios(double[] x) {
        return somaProdutosDesvios(x, x);
    }

    private static double somaProdutosDesvios(double[] x, double[] y) {
        double mx = media(x);
        double my = media(y);
        double s = 0;
        for (int i = 0; i < x.length; i++) {
            s += (x[i] - mx) * (y[i] - my);
        }
        return s;
    }

    private static double momentoCentral(double[] x, int k) {
        double m = media(x);
        double s = 0;
        for (double v : x) {
            s += Math.pow(v - m, k);
        }
        return s / x.length;
    }

    private static double[] ordenado(double[] x) {
        double[] o = x.clone();
        Arrays.sort(o);
        return o;
    }

    private static void exigir(double[] x, int minimo) {
        if (x == null || x.length < minimo) {
            throw new IllegalArgumentException("são necessários pelo menos " + minimo + " valor(es)");
        }
    }

    private static void exigirPar(double[] x, double[] y) {
        exigir(x, 2);
        if (y == null || y.length != x.length) {
            throw new IllegalArgumentException("x e y precisam ter o mesmo número de observações");
        }
    }

    /** ŷ = a·x + b. */
    public record Reta(double a, double b) {
        public double prever(double x) {
            return a * x + b;
        }
    }

    /** Classe da tabela de frequência: [inicio, fim), a última inclui o fim. */
    public record Classe(double inicio, double fim, int frequencia) {
        public double pontoMedio() {
            return (inicio + fim) / 2;
        }
    }
}
