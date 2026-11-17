package com.hemorede.controller;

import com.hemorede.domain.model.Doador;
import com.hemorede.service.DoacaoService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HU01 - Lista os doadores sintéticos para o formulário de registro de doação.
 */
@RestController
@RequestMapping("/api/doadores")
public class DoadorController {

    private final DoacaoService doacaoService;

    public DoadorController(DoacaoService doacaoService) {
        this.doacaoService = doacaoService;
    }

    @GetMapping
    public List<Doador> listar() {
        return doacaoService.listarDoadores();
    }
}
