package com.hemorede.service;

import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.StatusRequisicao;
import com.hemorede.exception.EstoqueInsuficienteException;
import com.hemorede.exception.EstoqueCompativelInsuficienteException;
import com.hemorede.exception.NenhumaBolsaCompativelException;
import com.hemorede.exception.TransicaoStatusInvalidaException;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.domain.model.Hospital;
import com.hemorede.domain.model.ItemRequisicao;
import com.hemorede.domain.model.Requisicao;
import com.hemorede.dto.ItemRequisicaoRequest;
import com.hemorede.dto.RequisicaoRequest;
import com.hemorede.repository.HospitalRepository;
import com.hemorede.repository.RequisicaoRepository;
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

    /** Status a partir dos quais a entrega pode ser confirmada (bolsas já reservadas). */
    private static final Set<StatusRequisicao> PRONTAS_PARA_ENTREGA =
            EnumSet.of(StatusRequisicao.ALOCADA, StatusRequisicao.APROVADA, StatusRequisicao.EM_ROTA);

    /** Status de bolsa que podem passar a UTILIZADA na entrega. */
    private static final Set<StatusBolsa> BOLSAS_ENTREGAVEIS =
            EnumSet.of(StatusBolsa.RESERVADA, StatusBolsa.EM_TRANSITO);

    private final RequisicaoRepository requisicaoRepository;
    private final HospitalRepository hospitalRepository;
    private final AlocacaoService alocacaoService;

    public RequisicaoService(RequisicaoRepository requisicaoRepository,
                             HospitalRepository hospitalRepository,
                             AlocacaoService alocacaoService) {
        this.requisicaoRepository = requisicaoRepository;
        this.hospitalRepository = hospitalRepository;
        this.alocacaoService = alocacaoService;
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

        return requisicaoRepository.save(requisicao);
    }

    @Transactional
    public Requisicao aprovar(Long requisicaoId) {
        Requisicao requisicao = requisicaoRepository.findById(requisicaoId)
                .orElseThrow(() -> new IllegalArgumentException("Requisição não encontrada: " + requisicaoId));

        // Se algum item não conseguir ser 100% alocado, a EstoqueInsuficienteException
        // propaga e o Spring reverte toda a transação (nenhuma bolsa fica
        // parcialmente reservada) — a requisição permanece com o status atual
        // (PENDENTE), coerente com a HU04. Um "save" explícito aqui antes de
        // relançar a exceção seria desfeito pelo próprio rollback, por isso
        // não é necessário.
        for (ItemRequisicao item : requisicao.getItens()) {
            try {
                alocacaoService.alocar(item);
            } catch (NenhumaBolsaCompativelException | EstoqueCompativelInsuficienteException ex) {
                // Preserva o contrato 422 do endpoint legado /aprovar.
                throw new EstoqueInsuficienteException(ex.getMessage());
            }
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

        if (!PRONTAS_PARA_ENTREGA.contains(requisicao.getStatus())) {
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
        return requisicaoRepository.save(requisicao);
    }
}
