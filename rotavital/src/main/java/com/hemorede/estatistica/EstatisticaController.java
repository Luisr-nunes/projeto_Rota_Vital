package com.hemorede.estatistica;

import com.hemorede.estatistica.EstatisticaDtos.Descritiva;
import com.hemorede.estatistica.EstatisticaDtos.Matriz;
import com.hemorede.estatistica.EstatisticaDtos.Par;
import com.hemorede.estatistica.EstatisticaDtos.Previsao;
import com.hemorede.estatistica.EstatisticaDtos.Variavel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Telas da disciplina de Estatística no painel de gestão (perfil HEMOCENTRO, pela regra
 * geral de /api/** em SegurancaConfig).
 */
@RestController
@RequestMapping("/api/estatistica")
public class EstatisticaController {

    private final EstatisticaService service;

    public EstatisticaController(EstatisticaService service) {
        this.service = service;
    }

    @GetMapping("/variaveis")
    public List<Variavel> variaveis() {
        return service.variaveis();
    }

    @GetMapping("/descritiva")
    public Descritiva descritiva(@RequestParam String variavel) {
        return service.descritiva(variavel);
    }

    @GetMapping("/correlacao")
    public Matriz correlacao(@RequestParam(defaultValue = "diario") String conjunto) {
        return service.matriz(conjunto);
    }

    @GetMapping("/correlacao/par")
    public Par par(@RequestParam String x, @RequestParam String y) {
        return service.par(x, y);
    }

    @GetMapping("/previsao")
    public Previsao previsao(@RequestParam String alvo, @RequestParam(required = false) Double x) {
        return service.previsao(alvo, x);
    }
}
