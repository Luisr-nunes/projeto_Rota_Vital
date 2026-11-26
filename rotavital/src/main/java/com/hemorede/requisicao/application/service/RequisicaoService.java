package com.hemorede.requisicao.application.service;

import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.requisicao.domain.enums.StatusRequisicao;
import com.hemorede.exception.EstoqueInsuficienteException;
import com.hemorede.exception.EstoqueCompativelInsuficienteException;
import com.hemorede.exception.NenhumaBolsaCompativelException;
import com.hemorede.exception.TransicaoStatusInvalidaException;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.domain.model.Hospital;
import com.hemorede.requisicao.domain.model.ItemRequisicao;
import com.hemorede.requisicao.domain.model.Requisicao;
import com.hemorede.domain.regras.RegraTransicaoRequisicao;
import com.hemorede.requisicao.application.dto.ItemRequisicaoRequest;
import com.hemorede.requisicao.application.dto.RequisicaoRequest;
import com.hemorede.repository.HospitalRepository;
import com.hemorede.requisicao.infrastructure.repository.RequisicaoRepository;
import com.hemorede.estoque.api.ReservaEstoque;
import com.hemorede.expedicao.application.service.RastreamentoService;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Regra de negócio 8: uma requisição só é aprovada quando todos os seus
 * itens conseguem ter a quantidade total de bolsas compatíveis alocadas.
 */
@Service
public class RequisicaoService {

    /** Status de bolsa que podem passar a UTILIZADA na entrega. */
    private static final Set<StatusBolsa> BOLSAS_ENTREGAVEIS =
            EnumSet.of(StatusBolsa.RESERVADA, StatusBolsa.EM_TRANSITO);

    private final RequisicaoRepository requisicaoRepository;
    private final HospitalRepository hospitalRepository;
    private final ReservaEstoque reservaEstoque;
    /** Opcional para manter o construtor dos testes unitários; encerra a rota na entrega. */
    private RastreamentoService rastreamentoService;

    /** Métricas de negócio (observabilidade): contador de requisições e tempo de alocação. */
    private final Counter requisicoesCriadas;
    private final Timer tempoAlocacao;

    public RequisicaoService(RequisicaoRepository requisicaoRepository,
                             HospitalRepository hospitalRepository,
                             ReservaEstoque reservaEstoque,
                             MeterRegistry registry) {
        this.requisicaoRepository = requisicaoRepository;
        this.hospitalRepository = hospitalRepository;
        this.reservaEstoque = reservaEstoque;
        this.requisicoesCriadas = Counter.builder("hemorede.requisicoes.criadas")
                .description("Total de requisições hospitalares registradas")
                .register(registry);
        this.tempoAlocacao = Timer.builder("hemorede.requisicoes.alocacao")
                .description("Tempo para reservar as bolsas compatíveis de uma requisição aprovada")
                .register(registry);
    }

    public List<Requisicao> listar() {
        return requisicaoRepository.findAll();
    }

    /** @throws IllegalArgumentException se a requisição não existir (404) */
    public Requisicao buscarPorId(Long id) {
        return requisicaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Requisição não encontrada: " + id));
    }

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setRastreamentoService(RastreamentoService rastreamentoService) {
        this.rastreamentoService = rastreamentoService;
    }

    /**
     * HU03 - Registrar requisição hospitalar. Monta a requisição a partir do
     * DTO validado, vinculada a um hospital existente, sempre com status
     * {@code PENDENTE} e data de solicitação definida pelo servidor.
     *
     * @throws IllegalArgumentException se o hospital não existir (404)
     */
    @Transactional
    public Requisicao registrar(RequisicaoRequest request) {
        Hospital hospital = hospitalRepository.findById(request.hospitalId())
                .orElseThrow(() -> new IllegalArgumentException("Hospital não encontrado: " + request.hospitalId()));

        Requisicao requisicao = new Requisicao();
        requisicao.setHospitalSolicitante(hospital);
        requisicao.setPrioridade(request.prioridade());
        requisicao.setStatus(StatusRequisicao.PENDENTE);

        List<ItemRequisicao> itens = new ArrayList<>();
        for (ItemRequisicaoRequest dadosItem : request.itens()) {
            ItemRequisicao item = new ItemRequisicao();
            item.setRequisicao(requisicao);
            item.setHemoComponente(dadosItem.hemoComponente());
            item.setTipoSanguineo(dadosItem.tipoSanguineo());
            item.setQuantidade(dadosItem.quantidade());
            itens.add(item);
        }
        requisicao.setItens(itens);

        Requisicao salva = requisicaoRepository.save(requisicao);
        requisicoesCriadas.increment();
        return salva;
    }

