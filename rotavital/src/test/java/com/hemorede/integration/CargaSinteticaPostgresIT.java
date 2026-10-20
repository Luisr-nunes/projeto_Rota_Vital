package com.hemorede.integration;

import com.hemorede.algoritmos.DadosSinteticos;
import com.hemorede.config.DataInitializer;
import com.hemorede.repository.BolsaRepository;
import com.hemorede.repository.DoadorRepository;
import com.hemorede.repository.EstoqueRepository;
import com.hemorede.repository.HospitalRepository;
import com.hemorede.repository.MotoristaRepository;
import com.hemorede.repository.RequisicaoRepository;
import com.hemorede.repository.RotaRepository;
import com.hemorede.repository.VeiculoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * PI2-191 — a carga de dados sintéticos é o que o Docker Compose liga por padrão
 * ({@code HEMOREDE_SEED_ENABLED=true}) contra o PostgreSQL, mas os demais testes a desligam.
 * Aqui ela roda de verdade no PostgreSQL, uma vez e depois de novo, para provar que não duplica dados.
 *
 * <p>O {@link DataInitializer} só existe como bean com {@code hemorede.seed.enabled=true}; para não
 * criar um segundo contexto Spring (e brigar pelo mesmo banco com o primeiro), ele é instanciado aqui
 * com os repositórios do contexto e executado dentro de uma transação, como o Spring faria.</p>
 */
class CargaSinteticaPostgresIT extends PostgresIntegrationTestBase {

    @Autowired
    private HospitalRepository hospitalRepository;
    @Autowired
    private EstoqueRepository estoqueRepository;
    @Autowired
    private DoadorRepository doadorRepository;
    @Autowired
    private BolsaRepository bolsaRepository;
    @Autowired
    private VeiculoRepository veiculoRepository;
    @Autowired
    private MotoristaRepository motoristaRepository;
    @Autowired
    private RotaRepository rotaRepository;
    @Autowired
    private RequisicaoRepository requisicaoRepository;
    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @DisplayName("A carga sintética roda no PostgreSQL e não duplica os dados na segunda execução")
    void deveCarregarDadosSinteticosUmaUnicaVez() {
        DataInitializer inicializador = new DataInitializer(hospitalRepository, estoqueRepository,
                doadorRepository, bolsaRepository, veiculoRepository, motoristaRepository,
                rotaRepository, requisicaoRepository);
        TransactionTemplate transacao = new TransactionTemplate(transactionManager);

        transacao.executeWithoutResult(status -> inicializador.run());

        assertEquals(5, contar("hospital"));
        assertEquals(1, contar("estoque"));
        assertEquals(3, contar("doador"));
        assertEquals(DadosSinteticos.criarBolsasExemplo().size(), contar("bolsa"));
        assertEquals(3, contar("veiculo"));
        assertEquals(1, contar("motorista"));
        assertEquals(1, contar("rota"));
        assertEquals(3, contar("requisicao"));

        long bolsasAntes = contar("bolsa");
        transacao.executeWithoutResult(status -> inicializador.run());

        assertEquals(5, contar("hospital"), "A segunda execução não deve recriar hospitais");
        assertEquals(bolsasAntes, contar("bolsa"), "A segunda execução não deve recriar bolsas");
        assertEquals(3, contar("requisicao"), "A segunda execução não deve recriar requisições");
    }
}
