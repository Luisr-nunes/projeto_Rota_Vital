package com.hemorede.algoritmos;

import com.hemorede.domain.enums.TipoSanguineo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static com.hemorede.domain.enums.TipoSanguineo.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Matriz ABO/Rh 8x8 (PI2 B2): 8 receptores x 8 doadores = 64 pares, cada um
 * verificado como compativel ou incompativel contra a tabela de referencia
 * escrita a mao (doc/aed-u2-compatibilidade.md, secao 2.3), que e independente
 * da implementacao.
 */
class MatrizCompatibilidadeTest {

    /** Receptor -> doadores que ele pode receber (hemacias). */
    private static final Map<TipoSanguineo, Set<TipoSanguineo>> ESPERADO = new EnumMap<>(TipoSanguineo.class);

    static {
        ESPERADO.put(O_NEG, Set.of(O_NEG));
        ESPERADO.put(O_POS, Set.of(O_POS, O_NEG));
        ESPERADO.put(A_NEG, Set.of(A_NEG, O_NEG));
        ESPERADO.put(A_POS, Set.of(A_POS, A_NEG, O_POS, O_NEG));
        ESPERADO.put(B_NEG, Set.of(B_NEG, O_NEG));
        ESPERADO.put(B_POS, Set.of(B_POS, B_NEG, O_POS, O_NEG));
        ESPERADO.put(AB_NEG, Set.of(A_NEG, B_NEG, AB_NEG, O_NEG));
        ESPERADO.put(AB_POS, Set.of(A_POS, A_NEG, B_POS, B_NEG, AB_POS, AB_NEG, O_POS, O_NEG));
    }

    private final MatrizCompatibilidade matriz = new MatrizCompatibilidade();

    /** Os 64 pares (receptor, doador) com o resultado esperado. */
    static Stream<Arguments> todosOsPares() {
        List<Arguments> pares = new ArrayList<>();
        for (TipoSanguineo receptor : TipoSanguineo.values()) {
            for (TipoSanguineo doador : TipoSanguineo.values()) {
                pares.add(Arguments.of(receptor, doador, ESPERADO.get(receptor).contains(doador)));
            }
        }
        return pares.stream();
    }

    @Nested
    @DisplayName("Matriz 8x8 (doador x receptor)")
    class Matriz {

        @ParameterizedTest(name = "doador {1} -> receptor {0}: compativel = {2}")
        @MethodSource("com.hemorede.algoritmos.MatrizCompatibilidadeTest#todosOsPares")
        @DisplayName("os 64 pares coincidem com a tabela de referencia")
        void sessentaEQuatroPares(TipoSanguineo receptor, TipoSanguineo doador, boolean esperado) {
            assertEquals(esperado, matriz.compativel(doador, receptor),
                    "doador " + doador + " -> receptor " + receptor);
        }

        @Test
        @DisplayName("ha exatamente 27 pares compativeis e 37 incompativeis")
        void totalDePares() {
            int compativeis = 0;
            int incompativeis = 0;
            for (TipoSanguineo receptor : TipoSanguineo.values()) {
                for (TipoSanguineo doador : TipoSanguineo.values()) {
                    if (matriz.compativel(doador, receptor)) {
                        compativeis++;
                    } else {
                        incompativeis++;
                    }
                }
            }
            assertEquals(27, compativeis);
            assertEquals(37, incompativeis);
        }

        @ParameterizedTest
        @EnumSource(TipoSanguineo.class)
        @DisplayName("todo tipo e compativel com ele mesmo")
        void diagonal(TipoSanguineo tipo) {
            assertTrue(matriz.compativel(tipo, tipo));
        }

        @ParameterizedTest
        @EnumSource(TipoSanguineo.class)
        @DisplayName("O- doa para todos e AB+ recebe de todos")
        void universais(TipoSanguineo tipo) {
            assertTrue(matriz.compativel(O_NEG, tipo), "O- -> " + tipo);
            assertTrue(matriz.compativel(tipo, AB_POS), tipo + " -> AB+");
        }

