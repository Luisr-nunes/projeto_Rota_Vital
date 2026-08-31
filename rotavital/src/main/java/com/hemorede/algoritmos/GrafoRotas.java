package com.hemorede.algoritmos;

import java.util.*;

/**
 * Representação de um grafo não-direcionado ponderado para cálculo de menor caminho
 * entre os nós da malha logística do Rota Vital (Hemocentro e Hospitais).
 * <p>
 * A estrutura interna utiliza lista de adjacência baseada em {@link Map}.
 * O cálculo de menor caminho é realizado pelo algoritmo de Dijkstra utilizando
 * uma {@link PriorityQueue} (min-heap) para seleção gulosa do nó de menor custo.
 * </p>
 * <p>
 * <b>Complexidade de tempo:</b> {@code O((V + E) log V)}, onde {@code V} é o número de vértices (nós)
 * e {@code E} é o número de arestas. Cada vértice é inserido e removido da fila de prioridade
 * em tempo logarítmico {@code O(log V)}, e cada aresta é percorrida uma única vez no relaxamento.
 * </p>
 * <p>
 * <b>Complexidade de espaço:</b> {@code O(V + E)} para armazenar a lista de adjacência, mapas de
 * distâncias, predecessores e nós visitados.
 * </p>
 */
public class GrafoRotas {

    /**
     * Registro imutável que representa o resultado do cálculo de rota mínima.
     *
     * @param caminho    Lista ordenada dos identificadores dos nós que compõem a rota mínima.
     * @param custoTotal Custo total acumulado (distância/tempo) do trajeto.
     */
    public record ResultadoRota(List<String> caminho, double custoTotal) {}

    /**
     * Registro auxiliar para manipulação de nós na fila de prioridade do Dijkstra.
     */
    private record NoDistancia(String id, double distancia) implements Comparable<NoDistancia> {
        @Override
        public int compareTo(NoDistancia outro) {
            return Double.compare(this.distancia, outro.distancia);
        }
    }

    private final Map<String, Map<String, Double>> adjacencias;

    /**
     * Cria uma nova instância de GrafoRotas vazio.
     */
    public GrafoRotas() {
        this.adjacencias = new HashMap<>();
    }

    /**
     * Adiciona um vértice (nó) ao grafo caso ainda não exista.
     *
     * @param id Identificador único do nó (ex: "N0", "N1").
     */
    public void adicionarNo(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("O identificador do nó não pode ser nulo ou vazio.");
        }
        adjacencias.putIfAbsent(id, new HashMap<>());
    }

    /**
     * Adiciona uma aresta bidirecional (não-direcionada) com determinado peso entre dois nós.
     * Se os nós ainda não existirem no grafo, são automaticamente criados.
     *
     * @param origem  Identificador do nó de origem.
     * @param destino Identificador do nó de destino.
     * @param peso    Custo/distância entre os dois nós (deve ser não-negativo).
     */
    public void adicionarAresta(String origem, String destino, double peso) {
        if (peso < 0) {
            throw new IllegalArgumentException("O peso da aresta não pode ser negativo no algoritmo de Dijkstra.");
        }
        adicionarNo(origem);
        adicionarNo(destino);

        adjacencias.get(origem).put(destino, peso);
        adjacencias.get(destino).put(origem, peso);
    }

    /**
     * Calcula o menor caminho entre dois nós utilizando o algoritmo de Dijkstra manual.
     *
     * @param origem  Identificador do nó de partida.
     * @param destino Identificador do nó de chegada.
     * @return {@link ResultadoRota} contendo a lista ordenada dos nós do caminho e o custo total.
     *         Se não houver caminho possível entre os nós, retorna lista vazia e custo {@link Double#POSITIVE_INFINITY}.
     * @throws IllegalArgumentException se o nó de origem ou de destino não existirem no grafo.
     */
    public ResultadoRota calcularMenorRota(String origem, String destino) {
        if (!adjacencias.containsKey(origem)) {
            throw new IllegalArgumentException("Nó de origem não encontrado no grafo: " + origem);
        }
        if (!adjacencias.containsKey(destino)) {
            throw new IllegalArgumentException("Nó de destino não encontrado no grafo: " + destino);
        }

        if (origem.equals(destino)) {
            return new ResultadoRota(List.of(origem), 0.0);
        }

        Map<String, Double> distancias = new HashMap<>();
        Map<String, String> anterior = new HashMap<>();
        Set<String> visitados = new HashSet<>();

        for (String no : adjacencias.keySet()) {
            distancias.put(no, Double.POSITIVE_INFINITY);
        }
        distancias.put(origem, 0.0);

        PriorityQueue<NoDistancia> filaPrioridade = new PriorityQueue<>();
        filaPrioridade.add(new NoDistancia(origem, 0.0));

        while (!filaPrioridade.isEmpty()) {
            NoDistancia atual = filaPrioridade.poll();
            String u = atual.id();

            if (visitados.contains(u)) {
                continue;
            }
            visitados.add(u);

            if (u.equals(destino)) {
                break;
            }

            Map<String, Double> vizinhos = adjacencias.getOrDefault(u, Collections.emptyMap());
            for (Map.Entry<String, Double> aresta : vizinhos.entrySet()) {
                String v = aresta.getKey();
                double peso = aresta.getValue();

                if (visitados.contains(v)) {
                    continue;
                }

                double novoCusto = distancias.get(u) + peso;
                if (novoCusto < distancias.get(v)) {
                    distancias.put(v, novoCusto);
                    anterior.put(v, u);
                    filaPrioridade.add(new NoDistancia(v, novoCusto));
                }
            }
        }

        double custoDestino = distancias.get(destino);
        if (Double.isInfinite(custoDestino)) {
            return new ResultadoRota(Collections.emptyList(), Double.POSITIVE_INFINITY);
        }

        List<String> caminho = reconstruirCaminho(anterior, destino);
        return new ResultadoRota(caminho, custoDestino);
    }

    /**
     * Reconstrói a lista ordenada de nós do caminho a partir do mapa de predecessores.
     */
    private List<String> reconstruirCaminho(Map<String, String> anterior, String destino) {
        LinkedList<String> caminho = new LinkedList<>();
        String no = destino;
        while (no != null) {
            caminho.addFirst(no);
            no = anterior.get(no);
        }
        return Collections.unmodifiableList(caminho);
    }

    /**
     * Retorna o conjunto de identificadores de todos os nós cadastrados no grafo.
     */
    public Set<String> getNos() {
        return Collections.unmodifiableSet(adjacencias.keySet());
    }

    /**
     * Retorna o mapa de adjacências de um nó específico.
     */
    public Map<String, Double> getVizinhos(String id) {
        return Collections.unmodifiableMap(adjacencias.getOrDefault(id, Collections.emptyMap()));
    }
}
