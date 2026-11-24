package com.hemorede.estoque.application.service;

import com.hemorede.algoritmos.FilaFEFO;
import com.hemorede.algoritmos.IndiceEstoque;
import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.regras.RegraFEFO;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.estoque.api.ReservaEstoque;
import com.hemorede.exception.EstoqueCompativelInsuficienteException;
import com.hemorede.exception.NenhumaBolsaCompativelException;
import com.hemorede.repository.BolsaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Implementa a reserva de estoque com compatibilidade sanguínea e FEFO. */
@Service
public class ReservaEstoqueService implements ReservaEstoque {

    private final BolsaRepository bolsaRepository;
    private final CompatibilidadeSanguineaService compatibilidadeSanguineaService;

    public ReservaEstoqueService(BolsaRepository bolsaRepository,
                                 CompatibilidadeSanguineaService compatibilidadeSanguineaService) {
        this.bolsaRepository = bolsaRepository;
        this.compatibilidadeSanguineaService = compatibilidadeSanguineaService;
    }

    @Override
    @Transactional
    public List<Bolsa> reservar(HemoComponente componente, TipoSanguineo tipoSolicitado, int quantidade) {
        if (componente == null || tipoSolicitado == null || quantidade <= 0) {
            throw new IllegalArgumentException("Componente, tipo sanguíneo e quantidade válida são obrigatórios");
        }

        List<Bolsa> candidatas = bolsaRepository
                .findByHemoComponenteAndStatus(componente, StatusBolsa.DISPONIVEL)
                .stream()
                .filter(bolsa -> bolsa.getDataValidade() != null)
                .filter(bolsa -> !bolsa.getDataValidade().isBefore(LocalDate.now()))
                .filter(bolsa -> compatibilidadeSanguineaService.compativel(
                        bolsa.getTipoSanguineo(), tipoSolicitado))
                .toList();

        if (candidatas.isEmpty()) {
            throw new NenhumaBolsaCompativelException("Nenhuma bolsa compatível disponível");
        }
        if (candidatas.size() < quantidade) {
            throw new EstoqueCompativelInsuficienteException(quantidade, candidatas.size());
        }

        IndiceEstoque indice = new IndiceEstoque();
        candidatas.forEach(indice::adicionarBolsa);
        List<TipoSanguineo> tiposCompativeis = compatibilidadeSanguineaService.doadoresCompativeis(tipoSolicitado);

        List<Bolsa> selecionadas = new ArrayList<>();
        for (int i = 0; i < quantidade; i++) {
            Bolsa proxima = proximaPorFefo(indice, tiposCompativeis, tipoSolicitado);
            if (proxima == null) {
                throw new EstoqueCompativelInsuficienteException(quantidade, selecionadas.size());
            }
            proxima.setStatus(StatusBolsa.RESERVADA);
            selecionadas.add(proxima);
        }

        java.util.Set<Bolsa> saiu = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        saiu.addAll(selecionadas);
        List<LocalDate> ficaram = candidatas.stream()
                .filter(b -> !saiu.contains(b))
                .map(Bolsa::getDataValidade)
                .toList();
        if (!RegraFEFO.respeitaFEFO(selecionadas.stream().map(Bolsa::getDataValidade).toList(), ficaram)) {
            throw new IllegalStateException("Seleção de bolsas violaria o FEFO; reserva abortada");
        }

        bolsaRepository.saveAll(selecionadas);
        return List.copyOf(selecionadas);
    }

    private Bolsa proximaPorFefo(IndiceEstoque indice, List<TipoSanguineo> tiposCompativeis, TipoSanguineo indiceTipoPedido) {
        TipoSanguineo tipoSelecionado = null;
        Bolsa bolsaSelecionada = null;

        for (TipoSanguineo tipoDoador : tiposCompativeis) {
            FilaFEFO fila = indice.consultarEstoque(tipoDoador);
            Bolsa candidata = fila.peek();
            if (candidata != null && (bolsaSelecionada == null || RegraFEFO.comparar(
                    candidata.getDataValidade(), candidata.getTipoSanguineo(), candidata.getDataColeta(),
                    bolsaSelecionada.getDataValidade(), bolsaSelecionada.getTipoSanguineo(),
                    bolsaSelecionada.getDataColeta(), indiceTipoPedido) < 0)) {
                tipoSelecionado = tipoDoador;
                bolsaSelecionada = candidata;
            }
        }
        return tipoSelecionado == null ? null : indice.alocarBolsa(tipoSelecionado);
    }
}
