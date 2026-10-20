package com.hemorede.dto;

import java.time.LocalDate;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;

/**
 * Representação enxuta de uma bolsa disponível, usada na resposta de
 * {@code GET /api/estoques/bolsas} (HU02). Evita expor as associações JPA
 * de {@link Bolsa} (doador, estoque) que não interessam a esta consulta.
 *
 * @param id             Identificador da bolsa.
 * @param tipoSanguineo  Tipo sanguíneo da bolsa.
 * @param hemoComponente Hemocomponente da bolsa.
 * @param dataColeta     Data em que a bolsa foi coletada.
 * @param dataValidade   Data de validade da bolsa (critério de ordenação FEFO).
 */
public record BolsaDisponivelResponse(
        Long id,
        TipoSanguineo tipoSanguineo,
        HemoComponente hemoComponente,
        LocalDate dataColeta,
        LocalDate dataValidade) {

    public static BolsaDisponivelResponse fromEntidade(Bolsa bolsa) {
        return new BolsaDisponivelResponse(
                bolsa.getId(),
                bolsa.getTipoSanguineo(),
                bolsa.getHemoComponente(),
                bolsa.getDataColeta(),
                bolsa.getDataValidade());
    }
}
