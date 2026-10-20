package com.hemorede.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.Prioridade;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.StatusRequisicao;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.domain.model.Hospital;
import com.hemorede.domain.model.ItemRequisicao;
import com.hemorede.domain.model.Requisicao;
import com.hemorede.dto.ItemRequisicaoRequest;
import com.hemorede.dto.RequisicaoRequest;
import com.hemorede.exception.EstoqueInsuficienteException;
import com.hemorede.repository.BolsaRepository;
import com.hemorede.repository.HospitalRepository;
import com.hemorede.repository.RequisicaoRepository;
import com.hemorede.service.RequisicaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * PI2-191 — fluxo de requisição e alocação (HU03 e HU08) contra um PostgreSQL real.
 *
 * <p>Os valores gravados são conferidos com SQL puro ({@code JdbcTemplate}), e não pelos repositórios,
 * para provar o que de fato ficou no banco: mapeamento dos enums como texto, tabela de junção
 * {@code item_requisicao_bolsa} e rollback de transação.</p>
 */
class RequisicaoFluxoPostgresIT extends PostgresIntegrationTestBase {

    @Autowired
    private HospitalRepository hospitalRepository;
    @Autowired
    private BolsaRepository bolsaRepository;
    @Autowired
    private RequisicaoRepository requisicaoRepository;
    @Autowired
    private RequisicaoService requisicaoService;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("Os testes rodam em PostgreSQL de verdade e o Hibernate criou o schema nele")
    void deveUsarPostgreSqlEComSchemaCriado() throws Exception {
        try (Connection conexao = dataSource.getConnection()) {
            assertEquals("PostgreSQL", conexao.getMetaData().getDatabaseProductName());
        }
        String versao = jdbc.queryForObject("SELECT version()", String.class);
        assertTrue(versao != null && versao.startsWith("PostgreSQL"), "Versão inesperada: " + versao);

        List<String> tabelas = jdbc.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public'", String.class);
        for (String esperada : List.of("hospital", "bolsa", "requisicao", "item_requisicao",
                "item_requisicao_bolsa")) {
            assertTrue(tabelas.contains(esperada), "Tabela ausente no PostgreSQL: " + esperada);
        }
    }

    @Test
    @DisplayName("Aprova a requisição reservando a bolsa compatível de validade mais próxima (FEFO)")
    void deveAprovarComFefoEGravarNoBanco() {
        Hospital hospital = hospitalRepository.save(novoHospital("22333444000155"));
        Bolsa venceCedo = bolsaRepository.save(novaBolsa(TipoSanguineo.O_POS, LocalDate.now().plusDays(3)));
        Bolsa venceDepois = bolsaRepository.save(novaBolsa(TipoSanguineo.O_POS, LocalDate.now().plusDays(20)));
        Requisicao salva = salvarRequisicao(hospital, novoItem(TipoSanguineo.O_POS, 1));

        requisicaoService.aprovar(salva.getId());

        assertEquals(StatusRequisicao.APROVADA.name(), statusDaRequisicao(salva.getId()));
        assertEquals(StatusBolsa.RESERVADA.name(), statusDaBolsa(venceCedo.getId()),
                "Deveria reservar a bolsa que vence primeiro (FEFO)");
        assertEquals(StatusBolsa.DISPONIVEL.name(), statusDaBolsa(venceDepois.getId()),
                "A bolsa de validade mais longa não deveria ser tocada");
        assertEquals(1, contar("item_requisicao_bolsa"), "A alocação deve ficar na tabela de junção");
    }

    @Test
    @DisplayName("Não aloca bolsa de tipo sanguíneo incompatível (ABO/Rh) e mantém a requisição PENDENTE")
    void naoDeveAlocarBolsaIncompativel() {
        Hospital hospital = hospitalRepository.save(novoHospital("33444555000166"));
        // Só existe AB- em estoque; um receptor O- não pode recebê-la.
        Bolsa incompativel = bolsaRepository.save(novaBolsa(TipoSanguineo.AB_NEG, LocalDate.now().plusDays(10)));
        Requisicao salva = salvarRequisicao(hospital, novoItem(TipoSanguineo.O_NEG, 1));

        assertThrows(EstoqueInsuficienteException.class, () -> requisicaoService.aprovar(salva.getId()));

        assertEquals(StatusRequisicao.PENDENTE.name(), statusDaRequisicao(salva.getId()));
        assertEquals(StatusBolsa.DISPONIVEL.name(), statusDaBolsa(incompativel.getId()));
    }

    @Test
    @DisplayName("Nunca aloca bolsa vencida, mesmo sendo a única do tipo em estoque")
    void naoDeveAlocarBolsaVencida() {
        Hospital hospital = hospitalRepository.save(novoHospital("44555666000177"));
        Bolsa vencida = bolsaRepository.save(novaBolsa(TipoSanguineo.A_POS, LocalDate.now().minusDays(1)));
        Requisicao salva = salvarRequisicao(hospital, novoItem(TipoSanguineo.A_POS, 1));

        assertThrows(EstoqueInsuficienteException.class, () -> requisicaoService.aprovar(salva.getId()));

        assertEquals(StatusRequisicao.PENDENTE.name(), statusDaRequisicao(salva.getId()));
        assertEquals(StatusBolsa.DISPONIVEL.name(), statusDaBolsa(vencida.getId()));
    }

