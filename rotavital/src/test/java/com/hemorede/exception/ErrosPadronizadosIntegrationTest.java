package com.hemorede.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Erros de entrada respondem no formato {erro, mensagem} em português, com o que fazer. */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:erros_padronizados;DB_CLOSE_DELAY=-1",
        "hemorede.seed.enabled=false"
})
@AutoConfigureMockMvc
@DisplayName("Erros padronizados da API")
class ErrosPadronizadosIntegrationTest {

    @Autowired private MockMvc mvc;

    @Test
    @DisplayName("JSON malformado: 400 CorpoInvalido")
    void jsonMalformado() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CorpoInvalido"))
                .andExpect(jsonPath("$.mensagem").value(containsString("JSON")));
    }

    @Test
    @DisplayName("Enum ou data inválidos: 400 citando o campo")
    void valorInvalidoNoCampo() throws Exception {
        String corpo = """
                {"nome":"Ana","email":"ana@teste.dev","senha":"senha-segura-1","cpf":"12345678909",
                 "tipoSanguineo":"Z_POS","sexo":"FEMININO","dataNascimento":"1995-04-10","pesoKg":60}
                """;
        mvc.perform(post("/api/auth/cadastro-doador").contentType(MediaType.APPLICATION_JSON).content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erro").value("CorpoInvalido"))
                .andExpect(jsonPath("$.mensagem").value(containsString("tipoSanguineo")));
    }

    @Test
    @WithMockUser(roles = "HEMOCENTRO")
    @DisplayName("Método não aceito: 405 em português")
    void metodoNaoAceito() throws Exception {
        mvc.perform(put("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.erro").value("MetodoNaoSuportado"));
    }

    @Test
    @DisplayName("Content-Type errado: 415 pedindo JSON")
    void formatoNaoSuportado() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.TEXT_PLAIN).content("x"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.erro").value("FormatoNaoSuportado"));
    }
}
