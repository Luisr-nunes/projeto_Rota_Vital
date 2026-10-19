package com.hemorede.controller;

import com.hemorede.domain.model.Doador;
import com.hemorede.repository.DoadorRepository;
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

    private final DoadorRepository doadorRepository;

    public DoadorController(DoadorRepository doadorRepository) {
        this.doadorRepository = doadorRepository;
    }

    @GetMapping
    public List<Doador> listar() {
        return doadorRepository.findAll();
    }
}
