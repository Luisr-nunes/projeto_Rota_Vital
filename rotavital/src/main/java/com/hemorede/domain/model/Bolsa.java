package com.hemorede.domain.model;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.TipoSanguineo;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "bolsa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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
    @Builder.Default
    private StatusBolsa status = StatusBolsa.DISPONIVEL;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doador_id")
    private Doador doador;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "estoque_id")
    private Estoque estoque;

    public boolean isVencida() {
        return dataValidade.isBefore(LocalDate.now());
    }

    public boolean isDisponivelParaAlocacao() {
        return status == StatusBolsa.DISPONIVEL && !isVencida();
    }
}
