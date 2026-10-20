package com.hemorede.service;

import com.hemorede.algoritmos.FilaFEFO;
import com.hemorede.algoritmos.GrafoRotas;
import com.hemorede.algoritmos.IndiceEstoque;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.StatusRequisicao;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.domain.model.ItemRequisicao;
import com.hemorede.domain.model.Requisicao;
import com.hemorede.dto.AlocacaoResponse;
import com.hemorede.exception.EstoqueCompativelInsuficienteException;
import com.hemorede.exception.NenhumaBolsaCompativelException;
import com.hemorede.repository.BolsaRepository;
import com.hemorede.repository.RequisicaoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Integra compatibilidade ABO/Rh, FEFO e Dijkstra na alocacao da HU08. */
@Service
public class AlocacaoService {

    private final BolsaRepository bolsaRepository;
    private final RequisicaoRepository requisicaoRepository;
    private final RoteirizacaoService roteirizacaoService;
    private final CompatibilidadeSanguineaService compatibilidadeSanguineaService;

    @Autowired
    public AlocacaoService(
            BolsaRepository bolsaRepository,
            RequisicaoRepository requisicaoRepository,
            RoteirizacaoService roteirizacaoService,
            CompatibilidadeSanguineaService compatibilidadeSanguineaService) {
        this.bolsaRepository = bolsaRepository;
        this.requisicaoRepository = requisicaoRepository;
        this.roteirizacaoService = roteirizacaoService;
        this.compatibilidadeSanguineaService = compatibilidadeSanguineaService;
    }

    /** Mantem compatibilidade com o uso legado que aloca apenas um item. */
    public AlocacaoService(BolsaRepository bolsaRepository) {
        this(bolsaRepository, null, null, new CompatibilidadeSanguineaService());
    }

    @Transactional
    public AlocacaoResponse alocarRequisicao(Long requisicaoId) {
        if (requisicaoRepository == null || roteirizacaoService == null) {
            throw new IllegalStateException("Alocacao de requisicao exige os servicos integrados");
        }

        Requisicao requisicao = requisicaoRepository.findById(requisicaoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Requisicao nao encontrada: " + requisicaoId));

        if (requisicao.getStatus() != StatusRequisicao.PENDENTE) {
            throw new IllegalStateException(
                    "A requisicao deve estar PENDENTE para ser alocada: " + requisicaoId);
        }
        if (requisicao.getItens() == null || requisicao.getItens().isEmpty()) {
            throw new IllegalStateException("A requisicao nao possui itens para alocar: " + requisicaoId);
        }

        // Calcula antes de reservar. Se nao houver rota, nenhuma bolsa e alterada.
        GrafoRotas.ResultadoRota rota = roteirizacaoService.calcularRotaMinima(requisicao);

        for (ItemRequisicao item : requisicao.getItens()) {
            alocar(item);
        }

        requisicao.setStatus(StatusRequisicao.ALOCADA);
        requisicaoRepository.save(requisicao);

        List<AlocacaoResponse.BolsaAlocada> bolsas = requisicao.getItens().stream()
                .flatMap(item -> item.getBolsasAlocadas().stream())
                .map(bolsa -> new AlocacaoResponse.BolsaAlocada(
                        bolsa.getId(),
                        bolsa.getTipoSanguineo(),
                        bolsa.getHemoComponente(),
                        bolsa.getDataValidade()))
                .toList();

        return new AlocacaoResponse(
                requisicao.getId(),
                requisicao.getStatus(),
                bolsas,
                rota.caminho(),
                rota.custoTotal());
    }

    @Transactional
    public void alocar(ItemRequisicao item) {
        validarItem(item);

        if (item.getBolsasAlocadas() == null) {
            item.setBolsasAlocadas(new ArrayList<>());
        }

        int necessario = item.getQuantidade() - item.getBolsasAlocadas().size();
        if (necessario <= 0) {
            return;
        }

        List<Bolsa> candidatasCompativeis = buscarCandidatasValidas(item);
        if (candidatasCompativeis.isEmpty()) {
            throw new NenhumaBolsaCompativelException("Nenhuma bolsa compatível disponível");
        }
        if (candidatasCompativeis.size() < necessario) {
            throw new EstoqueCompativelInsuficienteException(
                    necessario, candidatasCompativeis.size());
        }

        IndiceEstoque indice = construirIndiceEstoque(candidatasCompativeis);
        List<TipoSanguineo> tiposCompativeis = compatibilidadeSanguineaService
                .doadoresCompativeis(item.getTipoSanguineo());

        List<Bolsa> selecionadas = new ArrayList<>();
        for (int i = 0; i < necessario; i++) {
            Bolsa bolsa = alocarProximaCompativel(indice, tiposCompativeis);
            if (bolsa == null) {
                throw new EstoqueCompativelInsuficienteException(necessario, selecionadas.size());
            }
            bolsa.setStatus(StatusBolsa.RESERVADA);
            selecionadas.add(bolsa);
        }

        bolsaRepository.saveAll(selecionadas);
        item.getBolsasAlocadas().addAll(selecionadas);
    }

    private void validarItem(ItemRequisicao item) {
        if (item == null || item.getHemoComponente() == null
                || item.getTipoSanguineo() == null || item.getQuantidade() == null
                || item.getQuantidade() <= 0) {
            throw new IllegalStateException("Item de requisicao invalido para alocacao");
        }
    }

    private List<Bolsa> buscarCandidatasValidas(ItemRequisicao item) {
        LocalDate hoje = LocalDate.now();
        return bolsaRepository
                .findByHemoComponenteAndStatus(item.getHemoComponente(), StatusBolsa.DISPONIVEL)
                .stream()
                .filter(bolsa -> bolsa.getDataValidade() != null)
                .filter(bolsa -> !bolsa.getDataValidade().isBefore(hoje))
                .filter(bolsa -> compatibilidadeSanguineaService.compativel(
                        bolsa.getTipoSanguineo(), item.getTipoSanguineo()))
                .toList();
    }

    private IndiceEstoque construirIndiceEstoque(List<Bolsa> bolsas) {
        IndiceEstoque indice = new IndiceEstoque();
        bolsas.forEach(indice::adicionarBolsa);
        return indice;
    }

    private Bolsa alocarProximaCompativel(
            IndiceEstoque indice,
            List<TipoSanguineo> tiposCompativeis) {
        TipoSanguineo melhorTipo = null;
        Bolsa melhorBolsa = null;

        for (TipoSanguineo doador : tiposCompativeis) {
            FilaFEFO fila = indice.consultarEstoque(doador);
            Bolsa topo = fila.peek();
            if (topo != null && (melhorBolsa == null
                    || topo.getDataValidade().isBefore(melhorBolsa.getDataValidade()))) {
                melhorBolsa = topo;
                melhorTipo = doador;
            }
        }

        return melhorTipo != null ? indice.alocarBolsa(melhorTipo) : null;
    }
}
