package com.hemorede.config;

import com.hemorede.algoritmos.DadosSinteticos;
import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.Prioridade;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.StatusRequisicao;
import com.hemorede.domain.enums.StatusRota;
import com.hemorede.domain.enums.StatusVeiculo;
import com.hemorede.domain.enums.TipoRefrigeracao;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.domain.model.Doador;
import com.hemorede.domain.model.Estoque;
import com.hemorede.domain.model.Hospital;
import com.hemorede.domain.model.ItemRequisicao;
import com.hemorede.domain.model.Motorista;
import com.hemorede.domain.model.Requisicao;
import com.hemorede.domain.model.Rota;
import com.hemorede.domain.model.Veiculo;
import com.hemorede.repository.BolsaRepository;
import com.hemorede.repository.DoadorRepository;
import com.hemorede.repository.EstoqueRepository;
import com.hemorede.repository.HospitalRepository;
import com.hemorede.repository.MotoristaRepository;
import com.hemorede.repository.RequisicaoRepository;
import com.hemorede.repository.RotaRepository;
import com.hemorede.repository.VeiculoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Inicializador de dados sintéticos para demonstração e testes manuais da aplicação.
 * Atende à issue #9 ("Inicializar Dados Sintéticos com Hospitais e Estoque") e
 * fornece a massa para o painel de indicadores estatísticos e operacionais (HU07).
 *
 * Ativado apenas quando hemorede.seed.enabled=true (desativado por padrão em testes unitários/integrados).
 */
