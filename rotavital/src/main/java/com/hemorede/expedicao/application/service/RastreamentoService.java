package com.hemorede.expedicao.application.service;

import com.hemorede.service.TelemetriaService;

import com.hemorede.algoritmos.DadosSinteticos;
import com.hemorede.expedicao.domain.algorithm.GrafoRotas;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.requisicao.domain.enums.StatusRequisicao;
import com.hemorede.expedicao.domain.enums.StatusRota;
import com.hemorede.expedicao.domain.enums.StatusVeiculo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.domain.model.Hospital;
import com.hemorede.requisicao.domain.model.ItemRequisicao;
import com.hemorede.expedicao.domain.model.Motorista;
import com.hemorede.requisicao.domain.model.Requisicao;
import com.hemorede.expedicao.domain.model.Rota;
import com.hemorede.expedicao.domain.model.Veiculo;
import com.hemorede.domain.regras.RegraProgressoEntrega;
import com.hemorede.domain.regras.RegraRefrigeracao;
import com.hemorede.domain.regras.RegraTransicaoRequisicao;
import com.hemorede.dto.LeituraTemperaturaResponse;
import com.hemorede.expedicao.application.dto.RastreamentoResponse;
import com.hemorede.exception.TransicaoStatusInvalidaException;
import com.hemorede.exception.VeiculoIncompativelException;
import com.hemorede.repository.HospitalRepository;
import com.hemorede.expedicao.infrastructure.repository.MotoristaRepository;
import com.hemorede.requisicao.infrastructure.repository.RequisicaoRepository;
import com.hemorede.expedicao.infrastructure.repository.RotaRepository;
import com.hemorede.expedicao.infrastructure.repository.VeiculoRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Despacho e acompanhamento da entrega (HU05/HU06 estendidas).
 * <ul>
 *   <li>Despachar: requisição ALOCADA/APROVADA vira EM_ROTA, bolsas viram EM_TRANSITO, o veículo
 *       compatível (RN-06) vira EM_ROTA e uma {@link Rota} guarda saída e chegada prevista.</li>
 *   <li>Rastrear: trajeto de Dijkstra com coordenadas e posição estimada pelo tempo decorrido
 *       (RN-10), mais a última temperatura do veículo.</li>
 * </ul>
 */
@Service
public class RastreamentoService {

    private final RequisicaoRepository requisicaoRepository;
    private final VeiculoRepository veiculoRepository;
    private final MotoristaRepository motoristaRepository;
    private final RotaRepository rotaRepository;
    private final HospitalRepository hospitalRepository;
    private final RoteirizacaoService roteirizacaoService;
    private final GrafoRotas grafoRotas;
    private final TelemetriaService telemetriaService;

    @Value("${hemorede.rota.velocidade-media-kmh:60}")
    private double velocidadeMediaKmH = 60;

    @Value("${hemorede.hemocentro.latitude:-8.0476}")
    private double hemocentroLat = -8.0476;

    @Value("${hemorede.hemocentro.longitude:-34.8990}")
    private double hemocentroLng = -34.8990;

    /** Acelera o relógio da simulação de trajeto (1 = tempo real; 10 = 10x mais rápido na demonstração). */
    @Value("${hemorede.rastreamento.fator-tempo:1}")
    private double fatorTempo = 1;

    public RastreamentoService(RequisicaoRepository requisicaoRepository, VeiculoRepository veiculoRepository,
                               MotoristaRepository motoristaRepository, RotaRepository rotaRepository,
                               HospitalRepository hospitalRepository, RoteirizacaoService roteirizacaoService,
                               GrafoRotas grafoRotas, TelemetriaService telemetriaService) {
        this.requisicaoRepository = requisicaoRepository;
        this.veiculoRepository = veiculoRepository;
        this.motoristaRepository = motoristaRepository;
        this.rotaRepository = rotaRepository;
        this.hospitalRepository = hospitalRepository;
        this.roteirizacaoService = roteirizacaoService;
        this.grafoRotas = grafoRotas;
        this.telemetriaService = telemetriaService;
    }

