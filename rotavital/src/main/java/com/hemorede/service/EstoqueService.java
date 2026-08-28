package com.hemorede.service;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.repository.BolsaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Regra de negócio 7: estoque mínimo de segurança.
 * Verifica se a quantidade disponível de um tipo sanguíneo/componente
 * em um estoque está abaixo do limite mínimo configurado.
 */
@Service
@RequiredArgsConstructor
public class EstoqueService {

    private final BolsaRepository bolsaRepository;

    @Value("${hemorede.estoque.limite-minimo-padrao:5}")
    private int limiteMinimoPadrao;

    public boolean estoqueAbaixoDoMinimo(Long estoqueId, TipoSanguineo tipoSanguineo,
                                          HemoComponente hemoComponente) {
        long quantidadeDisponivel = bolsaRepository
                .countByEstoqueIdAndTipoSanguineoAndHemoComponenteAndStatus(
                        estoqueId, tipoSanguineo, hemoComponente, StatusBolsa.DISPONIVEL);

        return quantidadeDisponivel < limiteMinimoPadrao;
    }
}
