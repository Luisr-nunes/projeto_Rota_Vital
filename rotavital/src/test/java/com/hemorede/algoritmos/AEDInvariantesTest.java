package com.hemorede.algoritmos;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.TipoRefrigeracao;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Validações de entrada e invariantes das estruturas de AED e da matriz de compatibilidade sanguínea.
 */
@DisplayName("Validações e invariantes (GrafoRotas, FilaFEFO, IndiceEstoque, TipoSanguineo, HemoComponente)")
class AEDInvariantesTest {

    private static Bolsa bolsa(Long id, TipoSanguineo tipo, LocalDate validade) {
        return Bolsa.builder()
                .id(id)
                .tipoSanguineo(tipo)
                .hemoComponente(HemoComponente.HEMACIAS)
                .dataColeta(LocalDate.now().minusDays(1))
                .dataValidade(validade)
                .build();
    }

    @Nested
    @DisplayName("GrafoRotas")
    class Grafo {

        @Test
        @DisplayName("adicionarNo rejeita id nulo, vazio ou em branco")
        void adicionarNoRejeitaIdInvalido() {
            GrafoRotas grafo = new GrafoRotas();
            assertThrows(IllegalArgumentException.class, () -> grafo.adicionarNo(null));
            assertThrows(IllegalArgumentException.class, () -> grafo.adicionarNo(""));
            assertThrows(IllegalArgumentException.class, () -> grafo.adicionarNo("   "));
            assertTrue(grafo.getNos().isEmpty());
        }

        @Test
        @DisplayName("adicionarAresta rejeita peso negativo e não cria nós")
        void adicionarArestaRejeitaPesoNegativo() {
            GrafoRotas grafo = new GrafoRotas();
            assertThrows(IllegalArgumentException.class, () -> grafo.adicionarAresta("A", "B", -1.0));
            assertTrue(grafo.getNos().isEmpty());
        }

        @Test
        @DisplayName("Aresta com peso zero é aceita e o custo da rota é zero")
        void arestaComPesoZero() {
            GrafoRotas grafo = new GrafoRotas();
            grafo.adicionarAresta("A", "B", 0.0);
            GrafoRotas.ResultadoRota rota = grafo.calcularMenorRota("A", "B");
            assertEquals(List.of("A", "B"), rota.caminho());
            assertEquals(0.0, rota.custoTotal(), 0.0001);
        }

        @Test
        @DisplayName("Arestas são bidirecionais: a rota de ida e a de volta têm o mesmo custo")
        void arestasSaoSimetricas() {
            GrafoRotas grafo = DadosSinteticos.criarGrafoRotaVital();
            for (String a : grafo.getNos()) {
                for (String b : grafo.getNos()) {
                    assertEquals(
                            grafo.calcularMenorRota(a, b).custoTotal(),
                            grafo.calcularMenorRota(b, a).custoTotal(), 0.0001, a + "<->" + b);
                }
            }
        }

        @Test
        @DisplayName("Readicionar uma aresta substitui o peso anterior nos dois sentidos")
        void readicionarArestaSubstituiPeso() {
            GrafoRotas grafo = new GrafoRotas();
            grafo.adicionarAresta("A", "B", 10.0);
            grafo.adicionarAresta("B", "A", 4.0);
            assertEquals(4.0, grafo.calcularMenorRota("A", "B").custoTotal(), 0.0001);
            assertEquals(4.0, grafo.getVizinhos("A").get("B"), 0.0001);
            assertEquals(4.0, grafo.getVizinhos("B").get("A"), 0.0001);
        }

        @Test
        @DisplayName("Empate de custo entre caminhos: o custo é o mínimo e o caminho é válido")
        void empateDeCustoEntreCaminhos() {
            GrafoRotas grafo = new GrafoRotas();
            grafo.adicionarAresta("S", "X", 1.0);
            grafo.adicionarAresta("X", "T", 1.0);
            grafo.adicionarAresta("S", "Y", 1.0);
            grafo.adicionarAresta("Y", "T", 1.0);

            GrafoRotas.ResultadoRota rota = grafo.calcularMenorRota("S", "T");

            assertEquals(2.0, rota.custoTotal(), 0.0001);
            assertEquals(3, rota.caminho().size());
            assertEquals("S", rota.caminho().get(0));
            assertEquals("T", rota.caminho().get(2));
            assertTrue(List.of("X", "Y").contains(rota.caminho().get(1)));
        }

