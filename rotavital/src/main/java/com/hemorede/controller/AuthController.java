package com.hemorede.controller;

import com.hemorede.dto.CadastroDoadorRequest;
import com.hemorede.dto.LoginRequest;
import com.hemorede.dto.SessaoResponse;
import com.hemorede.service.AutenticacaoService;
import com.hemorede.seguranca.UsuarioAtual;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Login (público), autocadastro de doador (público) e sessão atual (autenticado). */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AutenticacaoService autenticacaoService;
    private final UsuarioAtual usuarioAtual;

    public AuthController(AutenticacaoService autenticacaoService, UsuarioAtual usuarioAtual) {
        this.autenticacaoService = autenticacaoService;
        this.usuarioAtual = usuarioAtual;
    }

    @PostMapping("/login")
    public SessaoResponse login(@Valid @RequestBody LoginRequest request) {
        return autenticacaoService.login(request.email(), request.senha());
    }

    @PostMapping("/cadastro-doador")
    public ResponseEntity<SessaoResponse> cadastrarDoador(@Valid @RequestBody CadastroDoadorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(autenticacaoService.cadastrarDoador(request));
    }

    @GetMapping("/me")
    public SessaoResponse me() {
        return autenticacaoService.sessaoAtual(usuarioAtual.email());
    }
}
