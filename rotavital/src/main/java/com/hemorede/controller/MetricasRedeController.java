package com.hemorede.controller;

import com.hemorede.dto.MetricasRedeResponse;
import com.hemorede.service.MetricasRedeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Métricas de rede da API para o painel (PI2-173 / PI2-175).
 * Borda HTTPS/443 (Render) → aplicação HTTP/8080.
 */
@RestController
@RequestMapping("/api/v1/metricas")
public class MetricasRedeController {

    private final MetricasRedeService metricasRedeService;

    public MetricasRedeController(MetricasRedeService metricasRedeService) {
        this.metricasRedeService = metricasRedeService;
    }

    @GetMapping("/rede")
    public MetricasRedeResponse rede() {
        return metricasRedeService.calcular();
    }
}
