package com.hemorede.service;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.domain.model.Doador;
import com.hemorede.exception.DoacaoForaDoIntervaloException;
import com.hemorede.exception.DoadorInativoException;
import com.hemorede.repository.BolsaRepository;
import com.hemorede.repository.DoadorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Regra de negócio 4: intervalo mínimo entre doações.
 * (Aqui simplificado por um único intervalo configurável; poderia
 * receber o sexo do doador para diferenciar 60/90 dias, por exemplo.)
 */
@Service
public class DoacaoService {

    private final DoadorRepository doadorRepository;
    private final BolsaRepository bolsaRepository;

    public DoacaoService(DoadorRepository doadorRepository, BolsaRepository bolsaRepository) {
        this.doadorRepository = doadorRepository;
        this.bolsaRepository = bolsaRepository;
    }

    @Value("${hemorede.doacao.intervalo-minimo-dias-homem:60}")
    private int intervaloMinimoDias;

    public void registrarDoacao(Long doadorId, LocalDate dataDoacao) {
        Doador doador = buscarDoador(doadorId);
        validarDoadorAtivo(doador);
        validarIntervalo(doador, dataDoacao);

        doador.setDataUltimaDoacao(dataDoacao);
        doadorRepository.save(doador);
    }

    /**
     * Registra a doação e a bolsa coletada na mesma transação.
     */
    @Transactional
    public Bolsa registrarDoacao(Long doadorId,
                                 TipoSanguineo tipoSanguineo,
                                 HemoComponente hemocomponente,
                                 LocalDate dataColeta,
                                 LocalDate validade) {
        Doador doador = buscarDoador(doadorId);
        validarDoadorAtivo(doador);
        validarIntervalo(doador, dataColeta);

        doador.setDataUltimaDoacao(dataColeta);
        doadorRepository.save(doador);

        Bolsa bolsa = Bolsa.builder()
                .tipoSanguineo(tipoSanguineo)
                .hemoComponente(hemocomponente)
                .dataColeta(dataColeta)
                .dataValidade(validade)
                .status(StatusBolsa.DISPONIVEL)
                .doador(doador)
                .build();

        return bolsaRepository.save(bolsa);
    }

    private Doador buscarDoador(Long doadorId) {
        return doadorRepository.findById(doadorId)
                .orElseThrow(() -> new IllegalArgumentException("Doador não encontrado: " + doadorId));
    }

    private void validarDoadorAtivo(Doador doador) {
        if (!doador.isAtivo()) {
            throw new DoadorInativoException(
                    "Doador " + doador.getId() + " está inativo e não pode realizar doações.");
        }
    }

    private void validarIntervalo(Doador doador, LocalDate dataDoacao) {
        if (doador.getDataUltimaDoacao() != null) {
            long diasDesdeUltima = java.time.temporal.ChronoUnit.DAYS
                    .between(doador.getDataUltimaDoacao(), dataDoacao);

            if (diasDesdeUltima < intervaloMinimoDias) {
                throw new DoacaoForaDoIntervaloException(
                        "Doador " + doador.getId() + " só pode doar novamente após "
                                + intervaloMinimoDias + " dias da última doação. Faltam "
                                + (intervaloMinimoDias - diasDesdeUltima) + " dia(s).");
            }
        }
    }
}
