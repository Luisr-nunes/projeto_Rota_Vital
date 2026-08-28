package com.hemorede.controller;

import com.hemorede.domain.model.Requisicao;
import com.hemorede.repository.RequisicaoRepository;
import com.hemorede.service.RequisicaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requisicoes")
@RequiredArgsConstructor
public class RequisicaoController {

    private final RequisicaoRepository requisicaoRepository;
    private final RequisicaoService requisicaoService;

    @GetMapping
    public List<Requisicao> listar() {
        return requisicaoRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<Requisicao> criar(@RequestBody Requisicao requisicao) {
        return ResponseEntity.ok(requisicaoRepository.save(requisicao));
    }

    @PostMapping("/{id}/aprovar")
    public ResponseEntity<Requisicao> aprovar(@PathVariable Long id) {
        return ResponseEntity.ok(requisicaoService.aprovar(id));
    }
}
