package com.hemorede.service;

import com.hemorede.domain.enums.TipoSanguineo;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Ponto unico de integracao entre a alocacao e as regras ABO/Rh.
 *
 * Enquanto a matriz da PI2-165 nao esta integrada, este adaptador delega para a regra
 * ja existente em {@link TipoSanguineo}. Quando MatrizCompatibilidade chegar,
 * somente esta classe precisara ser ajustada para usa-la.
 */
@Service
public class CompatibilidadeSanguineaService {

    public boolean compativel(TipoSanguineo doador, TipoSanguineo receptor) {
        if (doador == null || receptor == null) {
            return false;
        }
        return TipoSanguineo.compativel(doador, receptor);
    }

    public List<TipoSanguineo> doadoresCompativeis(TipoSanguineo receptor) {
        if (receptor == null) {
            return List.of();
        }
        return TipoSanguineo.doadoresCompativeis(receptor);
    }
}
