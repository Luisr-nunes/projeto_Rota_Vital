package com.hemorede.controller;

import com.hemorede.dto.AlocacaoResponse;
import com.hemorede.service.AlocacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requisicoes")
public class AlocacaoController {

    private final AlocacaoService alocacaoService;

    public AlocacaoController(AlocacaoService alocacaoService) {
        this.alocacaoService = alocacaoService;
    }

    @PostMapping("/{id}/alocacao")
    public ResponseEntity<AlocacaoResponse> alocar(@PathVariable Long id) {
        return ResponseEntity.ok(alocacaoService.alocarRequisicao(id));
    }
}
