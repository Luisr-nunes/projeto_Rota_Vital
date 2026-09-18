package com.hemorede.domain.model;

import com.hemorede.domain.enums.Prioridade;
import com.hemorede.domain.enums.StatusRequisicao;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "requisicao")
public class Requisicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospitalSolicitante;

    @Column(nullable = false)
    private LocalDateTime dataSolicitacao = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Prioridade prioridade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusRequisicao status = StatusRequisicao.PENDENTE;

    @OneToMany(mappedBy = "requisicao", cascade = CascadeType.ALL)
    private List<ItemRequisicao> itens = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rota_id")
    private Rota rota;

    public Requisicao() {
    }

    public Requisicao(Long id, Hospital hospitalSolicitante, LocalDateTime dataSolicitacao, Prioridade prioridade,
                       StatusRequisicao status, List<ItemRequisicao> itens, Rota rota) {
        this.id = id;
        this.hospitalSolicitante = hospitalSolicitante;
        this.dataSolicitacao = dataSolicitacao != null ? dataSolicitacao : LocalDateTime.now();
        this.prioridade = prioridade;
        this.status = status != null ? status : StatusRequisicao.PENDENTE;
        this.itens = itens != null ? itens : new ArrayList<>();
        this.rota = rota;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Hospital getHospitalSolicitante() {
        return hospitalSolicitante;
    }

    public void setHospitalSolicitante(Hospital hospitalSolicitante) {
        this.hospitalSolicitante = hospitalSolicitante;
    }

    public LocalDateTime getDataSolicitacao() {
        return dataSolicitacao;
    }

    public void setDataSolicitacao(LocalDateTime dataSolicitacao) {
        this.dataSolicitacao = dataSolicitacao;
    }

    public Prioridade getPrioridade() {
        return prioridade;
    }

    public void setPrioridade(Prioridade prioridade) {
        this.prioridade = prioridade;
    }

    public StatusRequisicao getStatus() {
        return status;
    }

    public void setStatus(StatusRequisicao status) {
        this.status = status;
    }

    public List<ItemRequisicao> getItens() {
        return itens;
    }

    public void setItens(List<ItemRequisicao> itens) {
        this.itens = itens;
    }

    public Rota getRota() {
        return rota;
    }

    public void setRota(Rota rota) {
        this.rota = rota;
    }
}
