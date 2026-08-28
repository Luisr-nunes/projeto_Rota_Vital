package com.hemorede.controller;

import com.hemorede.domain.model.Hospital;
import com.hemorede.repository.HospitalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/hospitais")
@RequiredArgsConstructor
public class HospitalController {

    private final HospitalRepository hospitalRepository;

    @GetMapping
    public List<Hospital> listar() {
        return hospitalRepository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Hospital> buscarPorId(@PathVariable Long id) {
        return hospitalRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Hospital> criar(@RequestBody Hospital hospital) {
        Hospital salvo = hospitalRepository.save(hospital);
        return ResponseEntity.ok(salvo);
    }
}
