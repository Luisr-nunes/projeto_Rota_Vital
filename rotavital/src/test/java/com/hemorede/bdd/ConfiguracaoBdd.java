package com.hemorede.bdd;

import io.cucumber.spring.CucumberContextConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;

@CucumberContextConfiguration
@SpringBootTest(properties = {
    "spring.datasource.url=jdbc:h2:mem:aceitacao_bdd;DB_CLOSE_DELAY=-1",
    "spring.jpa.hibernate.ddl-auto=create-drop",
    "hemorede.seed.enabled=false"
})
@AutoConfigureMockMvc
public class ConfiguracaoBdd {
}
