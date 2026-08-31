package com.hemorede.algoritmos;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Utilitário e fábrica de dados sintéticos definidos em {@code doc/escopo-grafo.md}.
 * <p>
 * Fornece o grafo de rotas com os 6 nós e suas respectivas distâncias,
 * além de um conjunto representativo de bolsas de sangue para testes de algoritmos
 * de estoque e ordenação FEFO.
 * </p>
 */
public final class DadosSinteticos {

    public static final String N0_HEMOCENTRO = "N0";
    public static final String N1_HOSPITAL_NORTE = "N1";
    public static final String N2_HOSPITAL_SUL = "N2";
    public static final String N3_HOSPITAL_LESTE = "N3";
    public static final String N4_HOSPITAL_OESTE = "N4";
    public static final String N5_HOSPITAL_CENTRAL_II = "N5";

    private DadosSinteticos() {
        // Construtor privado para classe utilitária
    }

    /**
     * Cria e retorna o {@link GrafoRotas} populado com os 6 nós e a matriz
     * de distâncias simétrica especificada em {@code doc/escopo-grafo.md}.
     *
     * @return Grafo de rotas pronto para cálculos de Dijkstra.
     */
    public static GrafoRotas criarGrafoRotaVital() {
        GrafoRotas grafo = new GrafoRotas();

        // Adiciona os 6 nós
        grafo.adicionarNo(N0_HEMOCENTRO);
        grafo.adicionarNo(N1_HOSPITAL_NORTE);
        grafo.adicionarNo(N2_HOSPITAL_SUL);
        grafo.adicionarNo(N3_HOSPITAL_LESTE);
        grafo.adicionarNo(N4_HOSPITAL_OESTE);
        grafo.adicionarNo(N5_HOSPITAL_CENTRAL_II);

        // Arestas a partir de N0 (Hemocentro)
        grafo.adicionarAresta(N0_HEMOCENTRO, N1_HOSPITAL_NORTE, 18.0);
        grafo.adicionarAresta(N0_HEMOCENTRO, N2_HOSPITAL_SUL, 15.0);
        grafo.adicionarAresta(N0_HEMOCENTRO, N3_HOSPITAL_LESTE, 20.0);
        grafo.adicionarAresta(N0_HEMOCENTRO, N4_HOSPITAL_OESTE, 17.0);
        grafo.adicionarAresta(N0_HEMOCENTRO, N5_HOSPITAL_CENTRAL_II, 10.0);

        // Arestas a partir de N1 (Hospital Norte)
        grafo.adicionarAresta(N1_HOSPITAL_NORTE, N2_HOSPITAL_SUL, 30.0);
        grafo.adicionarAresta(N1_HOSPITAL_NORTE, N3_HOSPITAL_LESTE, 25.0);
        grafo.adicionarAresta(N1_HOSPITAL_NORTE, N4_HOSPITAL_OESTE, 22.0);
        grafo.adicionarAresta(N1_HOSPITAL_NORTE, N5_HOSPITAL_CENTRAL_II, 20.0);

        // Arestas a partir de N2 (Hospital Sul)
        grafo.adicionarAresta(N2_HOSPITAL_SUL, N3_HOSPITAL_LESTE, 28.0);
        grafo.adicionarAresta(N2_HOSPITAL_SUL, N4_HOSPITAL_OESTE, 12.0);
        grafo.adicionarAresta(N2_HOSPITAL_SUL, N5_HOSPITAL_CENTRAL_II, 8.0);

        // Arestas a partir de N3 (Hospital Leste)
        grafo.adicionarAresta(N3_HOSPITAL_LESTE, N4_HOSPITAL_OESTE, 35.0);
        grafo.adicionarAresta(N3_HOSPITAL_LESTE, N5_HOSPITAL_CENTRAL_II, 14.0);

        // Arestas a partir de N4 (Hospital Oeste)
        grafo.adicionarAresta(N4_HOSPITAL_OESTE, N5_HOSPITAL_CENTRAL_II, 19.0);

        return grafo;
    }

