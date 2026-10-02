package com.hemorede.estruturas;

import com.hemorede.domain.model.Bolsa;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * CATÁLOGO de bolsas em Árvore Binária de Busca (ABB) ordenada por id.
 *
 * <p>Tradução de {@code aed/u1/c/arvore_bolsas.c}. Mesmo padrão recursivo:
 * cada método recursivo recebe a raiz de uma subárvore e devolve a nova raiz,
 * e quem chama religa a referência ({@code no.esquerda = inserirRec(no.esquerda, ...)}).</p>
 *
 * <p>Custo O(h): O(log n) com a árvore equilibrada, O(n) no pior caso (ids
 * inseridos em ordem). Balanceamento (AVL) não faz parte do escopo da U1.</p>
 */
public class ArvoreBolsas {

    /** Nó da árvore — equivale a {@code struct NoArvore}. */
    private static final class NoArvore {
        private Bolsa bolsa;
        private NoArvore esquerda;
        private NoArvore direita;

        private NoArvore(Bolsa bolsa) {
            this.bolsa = bolsa;
        }
    }

    private NoArvore raiz;
    private int tamanho;

    // Substitui o "int *resultado" usado em C para sinalizar inserção/remoção.
    private boolean alterou;

    /**
     * Insere a bolsa. Equivale a {@code arvore_inserir}.
     *
     * @return {@code true} se inseriu; {@code false} se o id já existe
     */
    public boolean inserir(Bolsa bolsa) {
        Objects.requireNonNull(bolsa, "bolsa não pode ser nula");
        Objects.requireNonNull(bolsa.getId(), "bolsa precisa de id");
        alterou = false;
        raiz = inserirRec(raiz, bolsa);
        if (alterou) {
            tamanho++;
        }
        return alterou;
    }

    private NoArvore inserirRec(NoArvore no, Bolsa bolsa) {
        if (no == null) {
            alterou = true;
            return new NoArvore(bolsa);
        }
        long id = bolsa.getId();
        long chave = no.bolsa.getId();
        if (id < chave) {
            no.esquerda = inserirRec(no.esquerda, bolsa);
        } else if (id > chave) {
            no.direita = inserirRec(no.direita, bolsa);
        }
        return no; // id igual: não insere
    }

    /** Busca pelo id. Equivale a {@code arvore_buscar}. */
    public Bolsa buscar(long id) {
        NoArvore no = buscarRec(raiz, id);
        return no == null ? null : no.bolsa;
    }

    private NoArvore buscarRec(NoArvore no, long id) {
        if (no == null || no.bolsa.getId() == id) {
            return no;
        }
        if (id < no.bolsa.getId()) {
            return buscarRec(no.esquerda, id);
        }
        return buscarRec(no.direita, id);
    }

    /**
     * Remove pelo id. Equivale a {@code arvore_remover}.
     *
     * @return {@code true} se removeu; {@code false} se o id não existe
     */
    public boolean remover(long id) {
        alterou = false;
        raiz = removerRec(raiz, id);
        if (alterou) {
            tamanho--;
        }
        return alterou;
    }

    private NoArvore removerRec(NoArvore no, long id) {
        if (no == null) {
            return null;
        }
        long chave = no.bolsa.getId();
        if (id < chave) {
            no.esquerda = removerRec(no.esquerda, id);
        } else if (id > chave) {
            no.direita = removerRec(no.direita, id);
        } else {
            // Casos 1 e 2: zero ou um filho — o filho (ou null) sobe.
            if (no.esquerda == null || no.direita == null) {
                alterou = true;
                return no.esquerda != null ? no.esquerda : no.direita;
            }
            // Caso 3: dois filhos — copia o sucessor em-ordem e o remove da direita.
            NoArvore sucessor = menorNo(no.direita);
            no.bolsa = sucessor.bolsa;
            no.direita = removerRec(no.direita, sucessor.bolsa.getId());
        }
        return no;
    }

    private NoArvore menorNo(NoArvore no) {
        while (no.esquerda != null) {
            no = no.esquerda;
        }
        return no;
    }

    /** Percurso em-ordem (ids crescentes). Equivale a {@code arvore_em_ordem}. */
    public void emOrdem(Consumer<Bolsa> visitar) {
        Objects.requireNonNull(visitar, "visitante não pode ser nulo");
        emOrdemRec(raiz, visitar);
    }

    private void emOrdemRec(NoArvore no, Consumer<Bolsa> visitar) {
        if (no == null) {
            return;
        }
        emOrdemRec(no.esquerda, visitar);
        visitar.accept(no.bolsa);
        emOrdemRec(no.direita, visitar);
    }

    /** Bolsas ordenadas por id, para serialização. */
    public List<Bolsa> paraListaOrdenada() {
        List<Bolsa> ordenadas = new ArrayList<>(tamanho);
        emOrdem(ordenadas::add);
        return ordenadas;
    }

    /** Altura: vazia = 0, só a raiz = 1. Equivale a {@code arvore_altura}. */
    public int altura() {
        return alturaRec(raiz);
    }

    private int alturaRec(NoArvore no) {
        if (no == null) {
            return 0;
        }
        return 1 + Math.max(alturaRec(no.esquerda), alturaRec(no.direita));
    }

    public int tamanho() {
        return tamanho;
    }

    public boolean vazia() {
        return raiz == null;
    }

    /** Id da raiz (útil em testes), ou {@code null} se vazia. */
    Long idRaiz() {
        return raiz == null ? null : raiz.bolsa.getId();
    }
}
