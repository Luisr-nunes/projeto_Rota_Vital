package com.hemorede.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hemorede.domain.enums.StatusRota;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rota")
public class Rota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veiculo_id", nullable = false)
    private Veiculo veiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "motorista_id", nullable = false)
    private Motorista motorista;

    @JsonIgnore
    @OneToMany(mappedBy = "rota")
    private List<Requisicao> requisicoes = new ArrayList<>();

    private LocalDateTime dataSaida;

    private LocalDateTime dataChegadaPrevista;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusRota status = StatusRota.PLANEJADA;

    public Rota() {
    }

    public Rota(Long id, Veiculo veiculo, Motorista motorista, List<Requisicao> requisicoes,
                LocalDateTime dataSaida, LocalDateTime dataChegadaPrevista, StatusRota status) {
        this.id = id;
        this.veiculo = veiculo;
        this.motorista = motorista;
        this.requisicoes = requisicoes != null ? requisicoes : new ArrayList<>();
        this.dataSaida = dataSaida;
        this.dataChegadaPrevista = dataChegadaPrevista;
        this.status = status != null ? status : StatusRota.PLANEJADA;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Veiculo getVeiculo() {
        return veiculo;
    }

    public void setVeiculo(Veiculo veiculo) {
        this.veiculo = veiculo;
    }

    public Motorista getMotorista() {
        return motorista;
    }

    public void setMotorista(Motorista motorista) {
        this.motorista = motorista;
    }

    public List<Requisicao> getRequisicoes() {
        return requisicoes;
    }

    public void setRequisicoes(List<Requisicao> requisicoes) {
        this.requisicoes = requisicoes;
    }

    public LocalDateTime getDataSaida() {
        return dataSaida;
    }

    public void setDataSaida(LocalDateTime dataSaida) {
        this.dataSaida = dataSaida;
    }

    public LocalDateTime getDataChegadaPrevista() {
        return dataChegadaPrevista;
    }

    public void setDataChegadaPrevista(LocalDateTime dataChegadaPrevista) {
        this.dataChegadaPrevista = dataChegadaPrevista;
    }

    public StatusRota getStatus() {
        return status;
    }

    public void setStatus(StatusRota status) {
        this.status = status;
    }
}
