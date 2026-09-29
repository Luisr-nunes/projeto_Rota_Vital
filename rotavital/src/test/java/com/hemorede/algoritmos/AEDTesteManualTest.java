package com.hemorede.algoritmos;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.TipoRefrigeracao;
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

@DisplayName("Testes Automatizados de AED (Grafo, FEFO e Índice de Estoque)")
class AEDTesteManualTest {

    private static Bolsa bolsaComColeta(Long id, TipoSanguineo tipo, LocalDate coleta, LocalDate validade) {
        return Bolsa.builder()
                .id(id)
                .tipoSanguineo(tipo)
                .hemoComponente(HemoComponente.HEMACIAS)
                .dataColeta(coleta)
                .dataValidade(validade)
                .build();
    }

    private static Bolsa bolsaComId(Long id, TipoSanguineo tipo, LocalDate validade) {
        return bolsaComColeta(id, tipo, LocalDate.now().minusDays(1), validade);
    }

    private static List<Long> drenarIds(FilaFEFO fila) {
        List<Long> ids = new ArrayList<>();
        while (!fila.estaVazia()) {
            ids.add(fila.alocar().getId());
        }
        return ids;
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

    @Nested
    @DisplayName("4. Casos de Borda - FEFO com empate de validade")
    class FefoEmpateTests {

        @Test
        @DisplayName("Mesma validade: a bolsa com coleta mais antiga sai primeiro")
        void empateDeValidadeDesempataPelaDataDeColeta() {
            LocalDate hoje = LocalDate.now();
            LocalDate validade = hoje.plusDays(7);
            Bolsa coletaRecente = bolsaComColeta(1L, TipoSanguineo.O_POS, hoje.minusDays(1), validade);
            Bolsa coletaAntiga = bolsaComColeta(2L, TipoSanguineo.O_POS, hoje.minusDays(10), validade);

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
            Bolsa b3 = bolsaComColeta(3L, TipoSanguineo.O_POS, hoje.minusDays(2), hoje.plusDays(7));
            Bolsa b1 = bolsaComColeta(1L, TipoSanguineo.O_POS, hoje.minusDays(2), hoje.plusDays(7));
            Bolsa b2 = bolsaComColeta(2L, TipoSanguineo.O_POS, hoje.minusDays(2), hoje.plusDays(7));

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
                    bolsaComColeta(10L, TipoSanguineo.A_POS, hoje.minusDays(3), validade),
                    bolsaComColeta(11L, TipoSanguineo.A_POS, hoje.minusDays(3), validade),
                    bolsaComColeta(12L, TipoSanguineo.A_POS, hoje.minusDays(6), validade),
                    bolsaComColeta(13L, TipoSanguineo.A_POS, hoje.minusDays(1), hoje.plusDays(2))
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
                fila.inserir(bolsaComColeta(i, TipoSanguineo.B_POS, hoje, hoje.plusDays(3)));
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
                    fila.inserir(bolsaComColeta(1L, TipoSanguineo.O_POS, LocalDate.now(), null)));
            assertTrue(fila.estaVazia());
        }

        @Test
        @DisplayName("peek em fila vazia retorna null e não lança exceção")
        void peekEmFilaVazia() {
            assertNull(new FilaFEFO().peek());
        }

        @Test
        @DisplayName("peek não remove o elemento")
        void peekNaoRemove() {
            FilaFEFO fila = new FilaFEFO();
            fila.inserir(bolsaComId(1L, TipoSanguineo.O_POS, LocalDate.now().plusDays(3)));
            assertNotNull(fila.peek());
            assertEquals(1, fila.tamanho());
        }

        @Test
        @DisplayName("proximasAoVencimento rejeita limite negativo")
        void proximasAoVencimentoRejeitaNegativo() {
            assertThrows(IllegalArgumentException.class, () -> new FilaFEFO().proximasAoVencimento(-1));
        }

        @Test
        @DisplayName("proximasAoVencimento inclui exatamente o dia-limite e exclui o dia seguinte")
        void proximasAoVencimentoFronteira() {
            LocalDate hoje = LocalDate.now();
            FilaFEFO fila = new FilaFEFO();
            fila.inserir(bolsaComId(1L, TipoSanguineo.O_POS, hoje.plusDays(5)));
            fila.inserir(bolsaComId(2L, TipoSanguineo.O_POS, hoje.plusDays(6)));

            List<Bolsa> emRisco = fila.proximasAoVencimento(5);

            assertEquals(1, emRisco.size());
            assertEquals(Long.valueOf(1L), emRisco.get(0).getId());
        }

        @Test
        @DisplayName("proximasAoVencimento(0) inclui bolsa que vence hoje e bolsa já vencida")
        void proximasAoVencimentoLimiteZero() {
            LocalDate hoje = LocalDate.now();
            FilaFEFO fila = new FilaFEFO();
            fila.inserir(bolsaComId(1L, TipoSanguineo.O_POS, hoje));
            fila.inserir(bolsaComId(2L, TipoSanguineo.O_POS, hoje.minusDays(2)));
            fila.inserir(bolsaComId(3L, TipoSanguineo.O_POS, hoje.plusDays(1)));

            List<Bolsa> emRisco = fila.proximasAoVencimento(0);

            assertEquals(List.of(2L, 1L), emRisco.stream().map(Bolsa::getId).toList());
        }
    }

