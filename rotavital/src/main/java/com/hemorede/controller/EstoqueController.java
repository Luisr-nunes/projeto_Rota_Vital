package com.hemorede.controller;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Estoque;
import com.hemorede.repository.EstoqueRepository;
import com.hemorede.service.EstoqueService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/estoques")
public class EstoqueController {

    private final EstoqueRepository estoqueRepository;
    private final EstoqueService estoqueService;

    public EstoqueController(EstoqueRepository estoqueRepository, EstoqueService estoqueService) {
        this.estoqueRepository = estoqueRepository;
        this.estoqueService = estoqueService;
    }

    @GetMapping
    public List<Estoque> listar() {
        return estoqueRepository.findAll();
    }

    @GetMapping("/{id}/alerta")
    public Map<String, Boolean> verificarAlerta(@PathVariable Long id,
                                                  @RequestParam TipoSanguineo tipoSanguineo,
                                                  @RequestParam HemoComponente hemoComponente) {
        boolean abaixoDoMinimo = estoqueService.estoqueAbaixoDoMinimo(id, tipoSanguineo, hemoComponente);
        return Map.of("abaixoDoMinimo", abaixoDoMinimo);
    }
}
