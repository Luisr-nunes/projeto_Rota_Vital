package com.hemorede.estruturas;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.Prioridade;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.domain.model.Requisicao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Mesmos cenários de {@code aed/u1/c/testes.c}, agora sobre as entidades JPA
 * reais ({@link Bolsa}, {@link Requisicao}) — comprova que as estruturas podem
 * ser consumidas pela aplicação sem adaptação.
 */
class EstruturasU1Test {

    private static Bolsa bolsa(long id, HemoComponente c, TipoSanguineo t) {
        return Bolsa.builder()
                .id(id).hemoComponente(c).tipoSanguineo(t)
                .dataColeta(LocalDate.of(2026, 9, 1))
                .dataValidade(LocalDate.of(2026, 10, 13))
                .build();
    }

    private static Requisicao requisicao(long id, Prioridade p) {
        return new Requisicao(id, null, null, p, null, null, null);
    }

    @Nested
    @DisplayName("Lista (estoque)")
    class Lista {

        @Test
        void operacoesEmListaVazia() {
            ListaEstoque l = new ListaEstoque();
            assertTrue(l.vazia());
            assertNull(l.buscar(1));
            assertNull(l.remover(1));
        }

        @Test
        void inserirRemoverConsultarECasosDeBorda() {
            ListaEstoque l = new ListaEstoque();
            assertTrue(l.inserir(bolsa(10, HemoComponente.HEMACIAS, TipoSanguineo.O_NEG)));
            assertTrue(l.inserir(bolsa(20, HemoComponente.HEMACIAS, TipoSanguineo.O_NEG)));
            assertTrue(l.inserir(bolsa(30, HemoComponente.PLASMA, TipoSanguineo.A_POS)));
            assertFalse(l.inserir(bolsa(20, HemoComponente.PLASMA, TipoSanguineo.A_POS)), "id duplicado");
            assertEquals(3, l.tamanho());
            assertEquals(10L, l.primeira().getId());
            assertEquals(30L, l.ultima().getId());

            assertEquals(HemoComponente.PLASMA, l.buscar(30).getHemoComponente());
            l.buscar(20).setStatus(StatusBolsa.RESERVADA); // consulta devolve a instância guardada
            assertEquals(1, l.contarDisponiveis(TipoSanguineo.O_NEG, HemoComponente.HEMACIAS));

            Bolsa removida = l.remover(20); // meio
            assertEquals(StatusBolsa.RESERVADA, removida.getStatus());
            assertNotNull(l.remover(30)); // fim
            assertEquals(10L, l.ultima().getId());
            assertTrue(l.inserir(bolsa(40, HemoComponente.PLAQUETAS, TipoSanguineo.B_POS)));
            assertEquals(List.of(10L, 40L), l.paraLista().stream().map(Bolsa::getId).toList());
            assertNotNull(l.remover(10)); // início
            assertNotNull(l.remover(40));
            assertTrue(l.vazia());
            assertNull(l.ultima());
            assertEquals(0, l.tamanho());
        }

        @Test
        void rejeitaBolsaSemId() {
            ListaEstoque l = new ListaEstoque();
            assertThrows(NullPointerException.class, () -> l.inserir(null));
            assertThrows(IllegalArgumentException.class, () -> l.inserir(new Bolsa()));
        }
    }

    @Nested
    @DisplayName("Fila (requisições)")
    class Fila {

        @Test
        void filaVazia() {
            FilaRequisicoes f = new FilaRequisicoes();
            assertTrue(f.vazia());
            assertNull(f.frente());
            assertNull(f.desenfileirar());
        }

        @Test
        void ordemFifoEConsultas() {
            FilaRequisicoes f = new FilaRequisicoes();
            f.enfileirar(requisicao(1, Prioridade.NORMAL));
            f.enfileirar(requisicao(2, Prioridade.URGENTE));
            f.enfileirar(requisicao(3, Prioridade.NORMAL));
            assertEquals(3, f.tamanho());
            assertEquals(1L, f.frente().getId());
            assertEquals(3L, f.consultar(3).getId());
            assertNull(f.consultar(99));
            assertEquals(2, f.posicao(3));
            assertEquals(-1, f.posicao(99));

            assertEquals(1L, f.desenfileirar().getId());
            assertEquals(2L, f.desenfileirar().getId());
            f.enfileirar(requisicao(4, Prioridade.NORMAL));
            assertEquals(3L, f.desenfileirar().getId());
            assertEquals(4L, f.desenfileirar().getId());
            assertTrue(f.vazia());

            f.enfileirar(requisicao(5, Prioridade.NORMAL)); // reaproveita após esvaziar
            assertEquals(5L, f.frente().getId());
            assertEquals(1, f.paraLista().size());
        }
    }

