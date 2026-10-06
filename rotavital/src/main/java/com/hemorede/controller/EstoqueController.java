package com.hemorede.controller;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.domain.model.Estoque;
import com.hemorede.dto.BolsaDisponivelResponse;
import com.hemorede.dto.ConsultaEstoqueResponse;
import com.hemorede.repository.EstoqueRepository;
import com.hemorede.service.EstoqueService;
import org.springframework.http.ResponseEntity;
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

    /**
     * HU02 - Consultar estoque.
     * <p>
     * Lista as bolsas {@code DISPONIVEL} e não vencidas do tipo sanguíneo e
     * hemocomponente informados, ordenadas da validade mais próxima para a
     * mais distante ({@link EstoqueService#consultarBolsasDisponiveis},
     * que combina {@code IndiceEstoque} + {@code FilaFEFO}).
     * </p>
     * <p>
     * Cenário 2 da HU02: quando não há bolsas para o filtro, retorna 200
     * com lista vazia e {@code mensagem: "nenhuma bolsa encontrada"}.
     * Um {@code tipo} ou {@code componente} inválido (fora do enum) resulta
     * em 400, tratado pelo {@code GlobalExceptionHandler}.
     * </p>
     */
    @GetMapping("/bolsas")
    public ResponseEntity<ConsultaEstoqueResponse> consultarBolsas(@RequestParam TipoSanguineo tipo,
                                                                     @RequestParam HemoComponente componente) {
        List<Bolsa> bolsas = estoqueService.consultarBolsasDisponiveis(tipo, componente);
        List<BolsaDisponivelResponse> itens = bolsas.stream()
                .map(BolsaDisponivelResponse::fromEntidade)
                .toList();
        return ResponseEntity.ok(ConsultaEstoqueResponse.de(itens));
    }
}
