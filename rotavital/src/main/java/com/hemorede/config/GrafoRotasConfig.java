package com.hemorede.config;

import com.hemorede.algoritmos.DadosSinteticos;
import com.hemorede.algoritmos.GrafoRotas;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Expõe o grafo de rotas sintético (Hemocentro + 5 hospitais, conforme
 * {@code doc/escopo-grafo.md}) como um bean singleton, para que os
 * services da aplicação (ex: {@code RoteirizacaoService}) possam calcular
 * rotas mínimas via Dijkstra sem reconstruir o grafo a cada chamada.
 */
@Configuration
public class GrafoRotasConfig {

    @Bean
    public GrafoRotas grafoRotas() {
        return DadosSinteticos.criarGrafoRotaVital();
    }
}
