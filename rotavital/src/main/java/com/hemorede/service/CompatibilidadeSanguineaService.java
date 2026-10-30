package com.hemorede.service;

import com.hemorede.algoritmos.MatrizCompatibilidade;
import com.hemorede.domain.enums.TipoSanguineo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Ponto unico de integracao entre a alocacao e as regras ABO/Rh.
 *
 * Delega para a {@link MatrizCompatibilidade} 8x8 (consulta O(1)). A lista de doadores devolvida
 * por {@link #doadoresCompativeis} ja vem em ordem de preferencia (tipo identico primeiro,
 * O- por ultimo), o que a alocacao usa como desempate quando duas bolsas vencem no mesmo dia.
 */
@Service
public class CompatibilidadeSanguineaService {

    private final MatrizCompatibilidade matriz = new MatrizCompatibilidade();

    public boolean compativel(TipoSanguineo doador, TipoSanguineo receptor) {
        return matriz.compativel(doador, receptor);
    }

    public List<TipoSanguineo> doadoresCompativeis(TipoSanguineo receptor) {
        return matriz.matching(receptor);
    }
}
