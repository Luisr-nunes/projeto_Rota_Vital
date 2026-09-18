package com.hemorede.controller;

import com.hemorede.algoritmos.GrafoRotas;
import com.hemorede.domain.model.Requisicao;
import com.hemorede.domain.model.Veiculo;
import com.hemorede.dto.CalcularRotaRequest;
import com.hemorede.dto.RotaCalculadaResponse;
import com.hemorede.repository.RequisicaoRepository;
import com.hemorede.service.RoteirizacaoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rotas")
public class RotaController {

    private final RoteirizacaoService roteirizacaoService;
    private final RequisicaoRepository requisicaoRepository;

    @Value("${hemorede.rota.velocidade-media-kmh:60}")
    private double velocidadeMediaKmH;

    public RotaController(RoteirizacaoService roteirizacaoService, RequisicaoRepository requisicaoRepository) {
        this.roteirizacaoService = roteirizacaoService;
        this.requisicaoRepository = requisicaoRepository;
    }

    @GetMapping("/proximas-requisicoes")
    public List<Requisicao> proximasParaRoteirizar() {
        return roteirizacaoService.proximasParaRoteirizar();
    }

    @GetMapping("/veiculo-compativel/{requisicaoId}")
    public Veiculo veiculoCompativel(@PathVariable Long requisicaoId) {
        Requisicao requisicao = requisicaoRepository.findById(requisicaoId)
                .orElseThrow(() -> new IllegalArgumentException("Requisição não encontrada: " + requisicaoId));
        return roteirizacaoService.selecionarVeiculoCompativel(requisicao);
    }

    /**
     * HU05 - Planejar rota de entrega.
     * <p>
     * Aciona o algoritmo de caminho mínimo (Dijkstra manual, {@link GrafoRotas})
     * via {@link RoteirizacaoService#calcularRotaMinima(Requisicao)} para
     * calcular a rota de menor custo do Hemocentro até o hospital solicitante
     * da requisição informada, conforme {@code doc/contratos-api.md}.
     * </p>
     * <p>
     * Se o hospital não tiver rota alcançável no grafo, ou não tiver um nó
     * associado, a exceção correspondente é tratada pelo
     * {@code GlobalExceptionHandler} (HTTP 422).
     * </p>
     */
    @PostMapping("/calcular")
    public ResponseEntity<RotaCalculadaResponse> calcular(@RequestBody CalcularRotaRequest requestBody) {
        Requisicao requisicao = requisicaoRepository.findById(requestBody.requisicaoId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Requisição não encontrada: " + requestBody.requisicaoId()));

        GrafoRotas.ResultadoRota resultado = roteirizacaoService.calcularRotaMinima(requisicao);
        double tempoEstimadoMinutos = (resultado.custoTotal() / velocidadeMediaKmH) * 60.0;

        RotaCalculadaResponse response = new RotaCalculadaResponse(
                requisicao.getId(),
                resultado.caminho(),
                resultado.custoTotal(),
                tempoEstimadoMinutos);

        return ResponseEntity.ok(response);
    }
}
