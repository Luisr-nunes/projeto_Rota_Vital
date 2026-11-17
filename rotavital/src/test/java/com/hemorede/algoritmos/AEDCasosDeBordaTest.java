package com.hemorede.algoritmos;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.exception.EstoqueInsuficienteException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Casos de borda das estruturas de AED: {@link FilaFEFO}, {@link IndiceEstoque}
 * e {@link GrafoRotas} (Dijkstra).
 */
@DisplayName("Casos de borda de AED (FEFO, Índice de Estoque e Dijkstra)")
class AEDCasosDeBordaTest {

    private static Bolsa bolsa(Long id, TipoSanguineo tipo, LocalDate coleta, LocalDate validade) {
        return Bolsa.builder()
                .id(id)
                .tipoSanguineo(tipo)
                .hemoComponente(HemoComponente.HEMACIAS)
                .dataColeta(coleta)
                .dataValidade(validade)
                .build();
    }

    private static List<Long> drenarIds(FilaFEFO fila) {
        List<Long> ids = new ArrayList<>();
        while (!fila.estaVazia()) {
            ids.add(fila.alocar().getId());
        }
        return ids;
    }

    @Nested
    @DisplayName("FEFO com empate de validade")
    class FefoEmpate {

        @Test
        @DisplayName("Mesma validade: a bolsa com coleta mais antiga sai primeiro")
        void empateDeValidadeDesempataPelaDataDeColeta() {
            LocalDate hoje = LocalDate.now();
            LocalDate validade = hoje.plusDays(7);
            Bolsa coletaRecente = bolsa(1L, TipoSanguineo.O_POS, hoje.minusDays(1), validade);
            Bolsa coletaAntiga = bolsa(2L, TipoSanguineo.O_POS, hoje.minusDays(10), validade);

            FilaFEFO fila = new FilaFEFO();
            fila.inserir(coletaRecente);
            fila.inserir(coletaAntiga);

            assertSame(coletaAntiga, fila.peek());
            assertEquals(List.of(2L, 1L), drenarIds(fila));
        }

        @Test
        @DisplayName("Mesma validade e mesma coleta: desempata pelo menor id")
        void empateDeValidadeEColetaDesempataPeloId() {
            LocalDate hoje = LocalDate.now();
            Bolsa b3 = bolsa(3L, TipoSanguineo.O_POS, hoje.minusDays(2), hoje.plusDays(7));
            Bolsa b1 = bolsa(1L, TipoSanguineo.O_POS, hoje.minusDays(2), hoje.plusDays(7));
            Bolsa b2 = bolsa(2L, TipoSanguineo.O_POS, hoje.minusDays(2), hoje.plusDays(7));

            FilaFEFO fila = new FilaFEFO();
            fila.inserir(b3);
            fila.inserir(b1);
            fila.inserir(b2);

            assertEquals(List.of(1L, 2L, 3L), drenarIds(fila));
        }

        @Test
        @DisplayName("A ordem de alocação independe da ordem de inserção (todas as permutações)")
        void ordemDeterministicaParaQualquerOrdemDeInsercao() {
            LocalDate hoje = LocalDate.now();
            LocalDate validade = hoje.plusDays(5);
            List<Bolsa> base = List.of(
                    bolsa(10L, TipoSanguineo.A_POS, hoje.minusDays(3), validade),
                    bolsa(11L, TipoSanguineo.A_POS, hoje.minusDays(3), validade),
                    bolsa(12L, TipoSanguineo.A_POS, hoje.minusDays(6), validade),
                    bolsa(13L, TipoSanguineo.A_POS, hoje.minusDays(1), hoje.plusDays(2))
            );
            // validade menor primeiro (13); depois coleta mais antiga (12); depois id (10, 11)
            List<Long> esperado = List.of(13L, 12L, 10L, 11L);

            permutar(new ArrayList<>(base), 0, permutacao -> {
                FilaFEFO fila = new FilaFEFO();
                permutacao.forEach(fila::inserir);
                assertEquals(esperado, drenarIds(fila));
            });
        }

        @Test
        @DisplayName("Empate de validade não impede a alocação de todas as bolsas (nada é perdido)")
        void empateNaoPerdeBolsas() {
            LocalDate hoje = LocalDate.now();
            FilaFEFO fila = new FilaFEFO();
            for (long i = 1; i <= 5; i++) {
                fila.inserir(bolsa(i, TipoSanguineo.B_POS, hoje, hoje.plusDays(3)));
            }
            assertEquals(5, fila.tamanho());
            assertEquals(List.of(1L, 2L, 3L, 4L, 5L), drenarIds(fila));
            assertTrue(fila.estaVazia());
        }