        @Test
        @DisplayName("Chamadas repetidas não alteram o grafo nem o resultado")
        void chamadasRepetidasSaoIdempotentes() {
            GrafoRotas grafo = DadosSinteticos.criarGrafoRotaVital();
            int nos = grafo.getNos().size();
            GrafoRotas.ResultadoRota primeira = grafo.calcularMenorRota("N1", "N2");
            GrafoRotas.ResultadoRota segunda = grafo.calcularMenorRota("N1", "N2");
            assertEquals(primeira, segunda);
            assertEquals(nos, grafo.getNos().size());
        }

        @Test
        @DisplayName("getNos e getVizinhos retornam visões imutáveis; vizinhos de nó desconhecido é vazio")
        void visoesImutaveis() {
            GrafoRotas grafo = new GrafoRotas();
            grafo.adicionarAresta("A", "B", 1.0);
            assertThrows(UnsupportedOperationException.class, () -> grafo.getNos().add("Z"));
            assertThrows(UnsupportedOperationException.class, () -> grafo.getVizinhos("A").put("Z", 1.0));
            assertTrue(grafo.getVizinhos("INEXISTENTE").isEmpty());
        }

        @Test
        @DisplayName("Todos os hospitais do grafo sintético são alcançáveis a partir do Hemocentro")
        void grafoSinteticoEhConexo() {
            GrafoRotas grafo = DadosSinteticos.criarGrafoRotaVital();
            for (String no : grafo.getNos()) {
                GrafoRotas.ResultadoRota rota = grafo.calcularMenorRota(DadosSinteticos.N0_HEMOCENTRO, no);
                assertFalse(rota.caminho().isEmpty(), "sem rota para " + no);
                assertFalse(Double.isInfinite(rota.custoTotal()), "custo infinito para " + no);
            }
        }
    }

    @Nested
    @DisplayName("FilaFEFO")
    class Fefo {

        @Test
        @DisplayName("peek em fila vazia retorna null e não lança exceção")
        void peekEmFilaVazia() {
            assertNull(new FilaFEFO().peek());
        }

        @Test
        @DisplayName("peek não remove o elemento")
        void peekNaoRemove() {
            FilaFEFO fila = new FilaFEFO();
            fila.inserir(bolsa(1L, TipoSanguineo.O_POS, LocalDate.now().plusDays(3)));
            assertNotNull(fila.peek());
            assertEquals(1, fila.tamanho());
        }

        @Test
        @DisplayName("proximasAoVencimento rejeita limite negativo")
        void proximasAoVencimentoRejeitaNegativo() {
            assertThrows(IllegalArgumentException.class, () -> new FilaFEFO().proximasAoVencimento(-1));
        }

        @Test
        @DisplayName("proximasAoVencimento em fila vazia retorna lista vazia")
        void proximasAoVencimentoFilaVazia() {
            assertTrue(new FilaFEFO().proximasAoVencimento(10).isEmpty());
        }

        @Test
        @DisplayName("proximasAoVencimento inclui exatamente o dia-limite e exclui o dia seguinte")
        void proximasAoVencimentoFronteira() {
            LocalDate hoje = LocalDate.now();
            FilaFEFO fila = new FilaFEFO();
            fila.inserir(bolsa(1L, TipoSanguineo.O_POS, hoje.plusDays(5)));
            fila.inserir(bolsa(2L, TipoSanguineo.O_POS, hoje.plusDays(6)));

            List<Bolsa> emRisco = fila.proximasAoVencimento(5);

            assertEquals(1, emRisco.size());
            assertEquals(Long.valueOf(1L), emRisco.get(0).getId());
        }

