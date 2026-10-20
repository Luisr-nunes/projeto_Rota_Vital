package com.hemorede.controller;

import com.hemorede.domain.model.Requisicao;
import com.hemorede.dto.RequisicaoRequest;
import com.hemorede.repository.RequisicaoRepository;
import com.hemorede.service.RequisicaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/requisicoes")
public class RequisicaoController {

    private final RequisicaoRepository requisicaoRepository;
    private final RequisicaoService requisicaoService;

    public RequisicaoController(RequisicaoRepository requisicaoRepository, RequisicaoService requisicaoService) {
        this.requisicaoRepository = requisicaoRepository;
        this.requisicaoService = requisicaoService;
    }

    @GetMapping
    public List<Requisicao> listar() {
        return requisicaoRepository.findAll();
    }

    /**
     * HU03 - Registra uma requisição hospitalar. Responde 201 Created com o
     * cabeçalho Location; 400 para corpo inválido (Bean Validation) e 404
     * para hospital inexistente.
     */
    @PostMapping
    public ResponseEntity<Requisicao> criar(@Valid @RequestBody RequisicaoRequest request) {
        Requisicao criada = requisicaoService.registrar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(criada.getId())
                .toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @PostMapping("/{id}/aprovar")
    public ResponseEntity<Requisicao> aprovar(@PathVariable Long id) {
        return ResponseEntity.ok(requisicaoService.aprovar(id));
    }
}
