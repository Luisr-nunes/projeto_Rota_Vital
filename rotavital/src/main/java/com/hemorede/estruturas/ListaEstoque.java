package com.hemorede.estruturas;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ESTOQUE do hemocentro como lista simplesmente encadeada de {@link Bolsa}.
 *
 * <p>Tradução de {@code aed/u1/c/lista_estoque.c}. Cada {@code new NoBolsa(...)}
 * corresponde a um {@code malloc(sizeof(NoBolsa))}; cada nó que deixa de ser
 * referenciado em {@link #remover(long)} é recolhido pelo coletor de lixo, papel
 * que em C é do {@code free(atual)}.</p>
 *
 * <p>A estrutura não usa coleções do {@code java.util} internamente — o
 * encadeamento é feito à mão com as referências {@code inicio}, {@code fim} e
 * {@code proximo}. {@link #paraLista()} existe apenas como saída para a camada
 * web (DTO/JSON).</p>
 *
 * <p>Não é thread-safe e não depende do Spring: um service pode instanciá-la
 * diretamente ou expô-la como bean.</p>
 */
public class ListaEstoque {

    /** Nó da lista — equivale a {@code struct NoBolsa}. */
    private static final class NoBolsa {
        private final Bolsa bolsa;
        private NoBolsa proximo;

        private NoBolsa(Bolsa bolsa) {
            this.bolsa = bolsa;
        }
    }

    private NoBolsa inicio;
    private NoBolsa fim;
    private int tamanho;

    /**
     * Insere no fim, rejeitando id repetido. Equivale a {@code lista_inserir}.
     *
     * @return {@code true} se inseriu; {@code false} se já existe bolsa com o mesmo id
     * @throws IllegalArgumentException se a bolsa ou o id forem nulos
     */
    public boolean inserir(Bolsa bolsa) {
        validar(bolsa);
        if (buscar(bolsa.getId()) != null) {
            return false;
        }
        NoBolsa novo = new NoBolsa(bolsa);
        if (fim == null) {
            inicio = novo;
        } else {
            fim.proximo = novo;
        }
        fim = novo;
        tamanho++;
        return true;
    }

    /**
     * Remove a bolsa com o id informado. Equivale a {@code lista_remover}.
     *
     * @return a bolsa removida, ou {@code null} se não existir
     */
    public Bolsa remover(long id) {
        NoBolsa anterior = null;
        NoBolsa atual = inicio;
        while (atual != null && atual.bolsa.getId() != id) {
            anterior = atual;
            atual = atual.proximo;
        }
        if (atual == null) {
            return null;
        }
        if (anterior == null) {
            inicio = atual.proximo;
        } else {
            anterior.proximo = atual.proximo;
        }
        if (atual == fim) {
            fim = anterior;
        }
        atual.proximo = null; // desliga o nó; sem referências, o GC o recolhe
        tamanho--;
        return atual.bolsa;
    }

    /**
     * Consulta por id. Devolve a própria instância guardada (alterações de status
     * refletem no estoque), como o ponteiro devolvido por {@code lista_buscar}.
     */
    public Bolsa buscar(long id) {
        for (NoBolsa atual = inicio; atual != null; atual = atual.proximo) {
            if (atual.bolsa.getId() == id) {
                return atual.bolsa;
            }
        }
        return null;
    }

    public int tamanho() {
        return tamanho;
    }

    public boolean vazia() {
        return inicio == null;
    }

    /** Conta bolsas DISPONIVEIS de um tipo/componente — versão recursiva. */
    public int contarDisponiveis(TipoSanguineo tipo, HemoComponente componente) {
        return contarDisponiveisRec(inicio, tipo, componente);
    }

    private int contarDisponiveisRec(NoBolsa no, TipoSanguineo tipo, HemoComponente componente) {
        if (no == null) {
            return 0;
        }
        int casa = (no.bolsa.getTipoSanguineo() == tipo
                && no.bolsa.getHemoComponente() == componente
                && no.bolsa.getStatus() == StatusBolsa.DISPONIVEL) ? 1 : 0;
        return casa + contarDisponiveisRec(no.proximo, tipo, componente);
    }

    /** Cópia em ordem de chegada, para serialização (não expõe os nós). */
    public List<Bolsa> paraLista() {
        List<Bolsa> copia = new ArrayList<>(tamanho);
        for (NoBolsa atual = inicio; atual != null; atual = atual.proximo) {
            copia.add(atual.bolsa);
        }
        return copia;
    }

    /** Primeira bolsa (mais antiga no estoque) ou {@code null}. */
    public Bolsa primeira() {
        return inicio == null ? null : inicio.bolsa;
    }

    /** Última bolsa inserida ou {@code null}. */
    public Bolsa ultima() {
        return fim == null ? null : fim.bolsa;
    }

    private static void validar(Bolsa bolsa) {
        Objects.requireNonNull(bolsa, "bolsa não pode ser nula");
        if (bolsa.getId() == null) {
            throw new IllegalArgumentException("bolsa precisa de id para entrar no estoque");
        }
    }
}
