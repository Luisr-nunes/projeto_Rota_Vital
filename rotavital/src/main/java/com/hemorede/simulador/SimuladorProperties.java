package com.hemorede.simulador;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Configuração do simulador de veículos (PI2-170), prefixo {@code hemorede.simulador}.
 * Sem nada configurado: desligado, cenário BAIXO e parâmetros padrão.
 */
@ConfigurationProperties(prefix = "hemorede.simulador")
public record SimuladorProperties(
        boolean enabled,
        CenarioCarga cenario,
        Map<CenarioCarga, Cenario> cenarios) {

    /** Carga de um cenário: quantos veículos e de quanto em quanto tempo cada um envia. */
    public record Cenario(int veiculos, long intervaloMs) {
        public Cenario {
            if (veiculos < 1) {
                throw new IllegalArgumentException("veiculos deve ser >= 1, mas foi " + veiculos);
            }
            if (intervaloMs < 1) {
                throw new IllegalArgumentException("intervalo-ms deve ser >= 1, mas foi " + intervaloMs);
            }
        }
    }

    public SimuladorProperties {
        cenario = cenario != null ? cenario : CenarioCarga.BAIXO;

        Map<CenarioCarga, Cenario> origem = (cenarios == null || cenarios.isEmpty()) ? padrao() : cenarios;
        Map<CenarioCarga, Cenario> copia = new EnumMap<>(CenarioCarga.class);
        copia.putAll(origem);
        cenarios = Collections.unmodifiableMap(copia);

        if (!cenarios.containsKey(cenario)) {
            throw new IllegalArgumentException(
                    "Cenário ativo '" + cenario + "' não tem parâmetros em hemorede.simulador.cenarios");
        }
    }

    /** Parâmetros do cenário escolhido em {@code hemorede.simulador.cenario}. */
    public Cenario ativo() {
        return cenarios.get(cenario);
    }

    private static Map<CenarioCarga, Cenario> padrao() {
        return Map.of(
                CenarioCarga.BAIXO, new Cenario(10, 5000),
                CenarioCarga.MODERADO, new Cenario(50, 1000),
                CenarioCarga.ALTO, new Cenario(100, 200));
    }
}