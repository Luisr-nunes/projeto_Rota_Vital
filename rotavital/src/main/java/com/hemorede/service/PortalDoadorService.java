package com.hemorede.service;

import com.hemorede.domain.model.Doador;
import com.hemorede.domain.regras.RegraElegibilidadeDoador;
import com.hemorede.dto.DadosDoadorRequest;
import com.hemorede.dto.DoadorPerfilResponse;
import com.hemorede.exception.AcessoNegadoRecursoException;
import com.hemorede.exception.TransicaoStatusInvalidaException;
import com.hemorede.repository.BolsaRepository;
import com.hemorede.repository.DoadorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/** Operações do perfil DOADOR sobre o próprio cadastro (doadorId vem do JWT). */
@Service
public class PortalDoadorService {

    private final DoadorRepository doadorRepository;
    private final BolsaRepository bolsaRepository;
    private final DoacaoService doacaoService;

    public PortalDoadorService(DoadorRepository doadorRepository, BolsaRepository bolsaRepository,
                               DoacaoService doacaoService) {
        this.doadorRepository = doadorRepository;
        this.bolsaRepository = bolsaRepository;
        this.doacaoService = doacaoService;
    }

    @Transactional(readOnly = true)
    public DoadorPerfilResponse perfil(Long doadorId) {
        Doador d = doador(doadorId);
        RegraElegibilidadeDoador.Resultado r = doacaoService.elegibilidade(d, LocalDate.now());
        List<DoadorPerfilResponse.Doacao> doacoes = bolsaRepository.findByDoadorIdOrderByDataColetaDesc(doadorId).stream()
                .map(b -> new DoadorPerfilResponse.Doacao(b.getId(), b.getDataColeta(), b.getHemoComponente(), b.getStatus()))
                .toList();
        return new DoadorPerfilResponse(d.getId(), d.getNome(), mascarar(d.getCpf()), d.getTipoSanguineo(), d.getSexo(),
                d.getDataNascimento(), d.getPesoKg(), d.getTelefone(), d.getDataUltimaDoacao(),
                r.apto(), r.motivos(), r.proximaDataPossivel(), doacoes);
    }

    @Transactional
    public DoadorPerfilResponse atualizar(Long doadorId, DadosDoadorRequest req) {
        Doador d = doador(doadorId);
        // o tipo sanguíneo só muda enquanto não há bolsa coletada; depois vale o tipo confirmado na coleta
        if (d.getTipoSanguineo() != req.tipoSanguineo()
                && !bolsaRepository.findByDoadorIdOrderByDataColetaDesc(doadorId).isEmpty()) {
            throw new TransicaoStatusInvalidaException(
                    "O tipo sanguíneo já foi confirmado em uma coleta; procure o hemocentro para alterá-lo.");
        }
        d.setNome(req.nome().trim());
        d.setTipoSanguineo(req.tipoSanguineo());
        d.setSexo(req.sexo());
        d.setDataNascimento(req.dataNascimento());
        d.setPesoKg(req.pesoKg());
        d.setTelefone(req.telefone());
        doadorRepository.save(d);
        return perfil(doadorId);
    }

    private Doador doador(Long id) {
        return doadorRepository.findById(id)
                .orElseThrow(() -> new AcessoNegadoRecursoException("Doador não encontrado"));
    }

    private static String mascarar(String cpf) {
        if (cpf == null) {
            return null;
        }
        String d = cpf.replaceAll("[^0-9]", "");
        return d.length() == 11 ? "***." + d.substring(3, 6) + "." + d.substring(6, 9) + "-**" : "***";
    }
}
