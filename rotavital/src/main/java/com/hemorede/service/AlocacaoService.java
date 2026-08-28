package com.hemorede.service;

import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.domain.model.ItemRequisicao;
import com.hemorede.exception.EstoqueInsuficienteException;
import com.hemorede.exception.IncompatibilidadeSanguineaException;
import com.hemorede.repository.BolsaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Responsável pela alocação de bolsas a itens de requisição.
 *
 * Regra de negócio 1 (compatibilidade sanguínea) e
 * Regra de negócio 2 (FEFO - First Expired, First Out) são aplicadas aqui.
 */
@Service
@RequiredArgsConstructor
public class AlocacaoService {

    private final BolsaRepository bolsaRepository;

    @Transactional
    public void alocar(ItemRequisicao item) {
        // Busca bolsas do mesmo hemocomponente/tipo sanguíneo, disponíveis,
        // já ordenadas por validade ascendente (FEFO) diretamente na query.
        List<Bolsa> candidatas = bolsaRepository
                .findByHemoComponenteAndTipoSanguineoAndStatusOrderByDataValidadeAsc(
                        item.getHemoComponente(), item.getTipoSanguineo(), StatusBolsa.DISPONIVEL);

        int necessario = item.getQuantidade() - item.getBolsasAlocadas().size();
        int alocadas = 0;

        for (Bolsa bolsa : candidatas) {
            if (alocadas >= necessario) {
                break;
            }

            // Regra 1: garante compatibilidade (redundante com a query, mas explícita
            // caso a lista venha de outra origem, ex: bolsas de tipo compatível O-).
            if (!com.hemorede.domain.enums.TipoSanguineo
                    .compativel(bolsa.getTipoSanguineo(), item.getTipoSanguineo())) {
                throw new IncompatibilidadeSanguineaException(
                        "Bolsa " + bolsa.getId() + " incompatível com item de requisição " + item.getId());
            }

            // Regra 3: nunca alocar bolsa vencida.
            if (bolsa.isVencida()) {
                bolsa.setStatus(StatusBolsa.DESCARTADA);
                bolsaRepository.save(bolsa);
                continue;
            }

            bolsa.setStatus(StatusBolsa.RESERVADA);
            bolsaRepository.save(bolsa);
            item.getBolsasAlocadas().add(bolsa);
            alocadas++;
        }

        if (alocadas < necessario) {
            throw new EstoqueInsuficienteException(
                    "Estoque insuficiente para o item de requisição " + item.getId()
                            + ". Necessário: " + necessario + ", alocado: " + alocadas);
        }
    }
}
