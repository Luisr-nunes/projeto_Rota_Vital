package com.hemorede.domain.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hospital")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class Hospital {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String cnpj;

    private String endereco;

    private Double latitude;

    private Double longitude;

    /**
     * Identificador do nó correspondente a este hospital no grafo de rotas
     * sintético definido em {@code doc/escopo-grafo.md} e construído por
     * {@code DadosSinteticos.criarGrafoRotaVital()} (ex.: "N1".."N5").
     * Usado pelo {@code RoteirizacaoService} para calcular a rota mínima
     * (Dijkstra) do Hemocentro até este hospital.
     */
    @Column(name = "codigo_no", unique = true)
    private String codigoNo;

    @JsonIgnore
    @OneToMany(mappedBy = "hospital", cascade = CascadeType.ALL)
    private List<Estoque> estoques = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "hospitalSolicitante", cascade = CascadeType.ALL)
    private List<Requisicao> requisicoes = new ArrayList<>();

    public Hospital() {
    }

    public Hospital(Long id, String nome, String cnpj, String endereco, Double latitude, Double longitude,
                     String codigoNo, List<Estoque> estoques, List<Requisicao> requisicoes) {
        this.id = id;
        this.nome = nome;
        this.cnpj = cnpj;
        this.endereco = endereco;
        this.latitude = latitude;
        this.longitude = longitude;
        this.codigoNo = codigoNo;
        this.estoques = estoques != null ? estoques : new ArrayList<>();
        this.requisicoes = requisicoes != null ? requisicoes : new ArrayList<>();
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

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getEndereco() {
        return endereco;
    }

    public void setEndereco(String endereco) {
        this.endereco = endereco;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getCodigoNo() {
        return codigoNo;
    }

    public void setCodigoNo(String codigoNo) {
        this.codigoNo = codigoNo;
    }

    public List<Estoque> getEstoques() {
        return estoques;
    }

    public void setEstoques(List<Estoque> estoques) {
        this.estoques = estoques;
    }

    public List<Requisicao> getRequisicoes() {
        return requisicoes;
    }

    public void setRequisicoes(List<Requisicao> requisicoes) {
        this.requisicoes = requisicoes;
    }
}
