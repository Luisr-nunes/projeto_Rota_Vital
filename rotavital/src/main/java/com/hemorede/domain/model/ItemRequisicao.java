package com.hemorede.domain.model;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.TipoSanguineo;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "item_requisicao")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItemRequisicao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requisicao_id", nullable = false)
    private Requisicao requisicao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HemoComponente hemoComponente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoSanguineo tipoSanguineo;

    @Column(nullable = false)
    private Integer quantidade;

    @Builder.Default
    @ManyToMany
    @JoinTable(
            name = "item_requisicao_bolsa",
            joinColumns = @JoinColumn(name = "item_requisicao_id"),
            inverseJoinColumns = @JoinColumn(name = "bolsa_id")
    )
    private List<Bolsa> bolsasAlocadas = new ArrayList<>();

    public boolean isTotalmenteAlocado() {
        return bolsasAlocadas.size() >= quantidade;
    }
}
