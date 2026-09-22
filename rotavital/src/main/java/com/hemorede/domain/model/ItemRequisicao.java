package com.hemorede.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.TipoSanguineo;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "item_requisicao")
public class ItemRequisicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requisicao_id", nullable = false)
    private Requisicao requisicao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HemoComponente hemoComponente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSanguineo tipoSanguineo;

    @Column(nullable = false)
    private Integer quantidade;

    @ManyToMany
    @JoinTable(
            name = "item_requisicao_bolsa",
            joinColumns = @JoinColumn(name = "item_requisicao_id"),
            inverseJoinColumns = @JoinColumn(name = "bolsa_id")
    )
    private List<Bolsa> bolsasAlocadas = new ArrayList<>();

    public ItemRequisicao() {
    }

    public ItemRequisicao(Long id, Requisicao requisicao, HemoComponente hemoComponente,
                           TipoSanguineo tipoSanguineo, Integer quantidade, List<Bolsa> bolsasAlocadas) {
        this.id = id;
        this.requisicao = requisicao;
        this.hemoComponente = hemoComponente;
        this.tipoSanguineo = tipoSanguineo;
        this.quantidade = quantidade;
        this.bolsasAlocadas = bolsasAlocadas != null ? bolsasAlocadas : new ArrayList<>();
    }

    public boolean isTotalmenteAlocado() {
        return bolsasAlocadas.size() >= quantidade;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Requisicao getRequisicao() {
        return requisicao;
    }

    public void setRequisicao(Requisicao requisicao) {
        this.requisicao = requisicao;
    }

    public HemoComponente getHemoComponente() {
        return hemoComponente;
    }

    public void setHemoComponente(HemoComponente hemoComponente) {
        this.hemoComponente = hemoComponente;
    }

    public TipoSanguineo getTipoSanguineo() {
        return tipoSanguineo;
    }

    public void setTipoSanguineo(TipoSanguineo tipoSanguineo) {
        this.tipoSanguineo = tipoSanguineo;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public List<Bolsa> getBolsasAlocadas() {
        return bolsasAlocadas;
    }

    public void setBolsasAlocadas(List<Bolsa> bolsasAlocadas) {
        this.bolsasAlocadas = bolsasAlocadas;
    }
}
