package com.hemorede.algoritmos;

import com.hemorede.domain.model.Bolsa;
import com.hemorede.exception.EstoqueInsuficienteException;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Fila de prioridade FEFO (First-Expired, First-Out) para gerenciamento de bolsas de sangue.
 * <p>
 * Implementada como um Min-Heap via {@link PriorityQueue}, onde a chave de ordenação
 * prioritária é a {@link Bolsa#getDataValidade()} em ordem crescente.
 * </p>
 * <p>
 * <b>Regra de ouro FEFO:</b> Nunca alocar uma bolsa mais recente enquanto houver uma bolsa
 * mais antiga ainda válida na fila, minimizando assim o descarte e o desperdício de hemocomponentes.
 * </p>
 * <p>
 * <b>Complexidade temporal:</b>
 * <ul>
 *   <li>Inserção ({@link #inserir(Bolsa)}): {@code O(log n)}</li>
 *   <li>Alocação/Remoção do topo ({@link #alocar()}): {@code O(log n)}</li>
 *   <li>Consulta ao topo (peek): {@code O(1)}</li>
 *   <li>Consulta de tamanho / verificação de vazio: {@code O(1)}</li>
 * </ul>
 * </p>
 */
public class FilaFEFO {

    private final PriorityQueue<Bolsa> fila;

    /**
     * Cria uma nova fila FEFO ordenada pela data de validade mais próxima.
     * Em caso de empate de validade, utiliza a data de coleta (mais antiga primeiro).
     */
    public FilaFEFO() {
        this.fila = new PriorityQueue<>(
                Comparator.comparing(Bolsa::getDataValidade, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Bolsa::getDataColeta, Comparator.nullsLast(Comparator.naturalOrder()))
        );
    }

    /**
     * Insere uma nova bolsa na fila FEFO.
     *
     * @param bolsa Bolsa a ser inserida no min-heap.
     * @throws IllegalArgumentException se a bolsa ou sua data de validade forem nulas.
     */
    public void inserir(Bolsa bolsa) {
        if (bolsa == null) {
            throw new IllegalArgumentException("A bolsa não pode ser nula.");
        }
        if (bolsa.getDataValidade() == null) {
            throw new IllegalArgumentException("A data de validade da bolsa não pode ser nula.");
        }
        fila.add(bolsa);
    }

    /**
     * Remove e retorna a bolsa com a data de validade mais próxima do vencimento (topo do min-heap).
     *
     * @return Bolsa com menor data de validade disponível.
     * @throws EstoqueInsuficienteException se a fila estiver vazia.
     */
    public Bolsa alocar() {
        if (fila.isEmpty()) {
            throw new EstoqueInsuficienteException("Não há bolsas disponíveis para alocação nesta fila FEFO.");
        }
        return fila.poll();
    }

    /**
     * Retorna uma lista com todas as bolsas que estão a {@code diasLimite} dias ou menos do vencimento,
     * sem removê-las da fila de prioridade.
     *
     * @param diasLimite Quantidade máxima de dias até o vencimento.
     * @return Lista ordenada das bolsas em risco de vencimento.
     * @throws IllegalArgumentException se {@code diasLimite} for negativo.
     */
    public List<Bolsa> proximasAoVencimento(int diasLimite) {
        if (diasLimite < 0) {
            throw new IllegalArgumentException("O limite de dias não pode ser negativo: " + diasLimite);
        }
        LocalDate dataLimite = LocalDate.now().plusDays(diasLimite);
        return fila.stream()
                .filter(b -> b.getDataValidade() != null && !b.getDataValidade().isAfter(dataLimite))
                .sorted(Comparator.comparing(Bolsa::getDataValidade, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    /**
     * Verifica se a fila FEFO está vazia.
     *
     * @return {@code true} se não houver bolsas na fila, {@code false} caso contrário.
     */
    public boolean estaVazia() {
        return fila.isEmpty();
    }

    /**
     * Retorna o total de bolsas armazenadas na fila FEFO.
     *
     * @return Quantidade de bolsas na fila.
     */
    public int tamanho() {
        return fila.size();
    }
}
