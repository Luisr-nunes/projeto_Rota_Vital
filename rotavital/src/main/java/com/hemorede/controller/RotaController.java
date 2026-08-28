package com.hemorede.controller;

import com.hemorede.domain.model.Requisicao;
import com.hemorede.domain.model.Veiculo;
import com.hemorede.repository.RequisicaoRepository;
import com.hemorede.service.RoteirizacaoService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/rotas")
@RequiredArgsConstructor
public class RotaController {

    private final RoteirizacaoService roteirizacaoService;
    private final RequisicaoRepository requisicaoRepository;

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
}