    @Nested
    @DisplayName("Pilha (histórico)")
    class Pilha {

        @Test
        void lifo() {
            PilhaHistorico p = new PilhaHistorico();
            assertTrue(p.vazia());
            assertNull(p.topo());
            assertNull(p.desempilhar());

            p.empilhar(new OperacaoEstoque(TipoOperacao.ENTRADA, bolsa(1, HemoComponente.HEMACIAS, TipoSanguineo.A_POS)));
            p.empilhar(new OperacaoEstoque(TipoOperacao.SAIDA, bolsa(2, HemoComponente.PLASMA, TipoSanguineo.O_POS)));
            assertEquals(2, p.tamanho());
            assertEquals(2L, p.topo().bolsa().getId());

            OperacaoEstoque op = p.desempilhar();
            assertEquals(TipoOperacao.SAIDA, op.tipo());
            assertEquals(2L, op.bolsa().getId());
            assertEquals(TipoOperacao.ENTRADA, p.desempilhar().tipo());
            assertTrue(p.vazia());
        }
    }

    @Nested
    @DisplayName("Gestão (lista + pilha, desfazer)")
    class Gestao {

        @Test
        void entradaSaidaEDesfazer() {
            GestaoEstoque g = new GestaoEstoque();
            assertTrue(g.registrarEntrada(bolsa(1, HemoComponente.HEMACIAS, TipoSanguineo.O_NEG)));
            assertTrue(g.registrarEntrada(bolsa(2, HemoComponente.PLASMA, TipoSanguineo.A_POS)));
            assertFalse(g.registrarEntrada(bolsa(2, HemoComponente.PLASMA, TipoSanguineo.A_POS)));
            assertEquals(2, g.getHistorico().tamanho(), "duplicada não entra no histórico");
            assertEquals(1L, g.registrarSaida(1).getId());
            assertNull(g.registrarSaida(99));
            assertEquals(1, g.getEstoque().tamanho());
            assertEquals(3, g.getHistorico().tamanho());

            assertEquals(TipoOperacao.SAIDA, g.desfazer().tipo());
            assertNotNull(g.getEstoque().buscar(1));
            assertEquals(TipoOperacao.ENTRADA, g.desfazer().tipo());
            assertNull(g.getEstoque().buscar(2));
            assertNotNull(g.desfazer());
            assertTrue(g.getEstoque().vazia());
            assertNull(g.desfazer());
        }
    }

    @Nested
    @DisplayName("Árvore binária de busca (catálogo por id)")
    class Arvore {

        @Test
        void arvoreVazia() {
            ArvoreBolsas a = new ArvoreBolsas();
            assertEquals(0, a.tamanho());
            assertEquals(0, a.altura());
            assertNull(a.buscar(50));
            assertFalse(a.remover(50));
        }

        @Test
        void inserirBuscarRemoverEPercursoEmOrdem() {
            ArvoreBolsas a = new ArvoreBolsas();
            for (long id : new long[]{50, 30, 70, 20, 40, 60, 80}) {
                assertTrue(a.inserir(bolsa(id, HemoComponente.HEMACIAS, TipoSanguineo.A_POS)));
            }
            assertFalse(a.inserir(bolsa(40, HemoComponente.PLASMA, TipoSanguineo.O_NEG)));
            assertEquals(7, a.tamanho());
            assertEquals(3, a.altura());
            assertNotNull(a.buscar(60));
            assertNull(a.buscar(65));
            assertEquals(List.of(20L, 30L, 40L, 50L, 60L, 70L, 80L),
                    a.paraListaOrdenada().stream().map(Bolsa::getId).toList());

            assertTrue(a.remover(20)); // folha
            assertTrue(a.remover(30)); // um filho
            assertNotNull(a.buscar(40));
            assertTrue(a.remover(50)); // raiz com dois filhos
            assertEquals(60L, a.idRaiz(), "sucessor em-ordem sobe para a raiz");
            assertEquals(4, a.tamanho());
            assertEquals(List.of(40L, 60L, 70L, 80L),
                    a.paraListaOrdenada().stream().map(Bolsa::getId).toList());
        }

        @Test
        void piorCasoIdsOrdenadosDegeneramEmLista() {
            ArvoreBolsas a = new ArvoreBolsas();
            for (long id = 1; id <= 10; id++) {
                a.inserir(bolsa(id, HemoComponente.HEMACIAS, TipoSanguineo.A_POS));
            }
            assertEquals(10, a.altura());
        }
    }
}
