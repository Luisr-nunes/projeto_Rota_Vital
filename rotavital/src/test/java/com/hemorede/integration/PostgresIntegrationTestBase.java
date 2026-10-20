package com.hemorede.integration;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Base dos testes de integração contra um PostgreSQL de verdade (PI2-191).
 *
 * <p>Os testes {@code *Test} usam H2 em memória e rodam com {@code mvn test}. Os testes {@code *IT}
 * que herdam desta classe sobem um container PostgreSQL via Testcontainers e rodam com
 * {@code mvn verify} (Maven Failsafe). Precisam de Docker disponível; no GitHub Actions o runner
 * {@code ubuntu-latest} já tem.</p>
 *
 * <p>O container é um singleton da JVM: sobe uma vez, é compartilhado por todas as classes {@code *IT}
 * e é removido ao final pelo Ryuk do Testcontainers. Container por classe com
 * {@code @DynamicPropertySource} seria um erro: o Spring reaproveita o contexto em cache, que ficaria
 * apontando para o container da classe anterior, já parado.</p>
 *
 * <p>A imagem é a mesma do {@code compose.yaml} ({@code postgres:16-alpine}), para testar na versão
 * que roda na máquina de quem desenvolve.</p>
 */
@SpringBootTest(properties = {
        // O application.yml de teste substitui o de main, então o Hibernate não cria o schema sozinho
        // fora de um banco embarcado: pedimos explicitamente.
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=false",
        "spring.h2.console.enabled=false",
        "hemorede.seed.enabled=false"
})
@AutoConfigureMockMvc
public abstract class PostgresIntegrationTestBase {

    private static final DockerImageName IMAGEM_POSTGRES = DockerImageName.parse("postgres:16-alpine");

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(IMAGEM_POSTGRES)
            .withDatabaseName("rotavital_test")
            .withUsername("rotavital")
            .withPassword("rotavital");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configurarDataSource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    protected JdbcTemplate jdbc;

    /** Cada teste começa com todas as tabelas vazias e os ids reiniciados. */
    @BeforeEach
    protected void limparBanco() {
        List<String> tabelas = jdbc.queryForList(
                "SELECT tablename FROM pg_tables WHERE schemaname = 'public'", String.class);
        if (tabelas.isEmpty()) {
            return;
        }
        String lista = tabelas.stream()
                .map(nome -> "\"" + nome + "\"")
                .collect(Collectors.joining(", "));
        jdbc.execute("TRUNCATE TABLE " + lista + " RESTART IDENTITY CASCADE");
    }

    protected long contar(String tabela) {
        Long total = jdbc.queryForObject("SELECT count(*) FROM " + tabela, Long.class);
        return total == null ? 0 : total;
    }

    protected String statusDaBolsa(Long id) {
        return jdbc.queryForObject("SELECT status FROM bolsa WHERE id = ?", String.class, id);
    }

    protected String statusDaRequisicao(Long id) {
        return jdbc.queryForObject("SELECT status FROM requisicao WHERE id = ?", String.class, id);
    }
}
