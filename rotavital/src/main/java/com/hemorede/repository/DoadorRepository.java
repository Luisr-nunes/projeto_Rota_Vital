package com.hemorede.repository;

import com.hemorede.domain.model.Doador;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DoadorRepository extends JpaRepository<Doador, Long> {
    Doador findByCpf(String cpf);
}
