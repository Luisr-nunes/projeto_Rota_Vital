package com.hemorede.indicadores;

import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.exception.ModeloProbabilisticoIndisponivelException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Ponto de integracao com a PI2-147.
 *
 * Os lambdas diarios definidos no relatorio devem ser configurados em
 * hemorede.indicadores.risco.lambda-diario. Sem os oito valores, o endpoint
 * informa que o modelo ainda nao esta disponivel em vez de inventar dados.
 */
@Component
@ConfigurationProperties(prefix = "hemorede.indicadores.risco")
public class ModeloProbabilisticoDemanda {

    private Map<TipoSanguineo, Double> lambdaDiario = new EnumMap<>(TipoSanguineo.class);

    public Map<TipoSanguineo, Double> obterLambdasValidados() {
        EnumMap<TipoSanguineo, Double> copia = new EnumMap<>(TipoSanguineo.class);

        for (TipoSanguineo tipo : TipoSanguineo.values()) {
            Double lambda = lambdaDiario.get(tipo);
            if (lambda == null || !Double.isFinite(lambda) || lambda < 0.0) {
                throw new ModeloProbabilisticoIndisponivelException(
                        "Modelo de demanda pendente: lambda diario ausente ou invalido para " + tipo);
            }
            copia.put(tipo, lambda);
        }

        return Collections.unmodifiableMap(copia);
    }

    public Map<TipoSanguineo, Double> getLambdaDiario() {
        return lambdaDiario;
    }

    public void setLambdaDiario(Map<TipoSanguineo, Double> lambdaDiario) {
        this.lambdaDiario = new EnumMap<>(TipoSanguineo.class);
        if (lambdaDiario != null) {
            this.lambdaDiario.putAll(lambdaDiario);
        }
    }
}
