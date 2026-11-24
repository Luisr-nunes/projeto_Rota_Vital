package com.hemorede.domain.regras;

import com.hemorede.requisicao.domain.enums.StatusRequisicao;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static com.hemorede.requisicao.domain.enums.StatusRequisicao.*;
import static org.junit.jupiter.api.Assertions.*;

@DisplayName("RN-09 - Máquina de estados da requisição")
class RegraTransicaoRequisicaoTest {

    @ParameterizedTest(name = "{0} -> {1} permitida")
    @CsvSource({
            "PENDENTE,ALOCADA", "PENDENTE,APROVADA", "PENDENTE,CANCELADA",
            "ALOCADA,EM_ROTA", "APROVADA,EM_ROTA", "ALOCADA,ENTREGUE", "APROVADA,ENTREGUE",
            "ALOCADA,CANCELADA", "EM_ROTA,ENTREGUE"
    })
    void transicoesPermitidas(StatusRequisicao de, StatusRequisicao para) {
        assertTrue(RegraTransicaoRequisicao.permitida(de, para));
    }

    @ParameterizedTest(name = "{0} -> {1} recusada")
    @CsvSource({
            "ENTREGUE,APROVADA", "ENTREGUE,EM_ROTA", "CANCELADA,PENDENTE", "EM_ROTA,CANCELADA",
            "PENDENTE,EM_ROTA", "PENDENTE,ENTREGUE", "APROVADA,APROVADA", "EM_ROTA,PENDENTE"
    })
    void transicoesRecusadas(StatusRequisicao de, StatusRequisicao para) {
        assertFalse(RegraTransicaoRequisicao.permitida(de, para));
    }

    @Test
    void nuloNuncaEPermitido() {
        assertFalse(RegraTransicaoRequisicao.permitida(null, ENTREGUE));
        assertFalse(RegraTransicaoRequisicao.permitida(PENDENTE, null));
    }

    @Test
    void estadosFinais() {
        assertTrue(RegraTransicaoRequisicao.finalizada(ENTREGUE));
        assertTrue(RegraTransicaoRequisicao.finalizada(CANCELADA));
        assertFalse(RegraTransicaoRequisicao.finalizada(EM_ROTA));
        assertFalse(RegraTransicaoRequisicao.finalizada(null));
    }
}
