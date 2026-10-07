package com.hemorede.service;

import com.hemorede.algoritmos.FilaFEFO;
import com.hemorede.algoritmos.IndiceEstoque;
import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.repository.BolsaRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Regra de negócio 7: estoque mínimo de segurança.
 * Verifica se a quantidade disponível de um tipo sanguíneo/componente
 * em um estoque está abaixo do limite mínimo configurado.
 */
@Service
public class EstoqueService {

    private final BolsaRepository bolsaRepository;

    public EstoqueService(BolsaRepository bolsaRepository) {
        this.bolsaRepository = bolsaRepository;
    }

    @Value("${hemorede.estoque.limite-minimo-padrao:5}")
    private int limiteMinimoPadrao;

     /**
     * Monta um {@link IndiceEstoque} em memória com as bolsas disponíveis de
     * um hemocomponente em um estoque específico, pronto para consulta por
     * tipo sanguíneo (hash, O(1) amortizado) e priorização por validade (FEFO).
     */
    public IndiceEstoque construirIndiceEstoque(Long estoqueId, HemoComponente hemoComponente) {
        IndiceEstoque indice = new IndiceEstoque();
        bolsaRepository
                .findByEstoqueIdAndHemoComponenteAndStatus(estoqueId, hemoComponente, StatusBolsa.DISPONIVEL)
                .forEach(indice::adicionarBolsa);
        return indice;
    }

     /**
     * Total de bolsas disponíveis de um tipo sanguíneo/componente em um
     * estoque, via {@link IndiceEstoque#totalDisponivelPorTipo(TipoSanguineo)}.
     */
    public int totalDisponivelPorTipo(Long estoqueId, TipoSanguineo tipoSanguineo, HemoComponente hemoComponente) {
        return construirIndiceEstoque(estoqueId, hemoComponente).totalDisponivelPorTipo(tipoSanguineo);
    }


    public boolean estoqueAbaixoDoMinimo(Long estoqueId, TipoSanguineo tipoSanguineo,
                                          HemoComponente hemoComponente) {
        long quantidadeDisponivel = bolsaRepository
                .countByEstoqueIdAndTipoSanguineoAndHemoComponenteAndStatus(
                        estoqueId, tipoSanguineo, hemoComponente, StatusBolsa.DISPONIVEL);

        return quantidadeDisponivel < limiteMinimoPadrao;
    }

    /**
     * Bolsas de um tipo sanguíneo/componente em um estoque que vencem em até
     * {@code diasLimite} dias, via {@link FilaFEFO#proximasAoVencimento(int)},
     * sem removê-las da fila. Apoia o painel gerencial (HU07) e a rotina de
     * alerta de vencimento descrita em {@code doc/escopo-grafo.md}.
     */
    public List<Bolsa> bolsasProximasAoVencimento(Long estoqueId, HemoComponente hemoComponente,
                                                   TipoSanguineo tipoSanguineo, int diasLimite) {
        FilaFEFO fila = construirIndiceEstoque(estoqueId, hemoComponente).consultarEstoque(tipoSanguineo);
        return fila.proximasAoVencimento(diasLimite);
    }
}