        @Test
        @DisplayName("Rejeita bolsa nula e bolsa sem data de validade")
        void rejeitaEntradasInvalidas() {
            FilaFEFO fila = new FilaFEFO();
            assertThrows(IllegalArgumentException.class, () -> fila.inserir(null));
            assertThrows(IllegalArgumentException.class, () ->
                    fila.inserir(bolsa(1L, TipoSanguineo.O_POS, LocalDate.now(), null)));
            assertTrue(fila.estaVazia());
        }
    }

    @Nested
    @DisplayName("IndiceEstoque - tipo sem bolsas")
    class IndiceSemEstoque {

        @Test
        @DisplayName("Consulta de tipo sem bolsas retorna fila vazia e lista vazia, sem exceção")
        void consultaDeTipoSemBolsasRetornaVazio() {
            IndiceEstoque indice = new IndiceEstoque();
            indice.adicionarBolsa(bolsa(1L, TipoSanguineo.O_POS, LocalDate.now(), LocalDate.now().plusDays(5)));

            FilaFEFO fila = assertDoesNotThrow(() -> indice.consultarEstoque(TipoSanguineo.AB_NEG));

            assertNotNull(fila);
            assertTrue(fila.estaVazia());
            assertEquals(0, fila.tamanho());
            assertNull(fila.peek());
            assertEquals(List.of(), assertDoesNotThrow(() -> fila.proximasAoVencimento(30)));
            assertEquals(0, indice.totalDisponivelPorTipo(TipoSanguineo.AB_NEG));
        }

        @Test
        @DisplayName("Índice recém-criado: todos os tipos retornam vazio e total geral é zero")
        void indiceVazioParaTodosOsTipos() {
            IndiceEstoque indice = new IndiceEstoque();
            for (TipoSanguineo tipo : TipoSanguineo.values()) {
                assertEquals(0, indice.totalDisponivelPorTipo(tipo), "tipo " + tipo);
                assertTrue(indice.consultarEstoque(tipo).proximasAoVencimento(365).isEmpty(), "tipo " + tipo);
            }
            assertEquals(0, indice.totalGeral());
        }

        @Test
        @DisplayName("Esgotar um tipo: consulta passa a ser vazia e nova alocação lança EstoqueInsuficienteException")
        void tipoEsgotadoFicaVazio() {
            IndiceEstoque indice = new IndiceEstoque();
            indice.adicionarBolsa(bolsa(1L, TipoSanguineo.A_NEG, LocalDate.now(), LocalDate.now().plusDays(5)));

            indice.alocarBolsa(TipoSanguineo.A_NEG);

            assertTrue(indice.consultarEstoque(TipoSanguineo.A_NEG).proximasAoVencimento(30).isEmpty());
            assertEquals(0, indice.totalDisponivelPorTipo(TipoSanguineo.A_NEG));
            assertThrows(EstoqueInsuficienteException.class, () -> indice.alocarBolsa(TipoSanguineo.A_NEG));
        }

        @Test
        @DisplayName("Tipo sem estoque não interfere nos demais tipos")
        void tipoSemEstoqueNaoAfetaOutros() {
            IndiceEstoque indice = new IndiceEstoque();
            indice.adicionarBolsa(bolsa(1L, TipoSanguineo.O_POS, LocalDate.now(), LocalDate.now().plusDays(5)));

            assertThrows(EstoqueInsuficienteException.class, () -> indice.alocarBolsa(TipoSanguineo.B_NEG));
            assertEquals(1, indice.totalDisponivelPorTipo(TipoSanguineo.O_POS));
            assertEquals(1, indice.totalGeral());
        }

        @Test
        @DisplayName("Tipo nulo: consulta retorna null, total zero e alocação lança IllegalArgumentException")
        void tipoNulo() {
            IndiceEstoque indice = new IndiceEstoque();
            assertNull(indice.consultarEstoque(null));
            assertEquals(0, indice.totalDisponivelPorTipo(null));
            assertThrows(IllegalArgumentException.class, () -> indice.alocarBolsa(null));
        }
    }

    @Nested
    @DisplayName("Dijkstra - nó inalcançável (HU05, cenário 2: sem rota)")
    class DijkstraSemRota {

        private static final String HOSPITAL_ISOLADO = "N6";