        @Test
        @DisplayName("proximasAoVencimento(0) inclui bolsa que vence hoje e bolsa já vencida")
        void proximasAoVencimentoLimiteZero() {
            LocalDate hoje = LocalDate.now();
            FilaFEFO fila = new FilaFEFO();
            fila.inserir(bolsa(1L, TipoSanguineo.O_POS, hoje));
            fila.inserir(bolsa(2L, TipoSanguineo.O_POS, hoje.minusDays(2)));
            fila.inserir(bolsa(3L, TipoSanguineo.O_POS, hoje.plusDays(1)));

            List<Bolsa> emRisco = fila.proximasAoVencimento(0);

            assertEquals(List.of(2L, 1L), emRisco.stream().map(Bolsa::getId).toList());
        }

        @Test
        @DisplayName("Fila com um único elemento: aloca e fica vazia")
        void umUnicoElemento() {
            FilaFEFO fila = new FilaFEFO();
            Bolsa b = bolsa(1L, TipoSanguineo.O_POS, LocalDate.now().plusDays(1));
            fila.inserir(b);
            assertSame(b, fila.alocar());
            assertTrue(fila.estaVazia());
            assertNull(fila.peek());
        }
    }

    @Nested
    @DisplayName("IndiceEstoque")
    class Indice {

        @Test
        @DisplayName("adicionarBolsa rejeita bolsa nula e bolsa sem tipo sanguíneo")
        void adicionarBolsaRejeitaInvalidos() {
            IndiceEstoque indice = new IndiceEstoque();
            assertThrows(IllegalArgumentException.class, () -> indice.adicionarBolsa(null));
            assertThrows(IllegalArgumentException.class, () ->
                    indice.adicionarBolsa(bolsa(1L, null, LocalDate.now().plusDays(3))));
            assertEquals(0, indice.totalGeral());
        }

        @Test
        @DisplayName("Cada tipo tem sua própria fila FEFO: alocar de um tipo não afeta os outros")
        void filasIndependentesPorTipo() {
            IndiceEstoque indice = new IndiceEstoque();
            LocalDate hoje = LocalDate.now();
            indice.adicionarBolsa(bolsa(1L, TipoSanguineo.O_POS, hoje.plusDays(9)));
            indice.adicionarBolsa(bolsa(2L, TipoSanguineo.A_POS, hoje.plusDays(1)));

            assertEquals(Long.valueOf(1L), indice.alocarBolsa(TipoSanguineo.O_POS).getId());
            assertEquals(1, indice.totalDisponivelPorTipo(TipoSanguineo.A_POS));
            assertEquals(1, indice.totalGeral());
        }

        @Test
        @DisplayName("Mesma validade em um tipo: desempate determinístico também via índice")
        void empateDeValidadeViaIndice() {
            IndiceEstoque indice = new IndiceEstoque();
            LocalDate validade = LocalDate.now().plusDays(4);
            indice.adicionarBolsa(bolsa(7L, TipoSanguineo.B_POS, validade));
            indice.adicionarBolsa(bolsa(5L, TipoSanguineo.B_POS, validade));
            indice.adicionarBolsa(bolsa(6L, TipoSanguineo.B_POS, validade));

            assertEquals(Long.valueOf(5L), indice.alocarBolsa(TipoSanguineo.B_POS).getId());
            assertEquals(Long.valueOf(6L), indice.alocarBolsa(TipoSanguineo.B_POS).getId());
            assertEquals(Long.valueOf(7L), indice.alocarBolsa(TipoSanguineo.B_POS).getId());
        }

        @Test
        @DisplayName("totalGeral soma todos os tipos")
        void totalGeralSomaTodosOsTipos() {
            IndiceEstoque indice = new IndiceEstoque();
            long id = 1;
            for (TipoSanguineo tipo : TipoSanguineo.values()) {
                indice.adicionarBolsa(bolsa(id++, tipo, LocalDate.now().plusDays(5)));
            }
            assertEquals(TipoSanguineo.values().length, indice.totalGeral());
        }
    }

