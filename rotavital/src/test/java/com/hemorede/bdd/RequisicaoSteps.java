package com.hemorede.bdd;

import com.hemorede.domain.enums.*;
import com.hemorede.domain.model.*;
import com.hemorede.repository.*;
import io.cucumber.java.Before;
import io.cucumber.java.pt.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** MockMvc executa controllers e servicos reais; apenas o banco e sintetico (H2). */
public class RequisicaoSteps {
    @Autowired private MockMvc mvc;
    @Autowired private HospitalRepository hospitais;
    @Autowired private RequisicaoRepository requisicoes;
    @Autowired private BolsaRepository bolsas;
    @Autowired private PlatformTransactionManager transactionManager;

    private final Map<String, Long> bolsasPorNome = new HashMap<>();
    private Long requisicaoId;
    private Long hospitalId;
    private ResultActions resposta;

    private TransactionTemplate transacao() {
        return new TransactionTemplate(transactionManager);
    }

    @Before
    public void prepararBancoIsolado() {
        transacao().executeWithoutResult(status -> {
            requisicoes.deleteAll();
            requisicoes.flush();
            bolsas.deleteAll();
            hospitais.deleteAll();
        });
        bolsasPorNome.clear();
        resposta = null;
    }

    @Dado("uma requisicao pendente de {int} bolsa(s) para receptor {string}")
    public void criarRequisicao(int quantidade, String receptor) {
        transacao().executeWithoutResult(status -> {
            Hospital hospital = new Hospital();
            hospital.setNome("Hospital BDD sintetico");
            hospital.setCnpj("00000000000191");
            hospital.setCodigoNo("N5");
            hospitalId = hospitais.save(hospital).getId();
            Requisicao requisicao = new Requisicao();
            requisicao.setHospitalSolicitante(hospital);
            requisicao.setPrioridade(Prioridade.NORMAL);
            ItemRequisicao item = new ItemRequisicao();
            item.setRequisicao(requisicao);
            item.setHemoComponente(HemoComponente.HEMACIAS);
            item.setTipoSanguineo(TipoSanguineo.valueOf(receptor));
            item.setQuantidade(quantidade);
            requisicao.getItens().add(item);
            requisicaoId = requisicoes.save(requisicao).getId();
        });
    }

    @Dado("a requisicao tambem solicita {int} bolsa para receptor {string}")
    public void adicionarItem(int quantidade, String receptor) {
        transacao().executeWithoutResult(status -> {
            Requisicao requisicao = requisicoes.findById(requisicaoId).orElseThrow();
            ItemRequisicao item = new ItemRequisicao();
            item.setRequisicao(requisicao);
            item.setHemoComponente(HemoComponente.HEMACIAS);
            item.setTipoSanguineo(TipoSanguineo.valueOf(receptor));
            item.setQuantidade(quantidade);
            requisicao.getItens().add(item);
            requisicoes.save(requisicao);
        });
    }

    @Dado("uma bolsa {string} do tipo {string} com validade em {int} dias")
    public void criarBolsa(String nome, String tipo, int dias) {
        Bolsa bolsa = Bolsa.builder()
                .hemoComponente(HemoComponente.HEMACIAS)
                .tipoSanguineo(TipoSanguineo.valueOf(tipo))
                .dataColeta(LocalDate.now().minusDays(10))
                .dataValidade(LocalDate.now().plusDays(dias))
                .status(StatusBolsa.DISPONIVEL).build();
        bolsasPorNome.put(nome, bolsas.save(bolsa).getId());
    }

    @Dado("o hospital nao possui no no grafo")
    public void removerNo() {
        transacao().executeWithoutResult(status -> {
            Hospital hospital = hospitais.findById(hospitalId).orElseThrow();
            hospital.setCodigoNo(null);
            hospitais.save(hospital);
        });
    }

    @Quando("solicito a alocacao da requisicao")
    public void alocar() throws Exception {
        resposta = mvc.perform(post("/api/v1/requisicoes/{id}/alocacao", requisicaoId));
    }

    @Entao("a API responde {int}")
    public void conferirHttp(int codigo) throws Exception {
        resposta.andExpect(status().is(codigo));
    }

    @Entao("a requisicao fica {string}")
    public void conferirRequisicao(String esperado) {
        assertEquals(esperado, requisicoes.findById(requisicaoId).orElseThrow().getStatus().name());
    }

    @Entao("a bolsa {string} fica {string}")
    public void conferirBolsa(String nome, String esperado) {
        assertEquals(esperado, bolsas.findById(bolsasPorNome.get(nome)).orElseThrow().getStatus().name());
    }

    @Entao("a requisicao possui a bolsa {string} associada")
    public void conferirAssociacao(String nome) {
        transacao().executeWithoutResult(status -> {
            Requisicao requisicao = requisicoes.findById(requisicaoId).orElseThrow();
            assertTrue(requisicao.getItens().stream().flatMap(i -> i.getBolsasAlocadas().stream())
                    .anyMatch(b -> b.getId().equals(bolsasPorNome.get(nome))));
        });
    }

    @Entao("a requisicao nao possui bolsas associadas")
    public void conferirSemAssociacoes() {
        transacao().executeWithoutResult(status -> assertTrue(
                requisicoes.findById(requisicaoId).orElseThrow().getItens().stream()
                        .allMatch(i -> i.getBolsasAlocadas().isEmpty())));
    }

    @Entao("a resposta inclui rota e custo")
    public void conferirRota() throws Exception {
        resposta.andExpect(jsonPath("$.rota[0]").value("N0"))
                .andExpect(jsonPath("$.rota[1]").value("N5"))
                .andExpect(jsonPath("$.custoTotal").value(10.0));
    }

    @Entao("a resposta informa {int} bolsas necessarias e {int} disponivel")
    public void conferirQuantidades(int necessarias, int disponiveis) throws Exception {
        resposta.andExpect(jsonPath("$.quantidadeNecessaria").value(necessarias))
                .andExpect(jsonPath("$.quantidadeDisponivel").value(disponiveis));
    }

    @Dado("uma requisicao alocada com uma bolsa reservada")
    public void prepararEntrega() {
        criarRequisicao(1, "A_POS");
        criarBolsa("entrega", "A_POS", 5);
        // Fixture do contrato HU06: nao substitui o teste de alocacao da HU08.
        transacao().executeWithoutResult(status -> {
            Requisicao requisicao = requisicoes.findById(requisicaoId).orElseThrow();
            Bolsa bolsa = bolsas.findById(bolsasPorNome.get("entrega")).orElseThrow();
            bolsa.setStatus(StatusBolsa.RESERVADA);
            requisicao.getItens().get(0).getBolsasAlocadas().add(bolsa);
            requisicao.setStatus(StatusRequisicao.ALOCADA);
            bolsas.save(bolsa);
            requisicoes.save(requisicao);
        });
    }

    @Quando("confirmo a entrega da requisicao")
    public void confirmarEntrega() throws Exception {
        resposta = mvc.perform(patch("/api/v1/requisicoes/{id}/entrega", requisicaoId));
    }
}
