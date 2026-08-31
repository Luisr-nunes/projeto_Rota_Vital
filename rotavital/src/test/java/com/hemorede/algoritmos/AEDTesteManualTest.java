package com.hemorede.algoritmos;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.exception.EstoqueInsuficienteException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Testes Automatizados de AED (Grafo, FEFO e Índice de Estoque)")
class AEDTesteManualTest {

    @Nested
    @DisplayName("1. Testes de Roteirização e Dijkstra (GrafoRotas)")
    class DijkstraTests {

        @Test
        @DisplayName("Deve calcular menor rota direta do Hemocentro (N0) para Hospitais N5, N1 e N2")
        void deveCalcularMenorRotaDireta() {
            GrafoRotas grafo = DadosSinteticos.criarGrafoRotaVital();

            // N0 -> N5 (Custo direto: 10.0)
            GrafoRotas.ResultadoRota rotaN5 = grafo.calcularMenorRota(
                    DadosSinteticos.N0_HEMOCENTRO,
                    DadosSinteticos.N5_HOSPITAL_CENTRAL_II
            );
            assertEquals(10.0, rotaN5.custoTotal(), 0.001);
            assertEquals(List.of("N0", "N5"), rotaN5.caminho());

            // N0 -> N1 (Custo direto: 18.0)
            GrafoRotas.ResultadoRota rotaN1 = grafo.calcularMenorRota(
                    DadosSinteticos.N0_HEMOCENTRO,
                    DadosSinteticos.N1_HOSPITAL_NORTE
            );
            assertEquals(18.0, rotaN1.custoTotal(), 0.001);
            assertEquals(List.of("N0", "N1"), rotaN1.caminho());

            // N0 -> N2 (Custo direto: 15.0)
            GrafoRotas.ResultadoRota rotaN2 = grafo.calcularMenorRota(
                    DadosSinteticos.N0_HEMOCENTRO,
                    DadosSinteticos.N2_HOSPITAL_SUL
            );
            assertEquals(15.0, rotaN2.custoTotal(), 0.001);
            assertEquals(List.of("N0", "N2"), rotaN2.caminho());
        }

        @Test
        @DisplayName("Deve priorizar rota indireta quando o custo acumulado for menor que a aresta direta")
        void devePriorizarRotaIndiretaMaisBarata() {
            GrafoRotas grafo = DadosSinteticos.criarGrafoRotaVital();

            // Rota N1 -> N2: Direta custa 30.0, mas via N5 (N1 -> N5 -> N2) custa 20.0 + 8.0 = 28.0
            GrafoRotas.ResultadoRota rotaN1ParaN2 = grafo.calcularMenorRota(
                    DadosSinteticos.N1_HOSPITAL_NORTE,
                    DadosSinteticos.N2_HOSPITAL_SUL
            );
            assertEquals(28.0, rotaN1ParaN2.custoTotal(), 0.001);
            assertEquals(List.of("N1", "N5", "N2"), rotaN1ParaN2.caminho());

            // Rota N3 -> N4: Direta custa 35.0, mas via N5 (N3 -> N5 -> N4) custa 14.0 + 19.0 = 33.0
            GrafoRotas.ResultadoRota rotaN3ParaN4 = grafo.calcularMenorRota(
                    DadosSinteticos.N3_HOSPITAL_LESTE,
                    DadosSinteticos.N4_HOSPITAL_OESTE
            );
            assertEquals(33.0, rotaN3ParaN4.custoTotal(), 0.001);
            assertEquals(List.of("N3", "N5", "N4"), rotaN3ParaN4.caminho());
        }

        @Test
        @DisplayName("Deve lidar com nó isolado retornando custo infinito e caminho vazio")
        void deveLidarComNoIsoladoSemCaminho() {
            GrafoRotas grafoDesconectado = new GrafoRotas();
            grafoDesconectado.adicionarAresta("A", "B", 5.0);
            grafoDesconectado.adicionarNo("ISOLADO");

            GrafoRotas.ResultadoRota resultado = grafoDesconectado.calcularMenorRota("A", "ISOLADO");
            assertTrue(Double.isInfinite(resultado.custoTotal()));
            assertTrue(resultado.caminho().isEmpty());
        }

        @Test
        @DisplayName("Deve lançar exceção se origem ou destino não existirem no grafo")
        void deveLancarExcecaoParaNosInexistentes() {
            GrafoRotas grafo = DadosSinteticos.criarGrafoRotaVital();

            assertThrows(IllegalArgumentException.class, () ->
                    grafo.calcularMenorRota("INEXISTENTE", DadosSinteticos.N1_HOSPITAL_NORTE)
            );
            assertThrows(IllegalArgumentException.class, () ->
                    grafo.calcularMenorRota(DadosSinteticos.N0_HEMOCENTRO, "INEXISTENTE")
            );
        }
    }

    @Nested
    @DisplayName("2. Testes de Fila FEFO (FilaFEFO)")
    class FilaFEFOTests {

