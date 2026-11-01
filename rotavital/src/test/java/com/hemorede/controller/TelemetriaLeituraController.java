package com.hemorede.controller;

import com.hemorede.dto.TelemetriaLeituraDTO;
import com.hemorede.service.TelemetriaIngestaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Ingestão de leituras de telemetria (PI2-168): valida o corpo, enfileira e
 * responde 202; o processamento é assíncrono ({@code ConsumidorTelemetria}).
 */
@RestController
@RequestMapping("/api/v1/telemetria")
public class TelemetriaLeituraController {

    private final TelemetriaIngestaoService ingestao;

    public TelemetriaLeituraController(TelemetriaIngestaoService ingestao) {
        this.ingestao = ingestao;
    }

    @PostMapping("/leituras")
    public ResponseEntity<Void> receberLeitura(@Valid @RequestBody TelemetriaLeituraDTO leitura) {
        ingestao.receber(leitura);
        return ResponseEntity.accepted().build();
    }
}