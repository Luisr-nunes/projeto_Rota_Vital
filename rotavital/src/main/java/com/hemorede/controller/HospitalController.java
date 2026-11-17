package com.hemorede.controller;

import com.hemorede.domain.model.Hospital;
import com.hemorede.service.HospitalService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hospitais")
public class HospitalController {

    private final HospitalService hospitalService;

    public HospitalController(HospitalService hospitalService) {
        this.hospitalService = hospitalService;
    }

    @GetMapping
    public List<Hospital> listar() {
        return hospitalService.listar();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Hospital> buscarPorId(@PathVariable Long id) {
        return hospitalService.buscarPorId(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Hospital> criar(@RequestBody Hospital hospital) {
        Hospital salvo = hospitalService.criar(hospital);
        return ResponseEntity.ok(salvo);
    }
}