    @Test
    @DisplayName("Se um item não pode ser atendido, o PostgreSQL desfaz as reservas dos itens anteriores")
    void deveFazerRollbackQuandoUmItemFalha() {
        Hospital hospital = hospitalRepository.save(novoHospital("55666777000188"));
        Bolsa primeira = bolsaRepository.save(novaBolsa(TipoSanguineo.O_POS, LocalDate.now().plusDays(5)));
        Bolsa segunda = bolsaRepository.save(novaBolsa(TipoSanguineo.O_POS, LocalDate.now().plusDays(9)));
        // Demanda total (1 + 2 = 3) maior que o estoque (2): qualquer que seja a ordem em que os itens
        // são processados, um deles chega a reservar bolsas antes de o outro falhar.
        Requisicao salva = salvarRequisicao(hospital,
                novoItem(TipoSanguineo.O_POS, 1),
                novoItem(TipoSanguineo.O_POS, 2));

        assertThrows(EstoqueInsuficienteException.class, () -> requisicaoService.aprovar(salva.getId()));

        assertEquals(StatusBolsa.DISPONIVEL.name(), statusDaBolsa(primeira.getId()));
        assertEquals(StatusBolsa.DISPONIVEL.name(), statusDaBolsa(segunda.getId()));
        assertEquals(StatusRequisicao.PENDENTE.name(), statusDaRequisicao(salva.getId()));
        assertEquals(0, contar("item_requisicao_bolsa"), "Nenhuma alocação pode ter sobrado no banco");
    }

    @Test
    @DisplayName("Pela API: registra (201, PENDENTE) e aprova (200, APROVADA) gravando a reserva no banco")
    void deveRegistrarEAprovarPelaApi() throws Exception {
        Hospital hospital = hospitalRepository.save(novoHospital("77888999000101"));
        Bolsa bolsa = bolsaRepository.save(novaBolsa(TipoSanguineo.O_NEG, LocalDate.now().plusDays(10)));
        RequisicaoRequest pedido = new RequisicaoRequest(hospital.getId(), Prioridade.URGENTE,
                List.of(new ItemRequisicaoRequest(HemoComponente.HEMACIAS, TipoSanguineo.O_NEG, 1)));

        MvcResult criada = mockMvc.perform(post("/api/requisicoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pedido)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andReturn();
        long id = objectMapper.readTree(criada.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/requisicoes/{id}/aprovar", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APROVADA"));

        assertEquals(StatusRequisicao.APROVADA.name(), statusDaRequisicao(id));
        assertEquals(StatusBolsa.RESERVADA.name(), statusDaBolsa(bolsa.getId()));
    }

    @Test
    @DisplayName("Pela API: estoque insuficiente responde 422 e nada fica reservado no banco")
    void deveResponder422SemReservarNadaQuandoFaltaEstoque() throws Exception {
        Hospital hospital = hospitalRepository.save(novoHospital("77888999000102"));
        Bolsa bolsa = bolsaRepository.save(novaBolsa(TipoSanguineo.O_NEG, LocalDate.now().plusDays(10)));
        RequisicaoRequest pedido = new RequisicaoRequest(hospital.getId(), Prioridade.URGENTE,
                List.of(new ItemRequisicaoRequest(HemoComponente.HEMACIAS, TipoSanguineo.O_NEG, 2)));

        MvcResult criada = mockMvc.perform(post("/api/requisicoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(pedido)))
                .andExpect(status().isCreated())
                .andReturn();
        long id = objectMapper.readTree(criada.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(post("/api/requisicoes/{id}/aprovar", id))
                .andExpect(status().isUnprocessableEntity());

        assertEquals(StatusRequisicao.PENDENTE.name(), statusDaRequisicao(id));
        assertEquals(StatusBolsa.DISPONIVEL.name(), statusDaBolsa(bolsa.getId()));
    }

    private Hospital novoHospital(String cnpj) {
        Hospital hospital = new Hospital();
        hospital.setNome("Hospital de Teste " + cnpj);
        hospital.setCnpj(cnpj);
        return hospital;
    }

    private Bolsa novaBolsa(TipoSanguineo tipoSanguineo, LocalDate dataValidade) {
        return Bolsa.builder()
                .hemoComponente(HemoComponente.HEMACIAS)
                .tipoSanguineo(tipoSanguineo)
                .dataColeta(LocalDate.now().minusDays(5))
                .dataValidade(dataValidade)
                .status(StatusBolsa.DISPONIVEL)
                .build();
    }

    private ItemRequisicao novoItem(TipoSanguineo tipoSanguineo, int quantidade) {
        ItemRequisicao item = new ItemRequisicao();
        item.setHemoComponente(HemoComponente.HEMACIAS);
        item.setTipoSanguineo(tipoSanguineo);
        item.setQuantidade(quantidade);
        return item;
    }

    private Requisicao salvarRequisicao(Hospital hospital, ItemRequisicao... itens) {
        Requisicao requisicao = new Requisicao();
        requisicao.setHospitalSolicitante(hospital);
        requisicao.setPrioridade(Prioridade.NORMAL);
        List<ItemRequisicao> lista = new ArrayList<>();
        for (ItemRequisicao item : itens) {
            item.setRequisicao(requisicao);
            lista.add(item);
        }
        requisicao.setItens(lista);
        return requisicaoRepository.save(requisicao);
    }
}
