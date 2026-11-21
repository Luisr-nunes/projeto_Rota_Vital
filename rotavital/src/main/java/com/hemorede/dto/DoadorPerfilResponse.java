package com.hemorede.dto;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.Sexo;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.TipoSanguineo;

import java.time.LocalDate;
import java.util.List;

/** Painel do DOADOR: dados básicos, elegibilidade para doar e histórico de doações. */
public record DoadorPerfilResponse(
        Long id,
        String nome,
        String cpfMascarado,
        TipoSanguineo tipoSanguineo,
        Sexo sexo,
        LocalDate dataNascimento,
        Double pesoKg,
        String telefone,
        LocalDate dataUltimaDoacao,
        boolean apto,
        List<String> motivos,
        LocalDate proximaDataPossivel,
        List<Doacao> doacoes
) {
    public record Doacao(Long bolsaId, LocalDate dataColeta, HemoComponente hemoComponente, StatusBolsa status) {
    }
}
