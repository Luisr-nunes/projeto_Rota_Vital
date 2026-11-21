package com.hemorede.controller;

import com.hemorede.domain.model.Hospital;
import com.hemorede.dto.HospitalRequest;
import com.hemorede.service.HospitalService;
import jakarta.validation.Valid;
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
    public ResponseEntity<Hospital> criar(@Valid @RequestBody HospitalRequest req) {
        Hospital hospital = new Hospital();
        hospital.setNome(req.nome().trim());
        hospital.setCnpj(req.cnpj().replaceAll("[^0-9]", ""));
        hospital.setEndereco(req.endereco());
        hospital.setLatitude(req.latitude());
        hospital.setLongitude(req.longitude());
        hospital.setCodigoNo(req.codigoNo());
        Hospital salvo = hospitalService.criar(hospital);
        return ResponseEntity.ok(salvo);
    }
}
