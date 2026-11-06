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
@RequestMapping({"/api/v1/requisicoes", "/api/requisicoes"})
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

    /**
     * HU06 - Confirma a entrega: requisição ALOCADA/APROVADA/EM_ROTA passa a
     * ENTREGUE e suas bolsas a UTILIZADA. 409 se a requisição não estiver
     * alocada; 404 se não existir.
     */
    @PatchMapping("/{id}/entrega")
    public ResponseEntity<Requisicao> confirmarEntrega(@PathVariable Long id) {
        return ResponseEntity.ok(requisicaoService.confirmarEntrega(id));
    }
}
