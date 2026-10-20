package com.hemorede.controller;

import com.hemorede.dto.LeituraTemperaturaResponse;
import com.hemorede.service.TelemetriaService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint de consulta da telemetria de temperatura (PI2-121), contrato em
 * {@code doc/telemetria.md} (seção 5): 200 com a leitura, 404 para veículo
 * inexistente e 400 quando {@code veiculoId} não é informado.
 * Borda HTTPS/443 (Render) → aplicação HTTP/8080.
 */
@RestController
@RequestMapping("/api/telemetria")
public class TelemetriaController {

    private final TelemetriaService telemetriaService;

    public TelemetriaController(TelemetriaService telemetriaService) {
        this.telemetriaService = telemetriaService;
    }

    @GetMapping("/temperatura")
    public LeituraTemperaturaResponse temperatura(@RequestParam Long veiculoId) {
        return telemetriaService.lerTemperatura(veiculoId);
    }
}
