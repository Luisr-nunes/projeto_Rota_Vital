package com.hemorede.service;

import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.repository.BolsaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Regra de negócio 3: bolsas vencidas nunca podem ser alocadas.
 * Job diário que varre o estoque e descarta automaticamente bolsas vencidas.
 */
@Service
@RequiredArgsConstructor
public class ValidadeService {

    private final BolsaRepository bolsaRepository;

    @Scheduled(cron = "0 0 3 * * *") // todo dia às 03h
    @Transactional
    public void expurgarBolsasVencidas() {
        List<Bolsa> vencidas = bolsaRepository
                .findByStatusAndDataValidadeBefore(StatusBolsa.DISPONIVEL, LocalDate.now());

        vencidas.forEach(bolsa -> bolsa.setStatus(StatusBolsa.DESCARTADA));
        bolsaRepository.saveAll(vencidas);
    }
}
