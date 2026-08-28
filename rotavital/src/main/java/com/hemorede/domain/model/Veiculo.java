package com.hemorede.domain.model;

import com.hemorede.domain.enums.StatusVeiculo;
import com.hemorede.domain.enums.TipoRefrigeracao;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "veiculo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Veiculo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String placa;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoRefrigeracao tipoRefrigeracao;

    @Column(nullable = false)
    private Integer capacidadeCarga;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private StatusVeiculo status = StatusVeiculo.DISPONIVEL;
}
