package com.hemorede.domain.model;

import com.hemorede.domain.enums.Prioridade;
import com.hemorede.domain.enums.StatusRequisicao;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "requisicao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Requisicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "hospital_id", nullable = false)
    private Hospital hospitalSolicitante;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime dataSolicitacao = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Prioridade prioridade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusRequisicao status = StatusRequisicao.PENDENTE;

    @Builder.Default
    @OneToMany(mappedBy = "requisicao", cascade = CascadeType.ALL)
    private List<ItemRequisicao> itens = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rota_id")
    private Rota rota;
}
