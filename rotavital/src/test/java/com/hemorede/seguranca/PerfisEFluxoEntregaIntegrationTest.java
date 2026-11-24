package com.hemorede.seguranca;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hemorede.algoritmos.DadosSinteticos;
import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.Perfil;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.expedicao.domain.enums.StatusVeiculo;
import com.hemorede.expedicao.domain.enums.TipoRefrigeracao;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.domain.model.Hospital;
import com.hemorede.expedicao.domain.model.Motorista;
import com.hemorede.expedicao.domain.model.Veiculo;
import com.hemorede.dto.NovoUsuarioRequest;
import com.hemorede.repository.BolsaRepository;
import com.hemorede.repository.HospitalRepository;
import com.hemorede.expedicao.infrastructure.repository.MotoristaRepository;
import com.hemorede.expedicao.infrastructure.repository.VeiculoRepository;
import com.hemorede.service.AutenticacaoService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Autenticação JWT, autorização dos 3 perfis, isolamento entre hospitais (404) e o fluxo
 * solicitar (HOSPITAL) -> alocar e despachar (HEMOCENTRO) -> rastrear -> confirmar recebimento (HOSPITAL).
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:perfis_fluxo;DB_CLOSE_DELAY=-1",
        "hemorede.seed.enabled=false"
})
@AutoConfigureMockMvc
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Perfis (JWT) e fluxo de entrega com rastreamento")
class PerfisEFluxoEntregaIntegrationTest {

    private static final String SENHA = "senha-de-teste-123";

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private AutenticacaoService autenticacao;
    @Autowired private HospitalRepository hospitais;
    @Autowired private BolsaRepository bolsas;
    @Autowired private VeiculoRepository veiculos;
    @Autowired private MotoristaRepository motoristas;

    private Hospital norte;
    private Veiculo refrigerado;

    @BeforeAll
    void preparar() {
        norte = hospitais.save(new Hospital(null, "Hospital Norte", "10.000.000/0001-01", "Recife", -8.0123, -34.8912,
                DadosSinteticos.N1_HOSPITAL_NORTE, null, null));
        Hospital sul = hospitais.save(new Hospital(null, "Hospital Sul", "20.000.000/0001-02", "Recife", -8.1234, -34.9123,
                DadosSinteticos.N2_HOSPITAL_SUL, null, null));
        refrigerado = veiculos.save(new Veiculo(null, "TST-0001", TipoRefrigeracao.REFRIGERADO_2_6, 100, StatusVeiculo.DISPONIVEL));
        motoristas.save(new Motorista(null, "Motorista Teste", "99999999999", "(81) 90000-0000"));
        for (int i = 0; i < 3; i++) {
            bolsas.save(Bolsa.builder().hemoComponente(HemoComponente.HEMACIAS).tipoSanguineo(TipoSanguineo.O_POS)
                    .dataColeta(LocalDate.now().minusDays(5)).dataValidade(LocalDate.now().plusDays(10 + i))
                    .status(StatusBolsa.DISPONIVEL).build());
        }
        autenticacao.criarUsuario(new NovoUsuarioRequest("Operador", "op@teste.dev", SENHA, Perfil.HEMOCENTRO, null));
        autenticacao.criarUsuario(new NovoUsuarioRequest("Banco Norte", "norte@teste.dev", SENHA, Perfil.HOSPITAL, norte.getId()));
        autenticacao.criarUsuario(new NovoUsuarioRequest("Banco Sul", "sul@teste.dev", SENHA, Perfil.HOSPITAL, sul.getId()));
    }

    private String token(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + SENHA + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(body).get("token").asText();
    }

