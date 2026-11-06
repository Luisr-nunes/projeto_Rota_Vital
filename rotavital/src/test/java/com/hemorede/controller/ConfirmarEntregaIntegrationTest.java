package com.hemorede.controller;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.Prioridade;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.StatusRequisicao;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.domain.model.Hospital;
import com.hemorede.domain.model.ItemRequisicao;
import com.hemorede.domain.model.Requisicao;
import com.hemorede.repository.BolsaRepository;
import com.hemorede.repository.HospitalRepository;
import com.hemorede.repository.RequisicaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * HU06 — Confirmar entrega (PI2-181): PATCH /api/v1/requisicoes/{id}/entrega.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:confirmar_entrega;DB_CLOSE_DELAY=-1",
        "hemorede.seed.enabled=false"
})
@AutoConfigureMockMvc
class ConfirmarEntregaIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private HospitalRepository hospitalRepository;
    @Autowired
    private BolsaRepository bolsaRepository;
    @Autowired
    private RequisicaoRepository requisicaoRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @BeforeEach
    void limparBanco() {
        new TransactionTemplate(transactionManager).executeWithoutResult(s -> {
            requisicaoRepository.deleteAll();
            requisicaoRepository.flush();
            bolsaRepository.deleteAll();
            hospitalRepository.deleteAll();
        });
    }

    @Test
    @DisplayName("Dada requisição ALOCADA, quando confirmar a entrega, então fica ENTREGUE e as bolsas UTILIZADA")
    void deveConfirmarEntregaDeRequisicaoAlocada() throws Exception {
        Bolsa bolsa = bolsaRepository.save(novaBolsa(StatusBolsa.RESERVADA));
        Requisicao requisicao = requisicaoRepository.save(
                novaRequisicao(StatusRequisicao.ALOCADA, List.of(bolsa)));

        mockMvc.perform(patch("/api/v1/requisicoes/{id}/entrega", requisicao.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(requisicao.getId()))
                .andExpect(jsonPath("$.status").value("ENTREGUE"))
                .andExpect(jsonPath("$.itens[0].bolsasAlocadas[0].status").value("UTILIZADA"));

        assertEquals(StatusRequisicao.ENTREGUE,
                requisicaoRepository.findById(requisicao.getId()).orElseThrow().getStatus());
        assertEquals(StatusBolsa.UTILIZADA,
                bolsaRepository.findById(bolsa.getId()).orElseThrow().getStatus());
    }

    private Requisicao novaRequisicao(StatusRequisicao status, List<Bolsa> bolsas) {
        Hospital hospital = new Hospital();
        hospital.setNome("Hospital Entrega");
        hospital.setCnpj("11222333000181");
        hospital.setCodigoNo("N5");
        hospital = hospitalRepository.save(hospital);

        Requisicao requisicao = new Requisicao();
        requisicao.setHospitalSolicitante(hospital);
        requisicao.setPrioridade(Prioridade.NORMAL);
        requisicao.setStatus(status);

        ItemRequisicao item = new ItemRequisicao();
        item.setRequisicao(requisicao);
        item.setHemoComponente(HemoComponente.HEMACIAS);
        item.setTipoSanguineo(TipoSanguineo.A_POS);
        item.setQuantidade(1);
        item.getBolsasAlocadas().addAll(bolsas);
        requisicao.getItens().add(item);
        return requisicao;
    }

    private Bolsa novaBolsa(StatusBolsa status) {
        return Bolsa.builder()
                .hemoComponente(HemoComponente.HEMACIAS)
                .tipoSanguineo(TipoSanguineo.A_POS)
                .dataColeta(LocalDate.now().minusDays(10))
                .dataValidade(LocalDate.now().plusDays(5))
                .status(status)
                .build();
    }
}
