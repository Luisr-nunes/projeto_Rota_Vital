package com.hemorede.estoque.api;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;

import java.util.List;

/** Caso de uso público do contexto de Estoque para reservar bolsas compatíveis. */
public interface ReservaEstoque {

    List<Bolsa> reservar(HemoComponente componente, TipoSanguineo tipoSolicitado, int quantidade);
}
