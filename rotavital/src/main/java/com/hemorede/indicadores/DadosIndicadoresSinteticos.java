package com.hemorede.indicadores;

import com.hemorede.domain.enums.StatusRequisicao;

import java.util.List;

/**
 * Amostra sintética exclusiva da sprint de Estatística.
 *
 * Mantê-la separada evita modificar as entidades e regras de negócio já
 * desenvolvidas por outros integrantes da equipe.
 */
public final class DadosIndicadoresSinteticos {

    private DadosIndicadoresSinteticos() {
    }

    public static List<AtendimentoSintetico> criarAtendimentosExemplo() {
        return List.of(
                new AtendimentoSintetico(StatusRequisicao.APROVADA, 120L),
                new AtendimentoSintetico(StatusRequisicao.ENTREGUE, 45L),
                new AtendimentoSintetico(StatusRequisicao.APROVADA, 30L),
                new AtendimentoSintetico(StatusRequisicao.PENDENTE, null),
                new AtendimentoSintetico(StatusRequisicao.CANCELADA, null)
        );
    }

    public record AtendimentoSintetico(
            StatusRequisicao status,
            Long tempoAtendimentoEmMinutos
    ) {
    }
}
