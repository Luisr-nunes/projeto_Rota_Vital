package com.hemorede.domain.regras;

import com.hemorede.requisicao.domain.enums.StatusRequisicao;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * RN-09 - Máquina de estados da requisição.
 *
 * <pre>
 * PENDENTE --alocar/aprovar--> ALOCADA | APROVADA --despachar--> EM_ROTA --confirmar--> ENTREGUE
 *     \_____________________________cancelar (antes de sair para entrega)_______/
 * </pre>
 *
 * ENTREGUE e CANCELADA são finais. ALOCADA/APROVADA podem ir direto a ENTREGUE para manter o
 * contrato legado da HU06 (retirada no balcão, sem transporte).
 * Classe pura (sem Spring/JPA): pode ser reutilizada em qualquer camada.
 */
public final class RegraTransicaoRequisicao {

    private static final Map<StatusRequisicao, Set<StatusRequisicao>> PERMITIDAS = new EnumMap<>(StatusRequisicao.class);

    static {
        PERMITIDAS.put(StatusRequisicao.PENDENTE,
                EnumSet.of(StatusRequisicao.ALOCADA, StatusRequisicao.APROVADA, StatusRequisicao.CANCELADA));
        PERMITIDAS.put(StatusRequisicao.ALOCADA,
                EnumSet.of(StatusRequisicao.EM_ROTA, StatusRequisicao.ENTREGUE, StatusRequisicao.CANCELADA));
        PERMITIDAS.put(StatusRequisicao.APROVADA,
                EnumSet.of(StatusRequisicao.EM_ROTA, StatusRequisicao.ENTREGUE, StatusRequisicao.CANCELADA));
        PERMITIDAS.put(StatusRequisicao.EM_ROTA, EnumSet.of(StatusRequisicao.ENTREGUE));
        PERMITIDAS.put(StatusRequisicao.ENTREGUE, EnumSet.noneOf(StatusRequisicao.class));
        PERMITIDAS.put(StatusRequisicao.CANCELADA, EnumSet.noneOf(StatusRequisicao.class));
    }

    private RegraTransicaoRequisicao() {
    }

    /** {@code true} se a requisição pode sair de {@code de} para {@code para}. O(1). */
    public static boolean permitida(StatusRequisicao de, StatusRequisicao para) {
        if (de == null || para == null) {
            return false;
        }
        return PERMITIDAS.get(de).contains(para);
    }

    /** {@code true} se o status não admite nenhuma transição (ENTREGUE, CANCELADA). */
    public static boolean finalizada(StatusRequisicao status) {
        return status != null && PERMITIDAS.get(status).isEmpty();
    }
}
