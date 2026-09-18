package com.hemorede.service;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.Prioridade;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.StatusRequisicao;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.domain.model.Hospital;
import com.hemorede.domain.model.ItemRequisicao;
import com.hemorede.domain.model.Requisicao;
import com.hemorede.exception.EstoqueInsuficienteException;
import com.hemorede.repository.BolsaRepository;
import com.hemorede.repository.HospitalRepository;
import com.hemorede.repository.RequisicaoRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes de integração do fluxo de aprovação de requisição (services +
 * repositories + H2), cobrindo FEFO e compatibilidade ABO/Rh.
 */
@SpringBootTest
class RequisicaoFluxoIntegrationTest {

    @Autowired
    private HospitalRepository hospitalRepository;
    @Autowired
    private BolsaRepository bolsaRepository;
    @Autowired
    private RequisicaoRepository requisicaoRepository;
    @Autowired
    private RequisicaoService requisicaoService;

    @Test
    @DisplayName("Aprova a requisição alocando a bolsa compatível com validade mais próxima (FEFO)")
    void deveAprovarRequisicaoComFEFO() {
        Hospital hospital = hospitalRepository.save(hospitalComCnpj("22333444000155"));

        // Duas bolsas O+ compatíveis: a de validade mais curta deve ser a escolhida.
        Bolsa bolsaVenceCedo = bolsaRepository.save(novaBolsa(TipoSanguineo.O_POS, LocalDate.now().plusDays(3)));
        Bolsa bolsaVenceDepois = bolsaRepository.save(novaBolsa(TipoSanguineo.O_POS, LocalDate.now().plusDays(20)));

        Requisicao requisicao = new Requisicao();
        requisicao.setHospitalSolicitante(hospital);
        requisicao.setPrioridade(Prioridade.NORMAL);

        ItemRequisicao item = new ItemRequisicao();
        item.setRequisicao(requisicao);
        item.setHemoComponente(HemoComponente.HEMACIAS);
        item.setTipoSanguineo(TipoSanguineo.O_POS);
        item.setQuantidade(1);
        requisicao.setItens(List.of(item));

        Requisicao salva = requisicaoRepository.save(requisicao);

        Requisicao aprovada = requisicaoService.aprovar(salva.getId());

        assertEquals(StatusRequisicao.APROVADA, aprovada.getStatus());
        Bolsa alocada = aprovada.getItens().get(0).getBolsasAlocadas().get(0);
        assertEquals(bolsaVenceCedo.getId(), alocada.getId(),
                "Deveria alocar a bolsa com validade mais próxima (FEFO)");

        Bolsa naoAlocada = bolsaRepository.findById(bolsaVenceDepois.getId()).orElseThrow();
        assertEquals(StatusBolsa.DISPONIVEL, naoAlocada.getStatus(),
                "A segunda bolsa não deveria ter sido tocada");
    }

    @Test
    @DisplayName("Não aloca bolsa de tipo sanguíneo incompatível (ABO/Rh)")
    void naoDeveAlocarBolsaIncompativel() {
        Hospital hospital = hospitalRepository.save(hospitalComCnpj("33444555000166"));

        // Só existe bolsa AB- em estoque; um receptor O- não pode recebê-la.
        bolsaRepository.save(novaBolsa(TipoSanguineo.AB_NEG, LocalDate.now().plusDays(10)));

        Requisicao requisicao = new Requisicao();
        requisicao.setHospitalSolicitante(hospital);
        requisicao.setPrioridade(Prioridade.URGENTE);

        ItemRequisicao item = new ItemRequisicao();
        item.setRequisicao(requisicao);
        item.setHemoComponente(HemoComponente.HEMACIAS);
        item.setTipoSanguineo(TipoSanguineo.O_NEG);
        item.setQuantidade(1);
        requisicao.setItens(List.of(item));

        Requisicao salva = requisicaoRepository.save(requisicao);

        assertThrows(EstoqueInsuficienteException.class, () -> requisicaoService.aprovar(salva.getId()));

        Requisicao aindaPendente = requisicaoRepository.findById(salva.getId()).orElseThrow();
        assertEquals(StatusRequisicao.PENDENTE, aindaPendente.getStatus(),
                "Requisição deve permanecer PENDENTE quando a alocação falha (rollback)");
    }

    @Test
    @DisplayName("Nunca aloca bolsa vencida, mesmo sendo a única do tipo em estoque")
    void naoDeveAlocarBolsaVencida() {
        Hospital hospital = hospitalRepository.save(hospitalComCnpj("44555666000177"));
        bolsaRepository.save(novaBolsa(TipoSanguineo.A_POS, LocalDate.now().minusDays(1))); // vencida

        Requisicao requisicao = new Requisicao();
        requisicao.setHospitalSolicitante(hospital);
        requisicao.setPrioridade(Prioridade.NORMAL);

        ItemRequisicao item = new ItemRequisicao();
        item.setRequisicao(requisicao);
        item.setHemoComponente(HemoComponente.HEMACIAS);
        item.setTipoSanguineo(TipoSanguineo.A_POS);
        item.setQuantidade(1);
        requisicao.setItens(List.of(item));

        Requisicao salva = requisicaoRepository.save(requisicao);

        assertThrows(EstoqueInsuficienteException.class, () -> requisicaoService.aprovar(salva.getId()));
    }

    private Hospital hospitalComCnpj(String cnpj) {
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
}
