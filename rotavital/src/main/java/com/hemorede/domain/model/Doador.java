package com.hemorede.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hemorede.domain.enums.Sexo;
import com.hemorede.domain.enums.TipoSanguineo;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "doador")
public class Doador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String cpf;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSanguineo tipoSanguineo;

    private LocalDate dataUltimaDoacao;

    /**
     * Indica se o doador pode participar de novas doações.
     * O valor padrão preserva o comportamento dos cadastros existentes.
     */
    @Column(nullable = false)
    private boolean ativo = true;

    /** Dados básicos informados pelo próprio doador (perfil DOADOR); opcionais nos cadastros legados. */
    @Enumerated(EnumType.STRING)
    private Sexo sexo;

    private LocalDate dataNascimento;

    private Double pesoKg;

    private String telefone;

    @JsonIgnore
    @OneToMany(mappedBy = "doador", cascade = CascadeType.ALL)
    private List<Bolsa> bolsas = new ArrayList<>();

    public Doador() {
    }

    public Doador(Long id, String nome, String cpf, TipoSanguineo tipoSanguineo,
                  LocalDate dataUltimaDoacao, List<Bolsa> bolsas) {
        this.id = id;
        this.nome = nome;
        this.cpf = cpf;
        this.tipoSanguineo = tipoSanguineo;
        this.dataUltimaDoacao = dataUltimaDoacao;
        this.bolsas = bolsas != null ? bolsas : new ArrayList<>();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public TipoSanguineo getTipoSanguineo() {
        return tipoSanguineo;
    }

    public void setTipoSanguineo(TipoSanguineo tipoSanguineo) {
        this.tipoSanguineo = tipoSanguineo;
    }

    public LocalDate getDataUltimaDoacao() {
        return dataUltimaDoacao;
    }

    public void setDataUltimaDoacao(LocalDate dataUltimaDoacao) {
        this.dataUltimaDoacao = dataUltimaDoacao;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public List<Bolsa> getBolsas() {
        return bolsas;
    }

    public void setBolsas(List<Bolsa> bolsas) {
        this.bolsas = bolsas;
    }

    public Sexo getSexo() {
        return sexo;
    }

    public void setSexo(Sexo sexo) {
        this.sexo = sexo;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public Double getPesoKg() {
        return pesoKg;
    }

    public void setPesoKg(Double pesoKg) {
        this.pesoKg = pesoKg;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }
}
