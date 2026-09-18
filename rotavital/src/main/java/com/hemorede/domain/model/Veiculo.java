package com.hemorede.domain.model;

import com.hemorede.domain.enums.StatusVeiculo;
import com.hemorede.domain.enums.TipoRefrigeracao;
import jakarta.persistence.*;

@Entity
@Table(name = "veiculo")
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String placa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoRefrigeracao tipoRefrigeracao;

    @Column(nullable = false)
    private Integer capacidadeCarga;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusVeiculo status = StatusVeiculo.DISPONIVEL;

    public Veiculo() {
    }

    public Veiculo(Long id, String placa, TipoRefrigeracao tipoRefrigeracao,
                   Integer capacidadeCarga, StatusVeiculo status) {
        this.id = id;
        this.placa = placa;
        this.tipoRefrigeracao = tipoRefrigeracao;
        this.capacidadeCarga = capacidadeCarga;
        this.status = status != null ? status : StatusVeiculo.DISPONIVEL;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPlaca() {
        return placa;
    }

    public void setPlaca(String placa) {
        this.placa = placa;
    }

    public TipoRefrigeracao getTipoRefrigeracao() {
        return tipoRefrigeracao;
    }

    public void setTipoRefrigeracao(TipoRefrigeracao tipoRefrigeracao) {
        this.tipoRefrigeracao = tipoRefrigeracao;
    }

    public Integer getCapacidadeCarga() {
        return capacidadeCarga;
    }

    public void setCapacidadeCarga(Integer capacidadeCarga) {
        this.capacidadeCarga = capacidadeCarga;
    }

    public StatusVeiculo getStatus() {
        return status;
    }

    public void setStatus(StatusVeiculo status) {
        this.status = status;
    }
}
