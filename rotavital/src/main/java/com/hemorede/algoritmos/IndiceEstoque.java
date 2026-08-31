package com.hemorede.algoritmos;

import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.exception.EstoqueInsuficienteException;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Índice de estoque indexado por tipo sanguíneo utilizando tabela Hash ({@link HashMap} / {@link EnumMap}).
 * <p>
 * O mapeamento associa cada {@link TipoSanguineo} diretamente a uma {@link FilaFEFO},
 * garantindo que qualquer consulta ou alocação por tipo sanguíneo já venha com
 * a priorização de validade (First-Expired, First-Out) nativamente integrada.
 * </p>
 * <p>
 * <b>Complexidade temporal:</b>
 * <ul>
 *   <li>Consulta por tipo ({@link #consultarEstoque(TipoSanguineo)}): {@code O(1)} amortizado.
 *       Esta é a operação mais frequente do sistema (triagem de pedidos e verificação de disponibilidade).</li>
 *   <li>Inserção de bolsa ({@link #adicionarBolsa(Bolsa)}): {@code O(1)} amortizado para encontrar o bucket hash
 *       + {@code O(log n)} para inserção no heap FEFO = {@code O(log n)}.</li>
 *   <li>Alocação de bolsa ({@link #alocarBolsa(TipoSanguineo)}): {@code O(1)} amortizado para encontrar o bucket hash
 *       + {@code O(log n)} para extração do elemento de menor validade do heap = {@code O(log n)}.</li>
 *   <li>Contagem de estoque por tipo ({@link #totalDisponivelPorTipo(TipoSanguineo)}): {@code O(1)}.</li>
 * </ul>
 * </p>
 */
public class IndiceEstoque {

    private final Map<TipoSanguineo, FilaFEFO> estoquePorTipo;

    /**
     * Inicializa a estrutura de índice de estoque com uma {@link FilaFEFO} para cada {@link TipoSanguineo}.
     */
    public IndiceEstoque() {
        this.estoquePorTipo = new EnumMap<>(TipoSanguineo.class);
        for (TipoSanguineo tipo : TipoSanguineo.values()) {
            this.estoquePorTipo.put(tipo, new FilaFEFO());
        }
    }

    /**
     * Adiciona uma bolsa ao estoque, direcionando-a imediatamente para a fila FEFO do seu tipo sanguíneo.
     *
     * @param bolsa Bolsa a ser adicionada.
     * @throws IllegalArgumentException se a bolsa ou seu tipo sanguíneo forem nulos.
     */
    public void adicionarBolsa(Bolsa bolsa) {
        if (bolsa == null) {
            throw new IllegalArgumentException("A bolsa não pode ser nula.");
        }
        if (bolsa.getTipoSanguineo() == null) {
            throw new IllegalArgumentException("O tipo sanguíneo da bolsa não pode ser nulo.");
        }
        estoquePorTipo.computeIfAbsent(bolsa.getTipoSanguineo(), k -> new FilaFEFO()).inserir(bolsa);
    }

    /**
     * Aloca (remove e retorna) a bolsa com validade mais próxima do vencimento para o tipo sanguíneo informado.
     *
     * @param tipo Tipo sanguíneo requisitado.
     * @return Bolsa alocada conforme política FEFO.
     * @throws EstoqueInsuficienteException se não houver bolsas disponíveis do tipo solicitado.
     * @throws IllegalArgumentException se o tipo sanguíneo for nulo.
     */
    public Bolsa alocarBolsa(TipoSanguineo tipo) {
        if (tipo == null) {
            throw new IllegalArgumentException("O tipo sanguíneo não pode ser nulo.");
        }
        FilaFEFO fila = estoquePorTipo.get(tipo);
        if (fila == null || fila.estaVazia()) {
            throw new EstoqueInsuficienteException("Estoque insuficiente para o tipo sanguíneo: " + tipo);
        }
        return fila.alocar();
    }

    /**
     * Retorna o total de bolsas disponíveis para um determinado tipo sanguíneo em tempo {@code O(1)}.
     *
     * @param tipo Tipo sanguíneo a ser consultado.
     * @return Quantidade de bolsas disponíveis.
     */
    public int totalDisponivelPorTipo(TipoSanguineo tipo) {
        if (tipo == null) {
            return 0;
        }
        FilaFEFO fila = estoquePorTipo.get(tipo);
        return (fila != null) ? fila.tamanho() : 0;
    }

    /**
     * Consulta a {@link FilaFEFO} associada ao tipo sanguíneo informado.
     *
     * @param tipo Tipo sanguíneo a ser consultado.
     * @return Fila FEFO do tipo correspondente.
     */
    public FilaFEFO consultarEstoque(TipoSanguineo tipo) {
        if (tipo == null) {
            return null;
        }
        return estoquePorTipo.computeIfAbsent(tipo, k -> new FilaFEFO());
    }

    /**
     * Retorna a quantidade total de bolsas somando todos os tipos sanguíneos.
     *
     * @return Total geral de bolsas em estoque.
     */
    public int totalGeral() {
        return estoquePorTipo.values().stream().mapToInt(FilaFEFO::tamanho).sum();
    }
}
