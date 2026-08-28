package com.hemorede.domain.model;

import com.hemorede.domain.enums.TipoSanguineo;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "doador")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
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

    @Builder.Default
    @OneToMany(mappedBy = "doador", cascade = CascadeType.ALL)
    private List<Bolsa> bolsas = new ArrayList<>();
}
