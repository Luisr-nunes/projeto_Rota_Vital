package com.hemorede.estatistica;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Fórmulas conferidas com os exemplos das aulas e com valores de referência do
 * numpy/scipy (ddof=1, bias=False), as mesmas convenções do pandas no Colab.
 */
class EstatisticaTest {

    private static final double EPS = 1e-9;
    // Caso 3 da aula de 17/10: café (xícaras/dia) x produtividade (%).
    private static final double[] CAFE = {1, 2, 3, 4, 5};
    private static final double[] PRODUTIVIDADE = {80, 85, 90, 92, 95};

    @Test
    @DisplayName("Tendência central: média, mediana, modas e média ponderada da nota da disciplina")
    void tendenciaCentral() {
        double[] x = {1, 2, 3, 4, 10};
        assertEquals(4.0, Estatistica.media(x), EPS);
        assertEquals(3.0, Estatistica.mediana(x), EPS);
        assertEquals(List.of(), Estatistica.modas(x));
        assertEquals(List.of(2.0, 5.0), Estatistica.modas(new double[]{2, 2, 5, 5, 7}));
        // Projeto 40%, prova 50%, atividades 10%: notas 8, 6 e 10 dão 7,2 (a simples daria 8,0).
        assertEquals(7.2, Estatistica.mediaPonderada(new double[]{8, 6, 10}, new double[]{40, 50, 10}), EPS);
    }

    @Test
    @DisplayName("Dispersão e quartis iguais ao numpy (variância amostral, quartis por interpolação)")
    void dispersaoEQuartis() {
        double[] x = {1, 2, 3, 4, 10};
        assertEquals(12.5, Estatistica.variancia(x), EPS);
        assertEquals(10.0, Estatistica.varianciaPopulacional(x), EPS);
        assertEquals(Math.sqrt(12.5), Estatistica.desvioPadrao(x), EPS);
        assertEquals(2.0, Estatistica.quantil(x, 0.25), EPS);
        assertEquals(4.0, Estatistica.quantil(x, 0.75), EPS);
        assertEquals(Math.sqrt(12.5) / 4 * 100, Estatistica.coeficienteVariacao(x), EPS);
    }

    @Test
    @DisplayName("Assimetria e curtose iguais ao scipy (bias=False): outlier à direita dá assimetria positiva")
    void assimetriaECurtose() {
        double[] x = {1, 2, 3, 4, 10};
        assertEquals(1.6970562748477143, Estatistica.assimetria(x), 1e-12);
        assertEquals(3.152, Estatistica.curtose(x), 1e-12);
        assertTrue(Estatistica.leituraAssimetria(Estatistica.assimetria(x)).startsWith("assimetria positiva"));
        assertTrue(Estatistica.leituraCurtose(Estatistica.curtose(x)).startsWith("leptocúrtica"));
        assertEquals(0.0, Estatistica.assimetria(new double[]{1, 2, 3, 4, 5}), EPS);
        assertNull(Estatistica.assimetria(new double[]{7, 7, 7}));
    }

    @Test
    @DisplayName("Pearson do caso do café: r = 0,9847 pelas duas fórmulas, r² = 0,9695, reta ŷ = 3,7x + 77,3")
    void pearsonCasoDoCafe() {
        double r = Estatistica.pearson(CAFE, PRODUTIVIDADE);
        assertEquals(0.9846556465128464, r, 1e-12);
        assertEquals(r, Estatistica.pearsonPorSomatorios(CAFE, PRODUTIVIDADE), 1e-12);
        assertEquals(0.9695, r * r, 1e-4);
        assertEquals(9.25, Estatistica.covariancia(CAFE, PRODUTIVIDADE), EPS);
        assertEquals("forte", Estatistica.forca(r));
        assertEquals("positiva", Estatistica.direcao(r));
        Estatistica.Reta reta = Estatistica.regressao(CAFE, PRODUTIVIDADE);
        assertEquals(3.7, reta.a(), EPS);
        assertEquals(77.3, reta.b(), EPS);
        assertEquals(99.5, reta.prever(6), EPS);
    }

    @Test
    @DisplayName("Da covariância ao r: Cov = r · σx · σy, e r não muda com a escala")
    void covarianciaEPropriedades() {
        double r = Estatistica.pearson(CAFE, PRODUTIVIDADE);
        double cov = Estatistica.covariancia(CAFE, PRODUTIVIDADE);
        assertEquals(cov, r * Estatistica.desvioPadrao(CAFE) * Estatistica.desvioPadrao(PRODUTIVIDADE), EPS);
        double[] cafeEmMl = {150, 300, 450, 600, 750};
        assertEquals(r, Estatistica.pearson(cafeEmMl, PRODUTIVIDADE), 1e-12);
        assertEquals(-r, Estatistica.pearson(CAFE, new double[]{-80, -85, -90, -92, -95}), 1e-12);
        assertNull(Estatistica.pearson(CAFE, new double[]{5, 5, 5, 5, 5}));
    }

    @Test
    @DisplayName("Histograma por Sturges: k = 1 + log2(n) classes, frequências somam n")
    void histograma() {
        double[] x = new double[16];
        for (int i = 0; i < x.length; i++) {
            x[i] = i;
        }
        List<Estatistica.Classe> h = Estatistica.histograma(x);
        assertEquals(5, h.size());
        assertEquals(16, h.stream().mapToInt(Estatistica.Classe::frequencia).sum());
        assertEquals(0.0, h.get(0).inicio(), EPS);
        assertEquals(15.0, h.get(4).fim(), EPS);
    }

    @Test
    @DisplayName("Entradas inválidas são recusadas com mensagem")
    void entradasInvalidas() {
        assertThrows(IllegalArgumentException.class, () -> Estatistica.media(new double[0]));
        assertThrows(IllegalArgumentException.class, () -> Estatistica.variancia(new double[]{1}));
        assertThrows(IllegalArgumentException.class, () -> Estatistica.pearson(CAFE, new double[]{1, 2}));
        assertThrows(IllegalArgumentException.class, () -> Estatistica.mediaPonderada(new double[]{1}, new double[]{0}));
    }
}
