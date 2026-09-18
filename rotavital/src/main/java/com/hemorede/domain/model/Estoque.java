package com.hemorede.domain.model;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "estoque")
public class Estoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Column(nullable = false)
    private Integer capacidadeMaxima;

    @OneToMany(mappedBy = "estoque", cascade = CascadeType.ALL)
    private List<Bolsa> bolsas = new ArrayList<>();

    public Estoque() {
    }

    public Estoque(Long id, Hospital hospital, Integer capacidadeMaxima, List<Bolsa> bolsas) {
        this.id = id;
        this.hospital = hospital;
        this.capacidadeMaxima = capacidadeMaxima;
        this.bolsas = bolsas != null ? bolsas : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Hospital getHospital() {
        return hospital;
    }

    public void setHospital(Hospital hospital) {
        this.hospital = hospital;
    }

    public Integer getCapacidadeMaxima() {
        return capacidadeMaxima;
    }

    public void setCapacidadeMaxima(Integer capacidadeMaxima) {
        this.capacidadeMaxima = capacidadeMaxima;
    }

    public List<Bolsa> getBolsas() {
        return bolsas;
    }

    public void setBolsas(List<Bolsa> bolsas) {
        this.bolsas = bolsas;
    }
}
