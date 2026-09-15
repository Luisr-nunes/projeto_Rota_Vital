package com.hemorede.controller;

import com.hemorede.dto.IndicadoresResponse;
import com.hemorede.service.IndicadoresService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/indicadores")
@RequiredArgsConstructor
public class IndicadoresController {

    private final IndicadoresService indicadoresService;

    @GetMapping
    public IndicadoresResponse consultar(
            @RequestParam(defaultValue = "5") int diasProximoVencimento) {
        return indicadoresService.calcular(diasProximoVencimento);
    }
}
