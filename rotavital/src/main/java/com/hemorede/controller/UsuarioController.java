package com.hemorede.controller;

import com.hemorede.dto.NovoUsuarioRequest;
import com.hemorede.dto.UsuarioResponse;
import com.hemorede.service.AutenticacaoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Gestão de contas pelo HEMOCENTRO (criar acesso de hospital ou de outro operador). */
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    private final AutenticacaoService autenticacaoService;

    public UsuarioController(AutenticacaoService autenticacaoService) {
        this.autenticacaoService = autenticacaoService;
    }

    @GetMapping
    public List<UsuarioResponse> listar() {
        return autenticacaoService.listarUsuarios();
    }

    @PostMapping
    public ResponseEntity<UsuarioResponse> criar(@Valid @RequestBody NovoUsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(autenticacaoService.criarUsuario(request));
    }
}