        @Test
        @DisplayName("Deve alocar sempre a bolsa com a menor data de validade primeiro (FEFO)")
        void deveAlocarBolsaComMenorDataValidadePrimeiro() {
            FilaFEFO fila = new FilaFEFO();
            LocalDate hoje = LocalDate.now();

            Bolsa bolsaValidadeLonga = Bolsa.builder()
                    .tipoSanguineo(TipoSanguineo.O_POS)
                    .hemoComponente(HemoComponente.HEMACIAS)
                    .dataValidade(hoje.plusDays(30))
                    .build();

            Bolsa bolsaValidadeCurta = Bolsa.builder()
                    .tipoSanguineo(TipoSanguineo.O_POS)
                    .hemoComponente(HemoComponente.HEMACIAS)
                    .dataValidade(hoje.plusDays(2))
                    .build();

            Bolsa bolsaValidadeMedia = Bolsa.builder()
                    .tipoSanguineo(TipoSanguineo.O_POS)
                    .hemoComponente(HemoComponente.HEMACIAS)
                    .dataValidade(hoje.plusDays(10))
                    .build();

            // Inserção fora de ordem
            fila.inserir(bolsaValidadeLonga);
            fila.inserir(bolsaValidadeCurta);
            fila.inserir(bolsaValidadeMedia);

            assertEquals(3, fila.tamanho());
            assertFalse(fila.estaVazia());

            // Alocações devem seguir estritamente a ordem de validade (2 dias -> 10 dias -> 30 dias)
            Bolsa primeiraAlocada = fila.alocar();
            assertEquals(hoje.plusDays(2), primeiraAlocada.getDataValidade());

            Bolsa segundaAlocada = fila.alocar();
            assertEquals(hoje.plusDays(10), segundaAlocada.getDataValidade());

            Bolsa terceiraAlocada = fila.alocar();
            assertEquals(hoje.plusDays(30), terceiraAlocada.getDataValidade());

            assertTrue(fila.estaVazia());
            assertEquals(0, fila.tamanho());
        }

        @Test
        @DisplayName("Deve lançar EstoqueInsuficienteException ao tentar alocar de uma fila vazia")
        void deveLancarExcecaoAoAlocarFilaVazia() {
            FilaFEFO fila = new FilaFEFO();
            assertTrue(fila.estaVazia());

            assertThrows(EstoqueInsuficienteException.class, fila::alocar);
        }

        @Test
        @DisplayName("Deve listar bolsas próximas ao vencimento sem removê-las da fila")
        void deveListarProximasAoVencimentoSemRemover() {
            FilaFEFO fila = new FilaFEFO();
            LocalDate hoje = LocalDate.now();

            Bolsa b1 = Bolsa.builder().dataValidade(hoje.plusDays(2)).build();
            Bolsa b2 = Bolsa.builder().dataValidade(hoje.plusDays(5)).build();
            Bolsa b3 = Bolsa.builder().dataValidade(hoje.plusDays(15)).build();

            fila.inserir(b1);
            fila.inserir(b2);
            fila.inserir(b3);

            List<Bolsa> emRisco = fila.proximasAoVencimento(5);
            assertEquals(2, emRisco.size());
            assertEquals(hoje.plusDays(2), emRisco.get(0).getDataValidade());
            assertEquals(hoje.plusDays(5), emRisco.get(1).getDataValidade());

            // Garante que os elementos permaneceram na fila
            assertEquals(3, fila.tamanho());
        }
    }

    @Nested
    @DisplayName("3. Testes de Índice de Estoque (IndiceEstoque)")
    class IndiceEstoqueTests {

        @Test
        @DisplayName("Deve indexar bolsas por tipo sanguíneo e alocar corretamente pela política FEFO")
        void deveIndexarEAlocarPorTipoSanguineo() {
            IndiceEstoque indice = new IndiceEstoque();
            List<Bolsa> bolsas = DadosSinteticos.criarBolsasExemplo();

            for (Bolsa bolsa : bolsas) {
                indice.adicionarBolsa(bolsa);
            }

            // O_POS possui 3 bolsas sintetizadas
            assertEquals(3, indice.totalDisponivelPorTipo(TipoSanguineo.O_POS));
            // O_NEG possui 2 bolsas sintetizadas
            assertEquals(2, indice.totalDisponivelPorTipo(TipoSanguineo.O_NEG));
            // A_POS possui 2 bolsas sintetizadas
            assertEquals(2, indice.totalDisponivelPorTipo(TipoSanguineo.A_POS));
            // Total geral deve ser 12 bolsas
            assertEquals(12, indice.totalGeral());

            LocalDate hoje = LocalDate.now();

            // Aloca O_POS: deve vir a bolsa de +2 dias
            Bolsa alocadaOPos1 = indice.alocarBolsa(TipoSanguineo.O_POS);
            assertEquals(TipoSanguineo.O_POS, alocadaOPos1.getTipoSanguineo());
            assertEquals(hoje.plusDays(2), alocadaOPos1.getDataValidade());
            assertEquals(2, indice.totalDisponivelPorTipo(TipoSanguineo.O_POS));

            // Aloca O_POS novamente: deve vir a bolsa de +15 dias
            Bolsa alocadaOPos2 = indice.alocarBolsa(TipoSanguineo.O_POS);
            assertEquals(hoje.plusDays(15), alocadaOPos2.getDataValidade());
            assertEquals(1, indice.totalDisponivelPorTipo(TipoSanguineo.O_POS));

            // Aloca O_NEG: deve vir a de +4 dias
            Bolsa alocadaONeg = indice.alocarBolsa(TipoSanguineo.O_NEG);
            assertEquals(TipoSanguineo.O_NEG, alocadaONeg.getTipoSanguineo());
            assertEquals(hoje.plusDays(4), alocadaONeg.getDataValidade());
            assertEquals(1, indice.totalDisponivelPorTipo(TipoSanguineo.O_NEG));
        }

        @Test
        @DisplayName("Deve lançar EstoqueInsuficienteException quando o tipo sanguíneo requisitado não possui estoque")
        void deveLancarExcecaoQuandoEstoqueDoTipoEstiverVazio() {
            IndiceEstoque indice = new IndiceEstoque();

            assertEquals(0, indice.totalDisponivelPorTipo(TipoSanguineo.AB_NEG));
            assertThrows(EstoqueInsuficienteException.class, () ->
                    indice.alocarBolsa(TipoSanguineo.AB_NEG)
            );
        }
    }
}