    @Test
    @DisplayName("Sem token a API responde 401; páginas e health continuam públicos")
    void semToken() throws Exception {
        mvc.perform(get("/api/v1/requisicoes")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.erro").value("NaoAutenticado"));
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Login: senha errada e e-mail inexistente dão o mesmo 401")
    void loginInvalido() throws Exception {
        for (String corpo : new String[]{
                "{\"email\":\"op@teste.dev\",\"senha\":\"errada-123\"}",
                "{\"email\":\"ninguem@teste.dev\",\"senha\":\"errada-123\"}"}) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.mensagem").value("E-mail ou senha incorretos."));
        }
    }

    @Test
    @DisplayName("Cinco falhas seguidas bloqueiam o e-mail com 429")
    void bloqueioPorTentativas() throws Exception {
        String corpo = "{\"email\":\"forca-bruta@teste.dev\",\"senha\":\"errada-123\"}";
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.erro").value("MuitasTentativasException"));
    }

    @Test
    @DisplayName("Token adulterado é recusado com 401")
    void tokenAdulterado() throws Exception {
        String t = token("op@teste.dev");
        String adulterado = t.substring(0, t.length() - 3) + (t.endsWith("AAA") ? "BBB" : "AAA");
        mvc.perform(get("/api/v1/requisicoes").header("Authorization", adulterado)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Doador se cadastra, vê só o próprio painel e não acessa a API do hemocentro")
    void doador() throws Exception {
        String cadastro = """
                {"nome":"Ana Doadora","email":"ana@teste.dev","senha":"%s","cpf":"123.456.789-01",
                 "tipoSanguineo":"A_NEG","sexo":"FEMININO","dataNascimento":"1995-04-10","pesoKg":62.5}
                """.formatted(SENHA);
        String body = mvc.perform(post("/api/auth/cadastro-doador").contentType(MediaType.APPLICATION_JSON).content(cadastro))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil").value("DOADOR"))
                .andReturn().getResponse().getContentAsString();
        String t = "Bearer " + json.readTree(body).get("token").asText();

        mvc.perform(post("/api/auth/cadastro-doador").contentType(MediaType.APPLICATION_JSON).content(cadastro))
                .andExpect(status().isConflict());

        mvc.perform(get("/api/doador/me").header("Authorization", t))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tipoSanguineo").value("A_NEG"))
                .andExpect(jsonPath("$.apto").value(true))
                .andExpect(jsonPath("$.cpfMascarado").value("***.456.789-**"));

        mvc.perform(get("/api/v1/requisicoes").header("Authorization", t)).andExpect(status().isForbidden());
        mvc.perform(get("/api/hospital/requisicoes").header("Authorization", t)).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Cadastro de doador com dados inválidos responde 400 com os campos")
    void doadorInvalido() throws Exception {
        mvc.perform(post("/api/auth/cadastro-doador").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"x\",\"senha\":\"123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.email").exists())
                .andExpect(jsonPath("$.erros.senha").exists());
    }

    @Test
    @DisplayName("Fluxo completo: hospital solicita, hemocentro aloca e despacha, hospital rastreia e confirma")
    void fluxoCompleto() throws Exception {
        String hospNorte = token("norte@teste.dev");
        String hospSul = token("sul@teste.dev");
        String hemocentro = token("op@teste.dev");

        // o hospital nunca escolhe o hospitalId: vem do token (o campo extra no corpo é ignorado)
        String criada = mvc.perform(post("/api/hospital/requisicoes").header("Authorization", hospNorte)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"hospitalId\":999,\"prioridade\":\"URGENTE\",\"itens\":[{\"hemoComponente\":\"HEMACIAS\",\"tipoSanguineo\":\"O_POS\",\"quantidade\":2}]}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDENTE"))
                .andExpect(jsonPath("$.hospitalSolicitante.id").value(norte.getId()))
                .andReturn().getResponse().getContentAsString();
        long id = json.readTree(criada).get("id").asLong();

        // outro hospital não enxerga a requisição (404, não 403)
        mvc.perform(get("/api/hospital/requisicoes/{id}", id).header("Authorization", hospSul)).andExpect(status().isNotFound());
        mvc.perform(get("/api/hospital/requisicoes/{id}/rastreamento", id).header("Authorization", hospSul)).andExpect(status().isNotFound());
        mvc.perform(patch("/api/hospital/requisicoes/{id}/recebimento", id).header("Authorization", hospSul)).andExpect(status().isNotFound());
        // hospital não usa a API do hemocentro
        mvc.perform(post("/api/v1/requisicoes/{id}/despacho", id).header("Authorization", hospNorte)).andExpect(status().isForbidden());

        // despachar antes de alocar: 409
        mvc.perform(post("/api/v1/requisicoes/{id}/despacho", id).header("Authorization", hemocentro)).andExpect(status().isConflict());

        mvc.perform(post("/api/v1/requisicoes/{id}/alocacao", id).header("Authorization", hemocentro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ALOCADA"));

        mvc.perform(post("/api/v1/requisicoes/{id}/despacho", id).header("Authorization", hemocentro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ROTA"))
                .andExpect(jsonPath("$.trajeto[0].no").value("N0"))
                .andExpect(jsonPath("$.veiculoPlaca").value("TST-0001"))
                .andExpect(jsonPath("$.googleMapsUrl").value(org.hamcrest.Matchers.startsWith("https://www.google.com/maps/dir/?api=1")));
        assertEquals(StatusVeiculo.EM_ROTA, veiculos.findById(refrigerado.getId()).orElseThrow().getStatus());

        String rast = mvc.perform(get("/api/hospital/requisicoes/{id}/rastreamento", id).header("Authorization", hospNorte))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("EM_ROTA"))
                .andReturn().getResponse().getContentAsString();
        JsonNode r = json.readTree(rast);
        JsonNode trajeto = r.get("trajeto");
        assertEquals("N1", trajeto.get(trajeto.size() - 1).get("no").asText());
        assertTrue(r.get("percentual").asDouble() >= 0 && r.get("percentual").asDouble() <= 100);
        assertFalse(r.get("previsaoChegada").isNull());

        // FEFO: saíram as duas bolsas que vencem antes; a de validade mais longa ficou
        long emTransito = bolsas.findAll().stream().filter(b -> b.getStatus() == StatusBolsa.EM_TRANSITO).count();
        assertEquals(2, emTransito);
        Bolsa ficou = bolsas.findAll().stream().filter(b -> b.getStatus() == StatusBolsa.DISPONIVEL).findFirst().orElseThrow();
        assertEquals(LocalDate.now().plusDays(12), ficou.getDataValidade());

        mvc.perform(patch("/api/hospital/requisicoes/{id}/recebimento", id).header("Authorization", hospNorte))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ENTREGUE"));
        assertEquals(StatusVeiculo.DISPONIVEL, veiculos.findById(refrigerado.getId()).orElseThrow().getStatus());

        mvc.perform(get("/api/hospital/requisicoes/{id}/rastreamento", id).header("Authorization", hospNorte))
                .andExpect(jsonPath("$.percentual").value(100.0))
                .andExpect(jsonPath("$.minutosRestantes").value(0));

        // a lista do outro hospital continua vazia
        String lista = mvc.perform(get("/api/hospital/requisicoes").header("Authorization", hospSul))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        assertEquals(0, json.readTree(lista).size());
    }

    @Test
    @DisplayName("Hemocentro cria conta de hospital; conta DOADOR só por autocadastro")
    void gestaoDeContas() throws Exception {
        String hemocentro = token("op@teste.dev");
        mvc.perform(post("/api/usuarios").header("Authorization", hemocentro).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Novo\",\"email\":\"novo@teste.dev\",\"senha\":\"%s\",\"perfil\":\"HOSPITAL\",\"hospitalId\":%d}"
                                .formatted(SENHA, norte.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.hospitalId").value(norte.getId()))
                .andExpect(jsonPath("$.senhaHash").doesNotExist());
        mvc.perform(post("/api/usuarios").header("Authorization", hemocentro).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"X\",\"email\":\"x@teste.dev\",\"senha\":\"%s\",\"perfil\":\"DOADOR\"}".formatted(SENHA)))
                .andExpect(status().isBadRequest());
        String lista = mvc.perform(get("/api/usuarios").header("Authorization", hemocentro))
                .andReturn().getResponse().getContentAsString();
        assertFalse(lista.contains("senhaHash"));
        assertFalse(lista.contains("$2a$"));
    }
}
