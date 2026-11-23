package com.hemorede.service;

import com.hemorede.domain.model.Hospital;
import com.hemorede.requisicao.domain.model.Requisicao;
import com.hemorede.dto.HospitalRequisicaoRequest;
import com.hemorede.expedicao.application.dto.RastreamentoResponse;
import com.hemorede.requisicao.application.dto.RequisicaoRequest;
import com.hemorede.exception.AcessoNegadoRecursoException;
import com.hemorede.repository.HospitalRepository;
import com.hemorede.requisicao.infrastructure.repository.RequisicaoRepository;
import com.hemorede.requisicao.application.service.RequisicaoService;
import com.hemorede.expedicao.application.service.RastreamentoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

/**
 * Operações do perfil HOSPITAL. O hospitalId sempre vem do JWT; requisição de outro hospital
 * responde 404, igual a inexistente, para não revelar que ela existe.
 */
@Service
public class PortalHospitalService {

    private final RequisicaoRepository requisicaoRepository;
    private final HospitalRepository hospitalRepository;
    private final RequisicaoService requisicaoService;
    private final RastreamentoService rastreamentoService;

    public PortalHospitalService(RequisicaoRepository requisicaoRepository, HospitalRepository hospitalRepository,
                                 RequisicaoService requisicaoService, RastreamentoService rastreamentoService) {
        this.requisicaoRepository = requisicaoRepository;
        this.hospitalRepository = hospitalRepository;
        this.requisicaoService = requisicaoService;
        this.rastreamentoService = rastreamentoService;
    }

    public Hospital hospital(Long hospitalId) {
        return hospitalRepository.findById(hospitalId)
                .orElseThrow(() -> new AcessoNegadoRecursoException("Hospital não encontrado"));
    }

    public List<Requisicao> listar(Long hospitalId) {
        return requisicaoRepository.findByHospitalSolicitanteIdOrderByDataSolicitacaoDesc(hospitalId);
    }

    public Requisicao buscar(Long hospitalId, Long requisicaoId) {
        Requisicao r = requisicaoRepository.findById(requisicaoId)
                .orElseThrow(() -> new AcessoNegadoRecursoException("Requisição não encontrada: " + requisicaoId));
        if (r.getHospitalSolicitante() == null || !Objects.equals(r.getHospitalSolicitante().getId(), hospitalId)) {
            throw new AcessoNegadoRecursoException("Requisição não encontrada: " + requisicaoId);
        }
        return r;
    }

    @Transactional
    public Requisicao solicitar(Long hospitalId, HospitalRequisicaoRequest request) {
        return requisicaoService.registrar(new RequisicaoRequest(hospitalId, request.prioridade(), request.itens()));
    }

    @Transactional(readOnly = true)
    public RastreamentoResponse rastrear(Long hospitalId, Long requisicaoId) {
        return rastreamentoService.rastrear(buscar(hospitalId, requisicaoId));
    }

    @Transactional
    public Requisicao confirmarRecebimento(Long hospitalId, Long requisicaoId) {
        buscar(hospitalId, requisicaoId);
        return requisicaoService.confirmarEntrega(requisicaoId);
    }
}
