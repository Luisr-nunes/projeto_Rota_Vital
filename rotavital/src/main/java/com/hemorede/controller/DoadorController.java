package com.hemorede.controller;

import com.hemorede.domain.model.Doador;
import com.hemorede.service.DoacaoService;
import com.hemorede.domain.regras.RegraElegibilidadeDoador;
import com.hemorede.dto.CorrecaoTipoSanguineoRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * HU01 - Lista os doadores sintéticos para o formulário de registro de doação.
 */
@RestController
@RequestMapping("/api/doadores")
public class DoadorController {

    private final DoacaoService doacaoService;

    public DoadorController(DoacaoService doacaoService) {
        this.doacaoService = doacaoService;
    }

    @GetMapping
    public List<Doador> listar() {
        return doacaoService.listarDoadores();
    }

    /** Situação do doador para doar hoje (RN-04): apto, motivos e próxima data possível. */
    @GetMapping("/{id}/elegibilidade")
    public RegraElegibilidadeDoador.Resultado elegibilidade(@PathVariable Long id) {
        return doacaoService.elegibilidade(id);
    }

    /** Correção do tipo sanguíneo pelo hemocentro (o doador não altera depois da primeira coleta). */
    @PatchMapping("/{id}/tipo-sanguineo")
    public Doador corrigirTipo(@PathVariable Long id, @Valid @RequestBody CorrecaoTipoSanguineoRequest request) {
        return doacaoService.corrigirTipoSanguineo(id, request.tipoSanguineo());
    }
}