    @Transactional
    public RastreamentoResponse despachar(Long requisicaoId) {
        Requisicao requisicao = buscar(requisicaoId);
        if (!RegraTransicaoRequisicao.permitida(requisicao.getStatus(), StatusRequisicao.EM_ROTA)) {
            throw new TransicaoStatusInvalidaException("Requisição " + requisicaoId + " está "
                    + requisicao.getStatus() + "; só requisição alocada pode sair para entrega");
        }
        List<com.hemorede.domain.enums.HemoComponente> componentes = requisicao.getItens().stream()
                .map(ItemRequisicao::getHemoComponente).toList();
        if (!RegraRefrigeracao.despachoUnicoPossivel(componentes)) {
            throw new VeiculoIncompativelException("A requisição " + requisicaoId
                    + " mistura faixas de refrigeração diferentes e precisa ser dividida em mais de um despacho.");
        }

        GrafoRotas.ResultadoRota rotaMinima = roteirizacaoService.calcularRotaMinima(requisicao);
        Veiculo veiculo = roteirizacaoService.selecionarVeiculoCompativel(requisicao);
        Motorista motorista = motoristaRepository.findAll().stream().findFirst()
                .orElseThrow(() -> new VeiculoIncompativelException("Nenhum motorista cadastrado para a entrega."));

        LocalDateTime agora = LocalDateTime.now();
        long segundos = Math.round(rotaMinima.custoTotal() / velocidadeMediaKmH * 3600);
        Rota rota = new Rota();
        rota.setVeiculo(veiculo);
        rota.setMotorista(motorista);
        rota.setStatus(StatusRota.EM_ANDAMENTO);
        rota.setDataSaida(agora);
        rota.setDataChegadaPrevista(agora.plusSeconds(Math.max(1, Math.round(segundos / fatorTempo))));
        rotaRepository.save(rota);

        veiculo.setStatus(StatusVeiculo.EM_ROTA);
        veiculoRepository.save(veiculo);

        for (ItemRequisicao item : requisicao.getItens()) {
            for (Bolsa bolsa : item.getBolsasAlocadas()) {
                if (bolsa.getStatus() == StatusBolsa.RESERVADA) {
                    bolsa.setStatus(StatusBolsa.EM_TRANSITO);
                }
            }
        }
        requisicao.setRota(rota);
        requisicao.setStatus(StatusRequisicao.EM_ROTA);
        requisicaoRepository.save(requisicao);
        return montar(requisicao, rotaMinima);
    }

    @Transactional(readOnly = true)
    public RastreamentoResponse rastrear(Long requisicaoId) {
        return rastrear(buscar(requisicaoId));
    }

    @Transactional(readOnly = true)
    public RastreamentoResponse rastrear(Requisicao requisicao) {
        return montar(requisicao, roteirizacaoService.calcularRotaMinima(requisicao));
    }

    /** Encerra a rota e libera o veículo quando a entrega é confirmada. */
    @Transactional
    public void encerrarRota(Requisicao requisicao) {
        Rota rota = requisicao.getRota();
        if (rota == null || rota.getStatus() == StatusRota.CONCLUIDA) {
            return;
        }
        rota.setStatus(StatusRota.CONCLUIDA);
        Veiculo veiculo = rota.getVeiculo();
        if (veiculo != null && veiculo.getStatus() == StatusVeiculo.EM_ROTA) {
            veiculo.setStatus(StatusVeiculo.DISPONIVEL);
            veiculoRepository.save(veiculo);
        }
        rotaRepository.save(rota);
    }

    private Requisicao buscar(Long id) {
        return requisicaoRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Requisição não encontrada: " + id));
    }

