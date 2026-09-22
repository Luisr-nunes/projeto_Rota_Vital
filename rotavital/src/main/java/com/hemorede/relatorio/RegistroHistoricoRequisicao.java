package com.hemorede.relatorio;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.Prioridade;
import com.hemorede.domain.enums.StatusRequisicao;
import com.hemorede.domain.enums.TipoSanguineo;

/**
 * Um registro do histórico nacional de requisições de hemocomponentes,
 * usado pelo relatório de estatísticas ({@link RelatorioHistoricoService}).
 *
 * É um recorte simplificado de {@link com.hemorede.domain.model.Requisicao}
 * (mais o item alocado e o tempo de atendimento), o suficiente para as
 * agregações do relatório, sem depender do banco de dados: em escala
 * nacional, o histórico é gerado sinteticamente em memória
 * ({@link GeradorHistoricoRequisicoes}) para que o benchmark meça só o
 * custo do processamento, não o de uma consulta ao banco.
 *
 * @param id                      identificador sintético do registro
 * @param status                  status da requisição no momento da consulta
 * @param tipoSanguineo           tipo sanguíneo solicitado
 * @param hemoComponente          hemocomponente solicitado
 * @param prioridade              prioridade da requisição
 * @param tempoAtendimentoMinutos minutos entre a solicitação e a entrega;
 *                                {@code null} quando a requisição ainda não
 *                                foi atendida (ex.: PENDENTE ou CANCELADA)
 */
public record RegistroHistoricoRequisicao(
        long id,
        StatusRequisicao status,
        TipoSanguineo tipoSanguineo,
        HemoComponente hemoComponente,
        Prioridade prioridade,
        Long tempoAtendimentoMinutos
) {
}
