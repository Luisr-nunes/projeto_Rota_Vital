package com.hemorede.controller;

import com.hemorede.dto.DadosDoadorRequest;
import com.hemorede.dto.DoadorPerfilResponse;
import com.hemorede.service.PortalDoadorService;
import com.hemorede.seguranca.UsuarioAtual;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

/** API do perfil DOADOR (/api/doador/**, só DOADOR). */
@RestController
@RequestMapping("/api/doador")
public class PortalDoadorController {

    private final PortalDoadorService service;
    private final UsuarioAtual usuarioAtual;

    public PortalDoadorController(PortalDoadorService service, UsuarioAtual usuarioAtual) {
        this.service = service;
        this.usuarioAtual = usuarioAtual;
    }

    @GetMapping("/me")
    public DoadorPerfilResponse perfil() {
        return service.perfil(usuarioAtual.doadorId());
    }

    @PutMapping("/me")
    public DoadorPerfilResponse atualizar(@Valid @RequestBody DadosDoadorRequest request) {
        return service.atualizar(usuarioAtual.doadorId(), request);
    }
}