    @Nested
    @DisplayName("TipoSanguineo e HemoComponente")
    class Dominio {

        @Test
        @DisplayName("Todo tipo é compatível consigo mesmo")
        void reflexividade() {
            for (TipoSanguineo t : TipoSanguineo.values()) {
                assertTrue(TipoSanguineo.compativel(t, t), t.name());
            }
        }

        @Test
        @DisplayName("O_NEG é doador universal")
        void doadorUniversal() {
            for (TipoSanguineo receptor : TipoSanguineo.values()) {
                assertTrue(TipoSanguineo.compativel(TipoSanguineo.O_NEG, receptor), receptor.name());
            }
        }

        @Test
        @DisplayName("AB_POS é receptor universal")
        void receptorUniversal() {
            for (TipoSanguineo doador : TipoSanguineo.values()) {
                assertTrue(TipoSanguineo.compativel(doador, TipoSanguineo.AB_POS), doador.name());
            }
        }

        @Test
        @DisplayName("O_NEG só recebe de O_NEG")
        void oNegSoRecebeDeONeg() {
            for (TipoSanguineo doador : TipoSanguineo.values()) {
                assertEquals(doador == TipoSanguineo.O_NEG,
                        TipoSanguineo.compativel(doador, TipoSanguineo.O_NEG), doador.name());
            }
        }

        @Test
        @DisplayName("Incompatibilidades clássicas são rejeitadas")
        void incompatibilidades() {
            assertFalse(TipoSanguineo.compativel(TipoSanguineo.A_POS, TipoSanguineo.A_NEG)); // Rh+ para Rh-
            assertFalse(TipoSanguineo.compativel(TipoSanguineo.A_POS, TipoSanguineo.B_POS));
            assertFalse(TipoSanguineo.compativel(TipoSanguineo.B_NEG, TipoSanguineo.A_NEG));
            assertFalse(TipoSanguineo.compativel(TipoSanguineo.AB_POS, TipoSanguineo.O_POS));
            assertFalse(TipoSanguineo.compativel(TipoSanguineo.AB_POS, TipoSanguineo.AB_NEG)); // Rh+ para Rh-
        }

        @Test
        @DisplayName("doadoresCompativeis é consistente com compativel, inclui o próprio tipo e termina em O_NEG")
        void listaConsistenteComPredicado() {
            for (TipoSanguineo receptor : TipoSanguineo.values()) {
                List<TipoSanguineo> lista = TipoSanguineo.doadoresCompativeis(receptor);
                assertTrue(lista.contains(receptor), "deve conter o tipo exato de " + receptor);
                assertEquals(TipoSanguineo.O_NEG, lista.get(lista.size() - 1), "O_NEG (mais universal) por último");
                for (TipoSanguineo doador : TipoSanguineo.values()) {
                    assertEquals(lista.contains(doador), TipoSanguineo.compativel(doador, receptor),
                            doador + "->" + receptor);
                }
            }
        }

        @Test
        @DisplayName("HemoComponente: validade e refrigeração esperadas")
        void hemocomponentes() {
            assertEquals(42, HemoComponente.HEMACIAS.getValidadeEmDias());
            assertEquals(5, HemoComponente.PLAQUETAS.getValidadeEmDias());
            assertEquals(365, HemoComponente.PLASMA.getValidadeEmDias());
            assertEquals(365, HemoComponente.CRIOPRECIPITADO.getValidadeEmDias());
            assertEquals(TipoRefrigeracao.REFRIGERADO_2_6, HemoComponente.HEMACIAS.getRefrigeracaoExigida());
            assertEquals(TipoRefrigeracao.CONGELADO_MENOS_20, HemoComponente.PLASMA.getRefrigeracaoExigida());
            assertEquals(TipoRefrigeracao.AMBIENTE_CONTROLADO_20_24, HemoComponente.PLAQUETAS.getRefrigeracaoExigida());
        }
    }
}
