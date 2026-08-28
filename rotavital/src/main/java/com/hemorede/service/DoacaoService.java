package com.hemorede.service;

import com.hemorede.domain.model.Doador;
import com.hemorede.exception.DoacaoForaDoIntervaloException;
import com.hemorede.repository.DoadorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

/**
 * Regra de negócio 4: intervalo mínimo entre doações.
 * (Aqui simplificado por um único intervalo configurável; poderia
 * receber o sexo do doador para diferenciar 60/90 dias, por exemplo.)
 */
@Service
@RequiredArgsConstructor
public class DoacaoService {

    private final DoadorRepository doadorRepository;

    @Value("${hemorede.doacao.intervalo-minimo-dias-homem:60}")
    private int intervaloMinimoDias;

    public void registrarDoacao(Long doadorId, LocalDate dataDoacao) {
        Doador doador = doadorRepository.findById(doadorId)
                .orElseThrow(() -> new IllegalArgumentException("Doador não encontrado: " + doadorId));

        if (doador.getDataUltimaDoacao() != null) {
            long diasDesdeUltima = java.time.temporal.ChronoUnit.DAYS
                    .between(doador.getDataUltimaDoacao(), dataDoacao);

            if (diasDesdeUltima < intervaloMinimoDias) {
                throw new DoacaoForaDoIntervaloException(
                        "Doador " + doadorId + " só pode doar novamente após "
                                + intervaloMinimoDias + " dias da última doação. Faltam "
                                + (intervaloMinimoDias - diasDesdeUltima) + " dia(s).");
            }
        }

        doador.setDataUltimaDoacao(dataDoacao);
        doadorRepository.save(doador);
    }
}
