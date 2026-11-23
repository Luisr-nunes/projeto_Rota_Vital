package com.hemorede.controller;

import com.hemorede.domain.model.Hospital;
import com.hemorede.requisicao.domain.model.Requisicao;
import com.hemorede.dto.HospitalRequisicaoRequest;
import com.hemorede.expedicao.application.dto.RastreamentoResponse;
import com.hemorede.service.PortalHospitalService;
import com.hemorede.seguranca.UsuarioAtual;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** API do perfil HOSPITAL (/api/hospital/**, só HOSPITAL). */
@RestController
@RequestMapping("/api/hospital")
public class PortalHospitalController {

    private final PortalHospitalService service;
    private final UsuarioAtual usuarioAtual;

    public PortalHospitalController(PortalHospitalService service, UsuarioAtual usuarioAtual) {
        this.service = service;
        this.usuarioAtual = usuarioAtual;
    }

    @GetMapping("/me")
    public Hospital me() {
        return service.hospital(usuarioAtual.hospitalId());
    }

    @GetMapping("/requisicoes")
    public List<Requisicao> listar() {
        return service.listar(usuarioAtual.hospitalId());
    }

    @PostMapping("/requisicoes")
    public ResponseEntity<Requisicao> solicitar(@Valid @RequestBody HospitalRequisicaoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.solicitar(usuarioAtual.hospitalId(), request));
    }

    @GetMapping("/requisicoes/{id}")
    public Requisicao buscar(@PathVariable Long id) {
        return service.buscar(usuarioAtual.hospitalId(), id);
    }

    @GetMapping("/requisicoes/{id}/rastreamento")
    public RastreamentoResponse rastrear(@PathVariable Long id) {
        return service.rastrear(usuarioAtual.hospitalId(), id);
    }

    @PatchMapping("/requisicoes/{id}/recebimento")
    public Requisicao confirmarRecebimento(@PathVariable Long id) {
        return service.confirmarRecebimento(usuarioAtual.hospitalId(), id);
    }
}
