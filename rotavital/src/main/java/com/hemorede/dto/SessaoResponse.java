package com.hemorede.dto;

import com.hemorede.domain.enums.Perfil;

import java.time.Instant;

/** Resposta do login/cadastro: token Bearer e dados para a interface montar a sessão. */
public record SessaoResponse(String token, Instant expiraEm, Long usuarioId, String nome, String email,
                             Perfil perfil, Long hospitalId, String hospitalNome, Long doadorId) {
}
