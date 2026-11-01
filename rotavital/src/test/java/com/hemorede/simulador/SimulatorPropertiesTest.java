package com.hemorede.simulador;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SimuladorPropertiesTest {

    private final ApplicationContextRunner runner =
            new ApplicationContextRunner().withUserConfiguration(SimuladorConfig.class);

    @Test
    @DisplayName("Sem propriedades: desligado, cenário BAIXO e parâmetros padrão")
    void deveUsarPadroesQuandoNadaConfigurado() {
        runner.run(context -> {
            SimuladorProperties props = context.getBean(SimuladorProperties.class);
            assertFalse(props.enabled());
            assertEquals(CenarioCarga.BAIXO, props.cenario());
            assertEquals(new SimuladorProperties.Cenario(10, 5000), props.ativo());
        });
    }

    @ParameterizedTest(name = "cenario={0} → {1} veículos, {2} ms")
    @CsvSource({"baixo, 2, 11", "moderado, 4, 22", "alto, 6, 33"})
    @DisplayName("Lê do yml o cenário escolhido, com veículos e intervalo próprios")
    void deveLerCenarioEscolhido(String nome, int veiculos, long intervaloMs) {
        runner.withPropertyValues(
                "hemorede.simulador.enabled=true",
                "hemorede.simulador.cenario=" + nome,
                "hemorede.simulador.cenarios.baixo.veiculos=2",
                "hemorede.simulador.cenarios.baixo.intervalo-ms=11",
                "hemorede.simulador.cenarios.moderado.veiculos=4",
                "hemorede.simulador.cenarios.moderado.intervalo-ms=22",
                "hemorede.simulador.cenarios.alto.veiculos=6",
                "hemorede.simulador.cenarios.alto.intervalo-ms=33"
        ).run(context -> {
            SimuladorProperties props = context.getBean(SimuladorProperties.class);
            assertTrue(props.enabled());
            assertEquals(veiculos, props.ativo().veiculos());
            assertEquals(intervaloMs, props.ativo().intervaloMs());
        });
    }

    @Test
    @DisplayName("Falha na subida se o cenário ativo não tem parâmetros")
    void deveFalharSeCenarioAtivoNaoConfigurado() {
        runner.withPropertyValues(
                "hemorede.simulador.cenario=alto",
                "hemorede.simulador.cenarios.baixo.veiculos=2",
                "hemorede.simulador.cenarios.baixo.intervalo-ms=11"
        ).run(context -> assertNotNull(context.getStartupFailure()));
    }

    @Test
    @DisplayName("Falha na subida se veículos for zero")
    void deveFalharComVeiculosInvalidos() {
        runner.withPropertyValues(
                "hemorede.simulador.cenarios.baixo.veiculos=0",
                "hemorede.simulador.cenarios.baixo.intervalo-ms=1000"
        ).run(context -> assertNotNull(context.getStartupFailure()));
    }
}