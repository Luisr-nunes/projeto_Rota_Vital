package com.hemorede.controller;

import com.hemorede.domain.model.Bolsa;
import com.hemorede.dto.DoacaoRequest;
import com.hemorede.service.DoacaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/doacoes")
public class DoacaoController {

    private final DoacaoService doacaoService;

    public DoacaoController(DoacaoService doacaoService) {
        this.doacaoService = doacaoService;
    }

    @PostMapping
    public ResponseEntity<Bolsa> registrar(@Valid @RequestBody DoacaoRequest request) {
        Bolsa bolsa = doacaoService.registrarDoacao(
                request.doadorId(),
                request.tipoSanguineo(),
                request.hemocomponente(),
                request.dataColeta(),
                request.validade()
        );

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(bolsa.getId())
                .toUri();

        return ResponseEntity.created(location).body(bolsa);
    }
}