    /**
     * Cria e retorna uma lista de bolsas de sangue sintéticas, abrangendo todos os 8 tipos sanguíneos
     * e com datas de validade variadas (algumas próximas ao vencimento e outras mais recentes).
     *
     * @return Lista com bolsas de sangue sintéticas prontas para testes.
     */
    public static List<Bolsa> criarBolsasExemplo() {
        LocalDate hoje = LocalDate.now();
        List<Bolsa> bolsas = new ArrayList<>();

        // O_POS - 3 bolsas com validades diferentes para testar ordenação FEFO
        bolsas.add(Bolsa.builder()
                .hemoComponente(HemoComponente.HEMACIAS)
                .tipoSanguineo(TipoSanguineo.O_POS)
                .dataColeta(hoje.minusDays(10))
                .dataValidade(hoje.plusDays(2)) // Próxima ao vencimento
                .status(StatusBolsa.DISPONIVEL)
                .build());

        bolsas.add(Bolsa.builder()
                .hemoComponente(HemoComponente.HEMACIAS)
                .tipoSanguineo(TipoSanguineo.O_POS)
                .dataColeta(hoje.minusDays(5))
                .dataValidade(hoje.plusDays(15))
                .status(StatusBolsa.DISPONIVEL)
                .build());

        bolsas.add(Bolsa.builder()
                .hemoComponente(HemoComponente.HEMACIAS)
                .tipoSanguineo(TipoSanguineo.O_POS)
                .dataColeta(hoje.minusDays(1))
                .dataValidade(hoje.plusDays(35))
                .status(StatusBolsa.DISPONIVEL)
                .build());

        // O_NEG - 2 bolsas
        bolsas.add(Bolsa.builder()
                .hemoComponente(HemoComponente.HEMACIAS)
                .tipoSanguineo(TipoSanguineo.O_NEG)
                .dataColeta(hoje.minusDays(8))
                .dataValidade(hoje.plusDays(4)) // Próxima ao vencimento
                .status(StatusBolsa.DISPONIVEL)
                .build());

        bolsas.add(Bolsa.builder()
                .hemoComponente(HemoComponente.HEMACIAS)
                .tipoSanguineo(TipoSanguineo.O_NEG)
                .dataColeta(hoje.minusDays(2))
                .dataValidade(hoje.plusDays(30))
                .status(StatusBolsa.DISPONIVEL)
                .build());

        // A_POS - 2 bolsas
        bolsas.add(Bolsa.builder()
                .hemoComponente(HemoComponente.HEMACIAS)
                .tipoSanguineo(TipoSanguineo.A_POS)
                .dataColeta(hoje.minusDays(12))
                .dataValidade(hoje.plusDays(3)) // Próxima ao vencimento
                .status(StatusBolsa.DISPONIVEL)
                .build());

        bolsas.add(Bolsa.builder()
                .hemoComponente(HemoComponente.HEMACIAS)
                .tipoSanguineo(TipoSanguineo.A_POS)
                .dataColeta(hoje.minusDays(3))
                .dataValidade(hoje.plusDays(25))
                .status(StatusBolsa.DISPONIVEL)
                .build());

        // A_NEG - 1 bolsa
        bolsas.add(Bolsa.builder()
                .hemoComponente(HemoComponente.HEMACIAS)
                .tipoSanguineo(TipoSanguineo.A_NEG)
                .dataColeta(hoje.minusDays(6))
                .dataValidade(hoje.plusDays(18))
                .status(StatusBolsa.DISPONIVEL)
                .build());

        // B_POS - 1 bolsa
        bolsas.add(Bolsa.builder()
                .hemoComponente(HemoComponente.HEMACIAS)
                .tipoSanguineo(TipoSanguineo.B_POS)
                .dataColeta(hoje.minusDays(4))
                .dataValidade(hoje.plusDays(22))
                .status(StatusBolsa.DISPONIVEL)
                .build());

        // B_NEG - 1 bolsa
        bolsas.add(Bolsa.builder()
                .hemoComponente(HemoComponente.HEMACIAS)
                .tipoSanguineo(TipoSanguineo.B_NEG)
                .dataColeta(hoje.minusDays(7))
                .dataValidade(hoje.plusDays(14))
                .status(StatusBolsa.DISPONIVEL)
                .build());

        // AB_POS - 1 bolsa
        bolsas.add(Bolsa.builder()
                .hemoComponente(HemoComponente.HEMACIAS)
                .tipoSanguineo(TipoSanguineo.AB_POS)
                .dataColeta(hoje.minusDays(5))
                .dataValidade(hoje.plusDays(28))
                .status(StatusBolsa.DISPONIVEL)
                .build());

        // AB_NEG - 1 bolsa
        bolsas.add(Bolsa.builder()
                .hemoComponente(HemoComponente.HEMACIAS)
                .tipoSanguineo(TipoSanguineo.AB_NEG)
                .dataColeta(hoje.minusDays(9))
                .dataValidade(hoje.plusDays(7))
                .status(StatusBolsa.DISPONIVEL)
                .build());

        return Collections.unmodifiableList(bolsas);
    }
}