@Component
@ConditionalOnProperty(name = "hemorede.seed.enabled", havingValue = "true")
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final HospitalRepository hospitalRepository;
    private final EstoqueRepository estoqueRepository;
    private final DoadorRepository doadorRepository;
    private final BolsaRepository bolsaRepository;
    private final VeiculoRepository veiculoRepository;
    private final MotoristaRepository motoristaRepository;
    private final RotaRepository rotaRepository;
    private final RequisicaoRepository requisicaoRepository;

    public DataInitializer(HospitalRepository hospitalRepository,
                           EstoqueRepository estoqueRepository,
                           DoadorRepository doadorRepository,
                           BolsaRepository bolsaRepository,
                           VeiculoRepository veiculoRepository,
                           MotoristaRepository motoristaRepository,
                           RotaRepository rotaRepository,
                           RequisicaoRepository requisicaoRepository) {
        this.hospitalRepository = hospitalRepository;
        this.estoqueRepository = estoqueRepository;
        this.doadorRepository = doadorRepository;
        this.bolsaRepository = bolsaRepository;
        this.veiculoRepository = veiculoRepository;
        this.motoristaRepository = motoristaRepository;
        this.rotaRepository = rotaRepository;
        this.requisicaoRepository = requisicaoRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (hospitalRepository.count() > 0) {
            log.info("Banco de dados já contém registros. Carga de dados sintéticos ignorada.");
            return;
        }

        log.info("Inicializando dados sintéticos da Hemorrede para demonstração da Entrega 02...");

        // 1. Hospitais da malha viária (N1 a N5)
        Hospital h1 = hospitalRepository.save(new Hospital(null, "Hospital Regional Norte", "11.111.111/0001-11", "Zona Norte, Recife - PE", -8.0123, -34.8912, DadosSinteticos.N1_HOSPITAL_NORTE, null, null));
        Hospital h2 = hospitalRepository.save(new Hospital(null, "Hospital Metropolitano Sul", "22.222.222/0001-22", "Zona Sul, Recife - PE", -8.1234, -34.9123, DadosSinteticos.N2_HOSPITAL_SUL, null, null));
        Hospital h3 = hospitalRepository.save(new Hospital(null, "Hospital Esperança Leste", "33.333.333/0001-33", "Zona Leste, Recife - PE", -8.0567, -34.8765, DadosSinteticos.N3_HOSPITAL_LESTE, null, null));
        Hospital h4 = hospitalRepository.save(new Hospital(null, "Hospital Universitário Oeste", "44.444.444/0001-44", "Zona Oeste, Recife - PE", -8.0432, -34.9456, DadosSinteticos.N4_HOSPITAL_OESTE, null, null));
        Hospital h5 = hospitalRepository.save(new Hospital(null, "Hospital Central de Emergência II", "55.555.555/0001-55", "Área Central, Recife - PE", -8.0611, -34.8811, DadosSinteticos.N5_HOSPITAL_CENTRAL_II, null, null));

        // 2. Estoque Central
        Estoque estoqueCentral = estoqueRepository.save(new Estoque(null, h5, 1000, new ArrayList<>()));

        // 3. Doador Sintético Padrão
        Doador doador = doadorRepository.save(new Doador(null, "Doador Sintético Central", "000.111.222-33", TipoSanguineo.O_POS, LocalDate.now().minusMonths(4), new ArrayList<>()));

        // 4. Bolsas Sintéticas (com validades variadas para testar e demonstrar FEFO)
        List<Bolsa> bolsasExemplo = DadosSinteticos.criarBolsasExemplo();
        for (Bolsa b : bolsasExemplo) {
            Bolsa nova = Bolsa.builder()
                    .hemoComponente(b.getHemoComponente())
                    .tipoSanguineo(b.getTipoSanguineo())
                    .dataColeta(b.getDataColeta())
                    .dataValidade(b.getDataValidade())
                    .status(StatusBolsa.DISPONIVEL)
                    .doador(doador)
                    .estoque(estoqueCentral)
                    .build();
            bolsaRepository.save(nova);
        }

        // 5. Veículos com refrigerações diversas
        Veiculo v1 = veiculoRepository.save(new Veiculo(null, "ROTA-1001", TipoRefrigeracao.REFRIGERADO_2_6, 200, StatusVeiculo.DISPONIVEL));
        veiculoRepository.save(new Veiculo(null, "ROTA-1002", TipoRefrigeracao.CONGELADO_MENOS_20, 150, StatusVeiculo.DISPONIVEL));
        veiculoRepository.save(new Veiculo(null, "ROTA-1003", TipoRefrigeracao.AMBIENTE_CONTROLADO_20_24, 100, StatusVeiculo.DISPONIVEL));

        // 6. Motorista Sintético
        Motorista motorista = motoristaRepository.save(new Motorista(null, "Carlos Eduardo Silva", "12345678900", "(81) 98765-4321"));

        // 7. Rota Concluída para cálculo de atendimento e tempo nos indicadores (HU07)
        LocalDateTime base = LocalDateTime.now();
        Rota rotaConcluida = new Rota();
        rotaConcluida.setVeiculo(v1);
        rotaConcluida.setMotorista(motorista);
        rotaConcluida.setStatus(StatusRota.CONCLUIDA);
        rotaConcluida.setDataSaida(base.minusMinutes(75));
        rotaConcluida.setDataChegadaPrevista(base.minusMinutes(30));
        rotaRepository.save(rotaConcluida);

        // 8. Requisições Sintéticas com múltiplos status (Pendente, Aprovada e Entregue)
        Requisicao req1 = new Requisicao();
        req1.setHospitalSolicitante(h1);
        req1.setPrioridade(Prioridade.URGENTE);
        req1.setDataSolicitacao(base.minusHours(1));
        req1.setStatus(StatusRequisicao.PENDENTE);

        ItemRequisicao item1 = new ItemRequisicao();
        item1.setRequisicao(req1);
        item1.setHemoComponente(HemoComponente.HEMACIAS);
        item1.setTipoSanguineo(TipoSanguineo.O_POS);
        item1.setQuantidade(1);
        req1.setItens(List.of(item1));
        requisicaoRepository.save(req1);

        Requisicao req2 = new Requisicao();
        req2.setHospitalSolicitante(h2);
        req2.setPrioridade(Prioridade.URGENTE);
        req2.setDataSolicitacao(base.minusHours(2));
        req2.setStatus(StatusRequisicao.APROVADA);

        ItemRequisicao item2 = new ItemRequisicao();
        item2.setRequisicao(req2);
        item2.setHemoComponente(HemoComponente.PLAQUETAS);
        item2.setTipoSanguineo(TipoSanguineo.A_POS);
        item2.setQuantidade(2);
        req2.setItens(List.of(item2));
        requisicaoRepository.save(req2);

        Requisicao req3 = new Requisicao();
        req3.setHospitalSolicitante(h3);
        req3.setPrioridade(Prioridade.NORMAL);
        req3.setDataSolicitacao(base.minusMinutes(75));
        req3.setStatus(StatusRequisicao.ENTREGUE);
        req3.setRota(rotaConcluida);

        ItemRequisicao item3 = new ItemRequisicao();
        item3.setRequisicao(req3);
        item3.setHemoComponente(HemoComponente.HEMACIAS);
        item3.setTipoSanguineo(TipoSanguineo.O_NEG);
        item3.setQuantidade(1);
        req3.setItens(List.of(item3));
        requisicaoRepository.save(req3);

        log.info("Carga inicial de dados sintéticos concluída com sucesso! (Hospitais N1-N5, Bolsas, Veículos, Motorista e Requisições prontos para uso)");
    }
}
