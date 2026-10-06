package com.hemorede.dto;

import java.util.List;

/**
 * Resposta de {@code GET /api/estoques/bolsas}: as bolsas disponíveis para o
 * filtro informado (tipo sanguíneo + hemocomponente), já ordenadas pela
 * validade mais próxima (FEFO).
 * <p>
 * Cenário 2 da HU02 - Consultar estoque sem resultado: quando não há
 * bolsas para o filtro, {@code bolsas} vem vazia e {@code mensagem}
 * informa "nenhuma bolsa encontrada"; quando há resultado, {@code mensagem}
 * vem nula.
 * </p>
 *
 * @param bolsas   Bolsas disponíveis, ordenadas da validade mais próxima para a mais distante.
 * @param mensagem "nenhuma bolsa encontrada" quando {@code bolsas} está vazia; {@code null} caso contrário.
 */
public record ConsultaEstoqueResponse(List<BolsaDisponivelResponse> bolsas, String mensagem) {

    private static final String MENSAGEM_SEM_RESULTADO = "nenhuma bolsa encontrada";

    public static ConsultaEstoqueResponse de(List<BolsaDisponivelResponse> bolsas) {
        return new ConsultaEstoqueResponse(bolsas, bolsas.isEmpty() ? MENSAGEM_SEM_RESULTADO : null);
    }
}
