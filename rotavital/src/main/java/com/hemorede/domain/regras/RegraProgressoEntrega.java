package com.hemorede.domain.regras;

import java.util.List;

/**
 * RN-10 - Progresso da entrega em trânsito.
 * <p>
 * Dado o trajeto (pontos geográficos na ordem do caminho de Dijkstra) e a fração do tempo
 * previsto já decorrida, calcula a posição estimada do veículo por interpolação linear ao longo
 * do trajeto, proporcional à distância de cada trecho. Sem GPS real no escopo acadêmico, é a
 * posição exibida no acompanhamento da entrega. Classe pura (sem Spring/JPA).
 * </p>
 */
public final class RegraProgressoEntrega {

    private RegraProgressoEntrega() {
    }

    public record Ponto(double lat, double lng) {
    }

    /** Posição estimada, índice do trecho atual (0 = primeiro trecho) e percentual concluído. */
    public record Posicao(Ponto ponto, int trechoAtual, double percentual) {
    }

    /** Fração [0, 1] do tempo previsto já decorrido. Duração zero ou negativa = concluído. */
    public static double fracaoDecorrida(long segundosDecorridos, long segundosPrevistos) {
        if (segundosPrevistos <= 0) {
            return 1.0;
        }
        return Math.max(0.0, Math.min(1.0, (double) segundosDecorridos / segundosPrevistos));
    }

    /**
     * Interpola a posição ao longo de {@code trajeto} para a fração {@code fracao}. O(p), p = pontos.
     *
     * @param pesosTrechos distância (ou custo) de cada trecho; tamanho = pontos - 1
     */
    public static Posicao interpolar(List<Ponto> trajeto, List<Double> pesosTrechos, double fracao) {
        if (trajeto == null || trajeto.isEmpty()) {
            throw new IllegalArgumentException("trajeto vazio");
        }
        if (trajeto.size() == 1) {
            return new Posicao(trajeto.get(0), 0, 100.0);
        }
        if (pesosTrechos == null || pesosTrechos.size() != trajeto.size() - 1) {
            throw new IllegalArgumentException("informe um peso para cada trecho do trajeto");
        }
        double f = Math.max(0.0, Math.min(1.0, fracao));
        double total = pesosTrechos.stream().mapToDouble(Double::doubleValue).sum();
        if (total <= 0) {
            Ponto destino = trajeto.get(trajeto.size() - 1);
            return new Posicao(destino, trajeto.size() - 2, 100.0);
        }

        double alvo = f * total;
        double acumulado = 0;
        for (int i = 0; i < pesosTrechos.size(); i++) {
            double peso = pesosTrechos.get(i);
            boolean ultimo = i == pesosTrechos.size() - 1;
            if (acumulado + peso >= alvo || ultimo) {
                double t = peso == 0 ? 1.0 : Math.min(1.0, (alvo - acumulado) / peso);
                Ponto a = trajeto.get(i);
                Ponto b = trajeto.get(i + 1);
                Ponto p = new Ponto(a.lat() + (b.lat() - a.lat()) * t, a.lng() + (b.lng() - a.lng()) * t);
                return new Posicao(p, i, Math.round(f * 1000.0) / 10.0);
            }
            acumulado += peso;
        }
        throw new IllegalStateException("inalcançável");
    }
}