    private RastreamentoResponse montar(Requisicao requisicao, GrafoRotas.ResultadoRota rotaMinima) {
        Map<String, Hospital> porNo = hospitalRepository.findAll().stream()
                .filter(h -> h.getCodigoNo() != null)
                .collect(Collectors.toMap(Hospital::getCodigoNo, h -> h, (a, b) -> a));

        List<RastreamentoResponse.PontoTrajeto> trajeto = new ArrayList<>();
        List<RegraProgressoEntrega.Ponto> pontos = new ArrayList<>();
        for (String no : rotaMinima.caminho()) {
            RastreamentoResponse.PontoTrajeto p = ponto(no, porNo.get(no));
            trajeto.add(p);
            pontos.add(new RegraProgressoEntrega.Ponto(p.lat(), p.lng()));
        }
        List<Double> pesos = new ArrayList<>();
        for (int i = 0; i + 1 < rotaMinima.caminho().size(); i++) {
            pesos.add(grafoRotas.getVizinhos(rotaMinima.caminho().get(i)).get(rotaMinima.caminho().get(i + 1)));
        }

        Rota rota = requisicao.getRota();
        double fracao;
        LocalDateTime saida = null;
        LocalDateTime previsao = null;
        Long minutosRestantes = null;
        switch (requisicao.getStatus()) {
            case EM_ROTA -> {
                saida = rota != null ? rota.getDataSaida() : null;
                previsao = rota != null ? rota.getDataChegadaPrevista() : null;
                if (saida != null && previsao != null) {
                    LocalDateTime agora = LocalDateTime.now();
                    fracao = RegraProgressoEntrega.fracaoDecorrida(
                            Duration.between(saida, agora).getSeconds(), Duration.between(saida, previsao).getSeconds());
                    minutosRestantes = Math.max(0, Duration.between(agora, previsao).toMinutes());
                } else {
                    fracao = 0;
                }
            }
            case ENTREGUE -> {
                fracao = 1;
                if (rota != null) {
                    saida = rota.getDataSaida();
                    previsao = rota.getDataChegadaPrevista();
                }
                minutosRestantes = 0L;
            }
            default -> fracao = 0;
        }
        RegraProgressoEntrega.Posicao posicao = RegraProgressoEntrega.interpolar(pontos, pesos, fracao);

        String placa = null;
        Double temperatura = null;
        String statusTemp = null;
        if (rota != null && rota.getVeiculo() != null) {
            placa = rota.getVeiculo().getPlaca();
            if (requisicao.getStatus() == StatusRequisicao.EM_ROTA) {
                try {
                    LeituraTemperaturaResponse leitura = telemetriaService.lerTemperatura(rota.getVeiculo().getId());
                    temperatura = leitura.temperaturaC();
                    statusTemp = leitura.status();
                } catch (RuntimeException semLeitura) {
                    // veículo ainda sem leitura de telemetria: o painel mostra "sem leitura"
                }
            }
        }

        Hospital destino = requisicao.getHospitalSolicitante();
        return new RastreamentoResponse(
                requisicao.getId(),
                requisicao.getStatus(),
                destino != null ? destino.getNome() : null,
                trajeto,
                rotaMinima.custoTotal(),
                new RastreamentoResponse.Posicao(posicao.ponto().lat(), posicao.ponto().lng()),
                requisicao.getStatus() == StatusRequisicao.EM_ROTA ? posicao.trechoAtual() : null,
                posicao.percentual() * (fracao == 0 ? 0 : 1),
                saida,
                previsao,
                minutosRestantes,
                placa,
                temperatura,
                statusTemp,
                googleMaps(trajeto));
    }

    private RastreamentoResponse.PontoTrajeto ponto(String no, Hospital hospital) {
        if (DadosSinteticos.N0_HEMOCENTRO.equals(no)) {
            return new RastreamentoResponse.PontoTrajeto(no, "Hemocentro", hemocentroLat, hemocentroLng);
        }
        if (hospital == null || hospital.getLatitude() == null || hospital.getLongitude() == null) {
            throw new IllegalStateException("Nó " + no + " sem coordenadas cadastradas");
        }
        return new RastreamentoResponse.PontoTrajeto(no, hospital.getNome(), hospital.getLatitude(), hospital.getLongitude());
    }

    /** Rota de carro no Google Maps (sem chave de API): origem, paradas intermediárias e destino. */
    static String googleMaps(List<RastreamentoResponse.PontoTrajeto> trajeto) {
        if (trajeto.size() < 2) {
            return null;
        }
        RastreamentoResponse.PontoTrajeto o = trajeto.get(0);
        RastreamentoResponse.PontoTrajeto d = trajeto.get(trajeto.size() - 1);
        StringBuilder url = new StringBuilder("https://www.google.com/maps/dir/?api=1&travelmode=driving")
                .append("&origin=").append(o.lat()).append(',').append(o.lng())
                .append("&destination=").append(d.lat()).append(',').append(d.lng());
        if (trajeto.size() > 2) {
            url.append("&waypoints=").append(trajeto.subList(1, trajeto.size() - 1).stream()
                    .map(p -> p.lat() + "," + p.lng()).collect(Collectors.joining("%7C")));
        }
        return url.toString();
    }
}
