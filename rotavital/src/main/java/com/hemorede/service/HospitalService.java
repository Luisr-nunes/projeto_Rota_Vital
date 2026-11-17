package com.hemorede.service;

import com.hemorede.domain.model.Hospital;
import com.hemorede.repository.HospitalRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class HospitalService {

    private final HospitalRepository hospitalRepository;

    public HospitalService(HospitalRepository hospitalRepository) {
        this.hospitalRepository = hospitalRepository;
    }

    public List<Hospital> listar() {
        return hospitalRepository.findAll();
    }

    public Optional<Hospital> buscarPorId(Long id) {
        return hospitalRepository.findById(id);
    }

    public Hospital criar(Hospital hospital) {
        return hospitalRepository.save(hospital);
    }
}
