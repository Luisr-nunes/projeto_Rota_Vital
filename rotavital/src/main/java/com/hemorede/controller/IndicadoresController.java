package com.hemorede.controller;

import com.hemorede.dto.IndicadoresResponse;
import com.hemorede.dto.RiscoDesabastecimentoResponse;
import com.hemorede.service.IndicadoresService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping({"/api/indicadores", "/api/v1/indicadores"})
public class IndicadoresController {

    private final IndicadoresService indicadoresService;

    public IndicadoresController(IndicadoresService indicadoresService) {
        this.indicadoresService = indicadoresService;
    }

    @GetMapping
    public IndicadoresResponse consultar(
            @RequestParam(defaultValue = "5") int diasProximoVencimento) {
        return indicadoresService.calcular(diasProximoVencimento);
    }

    @GetMapping("/risco-desabastecimento")
    public RiscoDesabastecimentoResponse consultarRiscoDesabastecimento(
            @RequestParam(defaultValue = "3") int dias) {
        return indicadoresService.calcularRiscoDesabastecimento(dias);
    }
}
