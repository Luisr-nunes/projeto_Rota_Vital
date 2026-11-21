package com.hemorede.controller;

import com.hemorede.domain.model.Requisicao;
import com.hemorede.dto.RequisicaoRequest;
import com.hemorede.dto.RastreamentoResponse;
import com.hemorede.service.RastreamentoService;
import com.hemorede.service.RequisicaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping({"/api/v1/requisicoes", "/api/requisicoes"})
public class RequisicaoController {

    private final RequisicaoService requisicaoService;
    private final RastreamentoService rastreamentoService;

    public RequisicaoController(RequisicaoService requisicaoService, RastreamentoService rastreamentoService) {
        this.requisicaoService = requisicaoService;
        this.rastreamentoService = rastreamentoService;
    }

    @GetMapping
    public List<Requisicao> listar() {
        return requisicaoService.listar();
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

    /**
     * HU06 - Confirma a entrega: requisição ALOCADA/APROVADA/EM_ROTA passa a
     * ENTREGUE e suas bolsas a UTILIZADA. 409 se a requisição não estiver
     * alocada; 404 se não existir.
     */
    /** Despacho: requisição alocada sai para entrega (EM_ROTA) com veículo compatível. */
    @PostMapping("/{id}/despacho")
    public RastreamentoResponse despachar(@PathVariable Long id) {
        return rastreamentoService.despachar(id);
    }

    /** Acompanhamento da entrega: trajeto, posição estimada, previsão e temperatura. */
    @GetMapping("/{id}/rastreamento")
    public RastreamentoResponse rastrear(@PathVariable Long id) {
        return rastreamentoService.rastrear(id);
    }

    @PatchMapping("/{id}/entrega")
    public ResponseEntity<Requisicao> confirmarEntrega(@PathVariable Long id) {
        return ResponseEntity.ok(requisicaoService.confirmarEntrega(id));
    }
}