        private void assertSemRota(GrafoRotas.ResultadoRota resultado) {
            assertTrue(resultado.caminho().isEmpty(), "caminho deve ser vazio");
            assertTrue(Double.isInfinite(resultado.custoTotal()), "custo deve ser infinito");
            assertEquals(Double.POSITIVE_INFINITY, resultado.custoTotal());
        }

        @Test
        @DisplayName("Hospital sem nenhuma aresta no grafo da Rota Vital: resultado 'sem rota' a partir do Hemocentro")
        void hospitalSemArestaNaoTemRota() {
            GrafoRotas grafo = DadosSinteticos.criarGrafoRotaVital();
            grafo.adicionarNo(HOSPITAL_ISOLADO);

            GrafoRotas.ResultadoRota resultado =
                    assertDoesNotThrow(() -> grafo.calcularMenorRota(DadosSinteticos.N0_HEMOCENTRO, HOSPITAL_ISOLADO));

            assertSemRota(resultado);
        }

        @Test
        @DisplayName("Sem rota também no sentido inverso (do nó isolado para o Hemocentro)")
        void semRotaNoSentidoInverso() {
            GrafoRotas grafo = DadosSinteticos.criarGrafoRotaVital();
            grafo.adicionarNo(HOSPITAL_ISOLADO);

            assertSemRota(grafo.calcularMenorRota(HOSPITAL_ISOLADO, DadosSinteticos.N0_HEMOCENTRO));
        }

        @Test
        @DisplayName("Componentes desconexos com arestas próprias: sem rota entre componentes")
        void componentesDesconexos() {
            GrafoRotas grafo = new GrafoRotas();
            grafo.adicionarAresta("A", "B", 3.0);
            grafo.adicionarAresta("B", "C", 4.0);
            grafo.adicionarAresta("X", "Y", 2.0);

            assertSemRota(grafo.calcularMenorRota("A", "Y"));
            assertSemRota(grafo.calcularMenorRota("X", "C"));
            // dentro do mesmo componente a rota segue existindo
            GrafoRotas.ResultadoRota interna = grafo.calcularMenorRota("A", "C");
            assertEquals(List.of("A", "B", "C"), interna.caminho());
            assertEquals(7.0, interna.custoTotal(), 0.001);
        }

        @Test
        @DisplayName("Ao ligar o nó isolado ao grafo, a rota passa a existir")
        void rotaPassaAExistirAposAdicionarAresta() {
            GrafoRotas grafo = DadosSinteticos.criarGrafoRotaVital();
            grafo.adicionarNo(HOSPITAL_ISOLADO);
            assertSemRota(grafo.calcularMenorRota(DadosSinteticos.N0_HEMOCENTRO, HOSPITAL_ISOLADO));

            grafo.adicionarAresta(DadosSinteticos.N5_HOSPITAL_CENTRAL_II, HOSPITAL_ISOLADO, 6.0);

            GrafoRotas.ResultadoRota resultado =
                    grafo.calcularMenorRota(DadosSinteticos.N0_HEMOCENTRO, HOSPITAL_ISOLADO);
            assertEquals(List.of("N0", "N5", "N6"), resultado.caminho());
            assertEquals(16.0, resultado.custoTotal(), 0.001);
        }

        @Test
        @DisplayName("Origem igual ao destino: rota trivial de custo zero, mesmo para nó isolado")
        void origemIgualDestinoEmNoIsolado() {
            GrafoRotas grafo = new GrafoRotas();
            grafo.adicionarNo("SOZINHO");

            GrafoRotas.ResultadoRota resultado = grafo.calcularMenorRota("SOZINHO", "SOZINHO");

            assertEquals(List.of("SOZINHO"), resultado.caminho());
            assertEquals(0.0, resultado.custoTotal(), 0.001);
        }

        @Test
        @DisplayName("Grafo vazio: origem e destino inexistentes lançam IllegalArgumentException")
        void grafoVazio() {
            GrafoRotas grafo = new GrafoRotas();
            assertThrows(IllegalArgumentException.class, () -> grafo.calcularMenorRota("N0", "N1"));
        }
    }

    private static void permutar(List<Bolsa> lista, int inicio, java.util.function.Consumer<List<Bolsa>> acao) {
        if (inicio == lista.size()) {
            acao.accept(new ArrayList<>(lista));
            return;
        }
        for (int i = inicio; i < lista.size(); i++) {
            Collections.swap(lista, inicio, i);
            permutar(lista, inicio + 1, acao);
            Collections.swap(lista, inicio, i);
        }
    }
}
