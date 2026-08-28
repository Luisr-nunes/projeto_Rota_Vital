package com.hemorede.domain.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "estoque")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Estoque {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospital;

    @Column(nullable = false)
    private Integer capacidadeMaxima;

    @Builder.Default
    @OneToMany(mappedBy = "estoque", cascade = CascadeType.ALL)
    private List<Bolsa> bolsas = new ArrayList<>();
}
