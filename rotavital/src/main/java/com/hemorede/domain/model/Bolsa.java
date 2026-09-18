package com.hemorede.domain.model;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.TipoSanguineo;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "bolsa")
public class Bolsa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HemoComponente hemoComponente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSanguineo tipoSanguineo;

    @Column(nullable = false)
    private LocalDate dataColeta;

    @Column(nullable = false)
    private LocalDate dataValidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusBolsa status = StatusBolsa.DISPONIVEL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doador_id")
    private Doador doador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estoque_id")
    private Estoque estoque;

    public Bolsa() {
    }

    public Bolsa(Long id, HemoComponente hemoComponente, TipoSanguineo tipoSanguineo, LocalDate dataColeta,
                 LocalDate dataValidade, StatusBolsa status, Doador doador, Estoque estoque) {
        this.id = id;
        this.hemoComponente = hemoComponente;
        this.tipoSanguineo = tipoSanguineo;
        this.dataColeta = dataColeta;
        this.dataValidade = dataValidade;
        this.status = status != null ? status : StatusBolsa.DISPONIVEL;
        this.doador = doador;
        this.estoque = estoque;
    }

    public boolean isVencida() {
        return dataValidade.isBefore(LocalDate.now());
    }

    public boolean isDisponivelParaAlocacao() {
        return status == StatusBolsa.DISPONIVEL && !isVencida();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public LocalDate getDataColeta() {
        return dataColeta;
    }

    public void setDataColeta(LocalDate dataColeta) {
        this.dataColeta = dataColeta;
    }

    public LocalDate getDataValidade() {
        return dataValidade;
    }

    public void setDataValidade(LocalDate dataValidade) {
        this.dataValidade = dataValidade;
    }

    public StatusBolsa getStatus() {
        return status;
    }

    public void setStatus(StatusBolsa status) {
        this.status = status;
    }

    public Doador getDoador() {
        return doador;
    }

    public void setDoador(Doador doador) {
        this.doador = doador;
    }

    public Estoque getEstoque() {
        return estoque;
    }

    public void setEstoque(Estoque estoque) {
        this.estoque = estoque;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;
        private HemoComponente hemoComponente;
        private TipoSanguineo tipoSanguineo;
        private LocalDate dataColeta;
        private LocalDate dataValidade;
        private StatusBolsa status = StatusBolsa.DISPONIVEL;
        private Doador doador;
        private Estoque estoque;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public Builder hemoComponente(HemoComponente hemoComponente) {
            this.hemoComponente = hemoComponente;
            return this;
        }

        public Builder tipoSanguineo(TipoSanguineo tipoSanguineo) {
            this.tipoSanguineo = tipoSanguineo;
            return this;
        }

        public Builder dataColeta(LocalDate dataColeta) {
            this.dataColeta = dataColeta;
            return this;
        }

        public Builder dataValidade(LocalDate dataValidade) {
            this.dataValidade = dataValidade;
            return this;
        }

        public Builder status(StatusBolsa status) {
            this.status = status;
            return this;
        }

        public Builder doador(Doador doador) {
            this.doador = doador;
            return this;
        }

        public Builder estoque(Estoque estoque) {
            this.estoque = estoque;
            return this;
        }

        public Bolsa build() {
            return new Bolsa(id, hemoComponente, tipoSanguineo, dataColeta, dataValidade, status, doador, estoque);
        }
    }
}