    @Nested
    @DisplayName("5. Casos de Borda - Índice de Estoque (tipo sem bolsas)")
    class IndiceSemEstoqueTests {

        @Test
        @DisplayName("Consulta de tipo sem bolsas retorna fila vazia e lista vazia, sem exceção")
        void consultaDeTipoSemBolsasRetornaVazio() {
            IndiceEstoque indice = new IndiceEstoque();
            indice.adicionarBolsa(bolsaComId(1L, TipoSanguineo.O_POS, LocalDate.now().plusDays(5)));

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
            indice.adicionarBolsa(bolsaComId(1L, TipoSanguineo.A_NEG, LocalDate.now().plusDays(5)));

            indice.alocarBolsa(TipoSanguineo.A_NEG);

            assertTrue(indice.consultarEstoque(TipoSanguineo.A_NEG).proximasAoVencimento(30).isEmpty());
            assertEquals(0, indice.totalDisponivelPorTipo(TipoSanguineo.A_NEG));
            assertThrows(EstoqueInsuficienteException.class, () -> indice.alocarBolsa(TipoSanguineo.A_NEG));
        }

        @Test
        @DisplayName("Tipo sem estoque não interfere nos demais tipos")
        void tipoSemEstoqueNaoAfetaOutros() {
            IndiceEstoque indice = new IndiceEstoque();
            indice.adicionarBolsa(bolsaComId(1L, TipoSanguineo.O_POS, LocalDate.now().plusDays(5)));

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

        @Test
        @DisplayName("adicionarBolsa rejeita bolsa nula e bolsa sem tipo sanguíneo")
        void adicionarBolsaRejeitaInvalidos() {
            IndiceEstoque indice = new IndiceEstoque();
            assertThrows(IllegalArgumentException.class, () -> indice.adicionarBolsa(null));
            assertThrows(IllegalArgumentException.class, () ->
                    indice.adicionarBolsa(bolsaComId(1L, null, LocalDate.now().plusDays(3))));
            assertEquals(0, indice.totalGeral());
        }

        @Test
        @DisplayName("Cada tipo tem sua própria fila FEFO: alocar de um tipo não afeta os outros")
        void filasIndependentesPorTipo() {
            IndiceEstoque indice = new IndiceEstoque();
            LocalDate hoje = LocalDate.now();
            indice.adicionarBolsa(bolsaComId(1L, TipoSanguineo.O_POS, hoje.plusDays(9)));
            indice.adicionarBolsa(bolsaComId(2L, TipoSanguineo.A_POS, hoje.plusDays(1)));

            assertEquals(Long.valueOf(1L), indice.alocarBolsa(TipoSanguineo.O_POS).getId());
            assertEquals(1, indice.totalDisponivelPorTipo(TipoSanguineo.A_POS));
            assertEquals(1, indice.totalGeral());
        }

        @Test
        @DisplayName("Mesma validade em um tipo: desempate determinístico também via índice")
        void empateDeValidadeViaIndice() {
            IndiceEstoque indice = new IndiceEstoque();
            LocalDate validade = LocalDate.now().plusDays(4);
            indice.adicionarBolsa(bolsaComId(7L, TipoSanguineo.B_POS, validade));
            indice.adicionarBolsa(bolsaComId(5L, TipoSanguineo.B_POS, validade));
            indice.adicionarBolsa(bolsaComId(6L, TipoSanguineo.B_POS, validade));

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
                indice.adicionarBolsa(bolsaComId(id++, tipo, LocalDate.now().plusDays(5)));
            }
            assertEquals(TipoSanguineo.values().length, indice.totalGeral());
        }
    }

    @Nested
    @DisplayName("6. Casos de Borda - Dijkstra (nó inalcançável, HU05 cenário 2)")
    class DijkstraSemRotaTests {

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
    @DisplayName("7. Domínio - TipoSanguineo e HemoComponente")
    class DominioTests {

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