    @Transactional
    public Requisicao aprovar(Long requisicaoId) {
        Requisicao requisicao = requisicaoRepository.findById(requisicaoId)
                .orElseThrow(() -> new IllegalArgumentException("Requisição não encontrada: " + requisicaoId));

        // RN-09: só PENDENTE pode ser aprovada. Antes, uma requisição ENTREGUE voltava a APROVADA.
        if (!RegraTransicaoRequisicao.permitida(requisicao.getStatus(), StatusRequisicao.APROVADA)) {
            throw new TransicaoStatusInvalidaException("Requisição " + requisicaoId + " está "
                    + requisicao.getStatus() + "; só requisição PENDENTE pode ser aprovada");
        }

        // Se algum item não conseguir ser 100% alocado, a EstoqueInsuficienteException
        // propaga e o Spring reverte toda a transação (nenhuma bolsa fica
        // parcialmente reservada) — a requisição permanece com o status atual
        // (PENDENTE), coerente com a HU04. Um "save" explícito aqui antes de
        // relançar a exceção seria desfeito pelo próprio rollback, por isso
        // não é necessário.
        Timer.Sample amostraAlocacao = Timer.start();
        try {
            for (ItemRequisicao item : requisicao.getItens()) {
                try {
                    int jaAlocadas = item.getBolsasAlocadas() == null ? 0 : item.getBolsasAlocadas().size();
                    int quantidadeRestante = item.getQuantidade() - jaAlocadas;
                    if (quantidadeRestante > 0) {
                        if (item.getBolsasAlocadas() == null) {
                            item.setBolsasAlocadas(new ArrayList<>());
                        }
                        item.getBolsasAlocadas().addAll(reservaEstoque.reservar(
                                item.getHemoComponente(), item.getTipoSanguineo(), quantidadeRestante));
                    }
                } catch (NenhumaBolsaCompativelException | EstoqueCompativelInsuficienteException ex) {
                    // Preserva o contrato 422 do endpoint legado /aprovar.
                    throw new EstoqueInsuficienteException(ex.getMessage());
                }
            }
        } finally {
            // Mede a alocação mesmo quando falha (estoque insuficiente): tempo gasto até a exceção importa pro painel.
            amostraAlocacao.stop(tempoAlocacao);
        }

        requisicao.setStatus(StatusRequisicao.APROVADA);
        return requisicaoRepository.save(requisicao);
    }

    /**
     * HU06 - Confirmar entrega. Só vale para requisição com bolsas já
     * reservadas (ALOCADA, APROVADA ou EM_ROTA): ela passa a ENTREGUE e cada
     * bolsa RESERVADA/EM_TRANSITO passa a UTILIZADA. Qualquer outro status
     * (PENDENTE, ENTREGUE, CANCELADA) é recusado sem alterar nada.
     *
     * @throws IllegalArgumentException se a requisição não existir (404)
     * @throws TransicaoStatusInvalidaException se a transição não for permitida (409)
     */
    @Transactional
    public Requisicao confirmarEntrega(Long requisicaoId) {
        Requisicao requisicao = requisicaoRepository.findById(requisicaoId)
                .orElseThrow(() -> new IllegalArgumentException("Requisição não encontrada: " + requisicaoId));

        if (!RegraTransicaoRequisicao.permitida(requisicao.getStatus(), StatusRequisicao.ENTREGUE)) {
            throw new TransicaoStatusInvalidaException("Requisição " + requisicaoId + " está "
                    + requisicao.getStatus() + "; só é possível confirmar a entrega de requisição alocada");
        }

        for (ItemRequisicao item : requisicao.getItens()) {
            for (Bolsa bolsa : item.getBolsasAlocadas()) {
                if (!BOLSAS_ENTREGAVEIS.contains(bolsa.getStatus())) {
                    throw new TransicaoStatusInvalidaException("Bolsa " + bolsa.getId() + " está "
                            + bolsa.getStatus() + " e não pode ser entregue");
                }
                bolsa.setStatus(StatusBolsa.UTILIZADA);
            }
        }

        requisicao.setStatus(StatusRequisicao.ENTREGUE);
        if (rastreamentoService != null) {
            rastreamentoService.encerrarRota(requisicao);
        }
        return requisicaoRepository.save(requisicao);
    }
}
