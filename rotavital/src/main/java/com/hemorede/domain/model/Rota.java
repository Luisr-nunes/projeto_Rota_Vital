package com.hemorede.domain.model;

import com.hemorede.domain.enums.StatusRota;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rota")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Builder.Default
    @OneToMany(mappedBy = "rota")
    private List<Requisicao> requisicoes = new ArrayList<>();

    private LocalDateTime dataSaida;

    private LocalDateTime dataChegadaPrevista;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusRota status = StatusRota.PLANEJADA;
}