        @Test
        @DisplayName("Rh+ nunca doa para Rh- e ABO diferente nao e compativel")
        void incompativeisClassicos() {
            assertFalse(matriz.compativel(A_POS, A_NEG));
            assertFalse(matriz.compativel(AB_POS, AB_NEG));
            assertFalse(matriz.compativel(A_POS, B_POS));
            assertFalse(matriz.compativel(B_NEG, A_NEG));
            assertFalse(matriz.compativel(AB_POS, O_POS));
            assertFalse(matriz.compativel(AB_NEG, O_NEG));
        }

        @Test
        @DisplayName("tipo nulo e tratado como incompativel")
        void nulos() {
            assertFalse(matriz.compativel(null, A_POS));
            assertFalse(matriz.compativel(A_POS, null));
            assertFalse(matriz.compativel(null, null));
        }

        @Test
        @DisplayName("e consistente com a regra ja existente em TipoSanguineo")
        void consistenteComTipoSanguineo() {
            for (TipoSanguineo receptor : TipoSanguineo.values()) {
                for (TipoSanguineo doador : TipoSanguineo.values()) {
                    assertEquals(TipoSanguineo.compativel(doador, receptor),
                            matriz.compativel(doador, receptor),
                            doador + " -> " + receptor);
                }
            }
        }
    }

    @Nested
    @DisplayName("Matching: doadores compativeis em ordem de preferencia")
    class Matching {

        @ParameterizedTest
        @EnumSource(TipoSanguineo.class)
        @DisplayName("retorna exatamente o conjunto de doadores compativeis, sem repetir")
        void conjuntoCompativel(TipoSanguineo receptor) {
            List<TipoSanguineo> doadores = matriz.matching(receptor);

            assertEquals(ESPERADO.get(receptor), Set.copyOf(doadores));
            assertEquals(doadores.size(), Set.copyOf(doadores).size(), "sem duplicados");
        }

        @ParameterizedTest
        @EnumSource(TipoSanguineo.class)
        @DisplayName("o tipo identico vem primeiro")
        void identicoPrimeiro(TipoSanguineo receptor) {
            assertEquals(receptor, matriz.matching(receptor).get(0));
        }

        @ParameterizedTest
        @EnumSource(TipoSanguineo.class)
        @DisplayName("O- (doador universal) vem por ultimo")
        void oNegativoPorUltimo(TipoSanguineo receptor) {
            List<TipoSanguineo> doadores = matriz.matching(receptor);

            assertEquals(O_NEG, doadores.get(doadores.size() - 1));
        }

        @Test
        @DisplayName("ordem exata para cada receptor")
        void ordemExata() {
            assertEquals(List.of(O_NEG), matriz.matching(O_NEG));
            assertEquals(List.of(O_POS, O_NEG), matriz.matching(O_POS));
            assertEquals(List.of(A_NEG, O_NEG), matriz.matching(A_NEG));
            assertEquals(List.of(A_POS, A_NEG, O_POS, O_NEG), matriz.matching(A_POS));
            assertEquals(List.of(B_NEG, O_NEG), matriz.matching(B_NEG));
            assertEquals(List.of(B_POS, B_NEG, O_POS, O_NEG), matriz.matching(B_POS));
            assertEquals(List.of(AB_NEG, A_NEG, B_NEG, O_NEG), matriz.matching(AB_NEG));
            assertEquals(List.of(AB_POS, A_POS, B_POS, AB_NEG, A_NEG, B_NEG, O_POS, O_NEG),
                    matriz.matching(AB_POS));
        }

        @Test
        @DisplayName("a lista devolvida e imutavel (nao expoe o estado interno)")
        void imutavel() {
            List<TipoSanguineo> doadores = matriz.matching(A_POS);

            assertThrows(UnsupportedOperationException.class, () -> doadores.add(B_POS));
            assertEquals(List.of(A_POS, A_NEG, O_POS, O_NEG), matriz.matching(A_POS));
        }

        @Test
        @DisplayName("receptor nulo devolve lista vazia")
        void receptorNulo() {
            assertTrue(matriz.matching(null).isEmpty());
        }
    }
}
