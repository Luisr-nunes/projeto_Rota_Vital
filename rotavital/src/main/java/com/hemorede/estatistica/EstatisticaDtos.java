package com.hemorede.estatistica;

import java.util.List;

/** Respostas da API /api/estatistica (telas da disciplina de Estatística no painel de gestão). */
public final class EstatisticaDtos {

    private EstatisticaDtos() {
    }

    /** fonte: "real" (banco do sistema) ou "sintetico" (HistoricoSintetico, semente fixa). */
    public record Variavel(String id, String nome, String unidade, String tipo,
                           String conjunto, String fonte, int n, boolean correlacionavel) {
    }

    public record Descritiva(
            Variavel variavel,
            double soma,
            double media,
            double mediana,
            List<Double> modas,
            Estatistica.Classe classeModal,
            Double mediaPonderadaAgrupada,
            double minimo,
            double maximo,
            double amplitude,
            double q1,
            double q3,
            double iqr,
            Double variancia,
            Double varianciaPopulacional,
            Double desvioPadrao,
            Double coeficienteVariacao,
            Double assimetria,
            String leituraAssimetria,
            Double curtose,
            String leituraCurtose,
            String relacaoMediaMediana,
            double limiteInferiorOutlier,
            double limiteSuperiorOutlier,
            int outliers,
            List<Estatistica.Classe> histograma,
            List<Double> primeirosValores) {
    }

    /** r de Pearson entre todas as variáveis do conjunto; null onde uma variável é constante. */
    public record Matriz(String conjunto, String nome, String descricao, List<Variavel> variaveis, List<List<Double>> r) {
    }

    /** Uma linha da tabela de desvios, como na resolução manual da prova. */
    public record LinhaMemoria(double x, double y, double dx, double dy, double dxdy, double dx2, double dy2) {
    }

    public record Somatorios(double sx, double sy, double sxy, double sx2, double sy2,
                             double sdxdy, double sdx2, double sdy2) {
    }

    public record Par(
            Variavel x,
            Variavel y,
            int n,
            double mediaX,
            double mediaY,
            double desvioX,
            double desvioY,
            double covariancia,
            Double r,
            Double rPorSomatorios,
            Double r2,
            String forca,
            String direcao,
            Estatistica.Reta reta,
            Somatorios somatorios,
            List<LinhaMemoria> memoria,
            List<double[]> pontos) {
    }

    public record FeatureAvaliada(Variavel variavel, Double r, Double r2, String forca, String direcao,
                                  boolean selecionada, String motivo) {
    }

    public record Previsao(
            Variavel alvo,
            double limiar,
            List<FeatureAvaliada> features,
            List<List<String>> redundantes,
            Variavel melhorFeature,
            Estatistica.Reta reta,
            Double r2,
            Double x,
            Double yPrevisto) {
    }
}
