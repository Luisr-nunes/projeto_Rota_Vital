package com.hemorede.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "hospital")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Builder.Default
    @OneToMany(mappedBy = "hospital", cascade = CascadeType.ALL)
    private List<Estoque> estoques = new ArrayList<>();

    @Builder.Default
    @OneToMany(mappedBy = "hospitalSolicitante", cascade = CascadeType.ALL)
    private List<Requisicao> requisicoes = new ArrayList<>();
}
