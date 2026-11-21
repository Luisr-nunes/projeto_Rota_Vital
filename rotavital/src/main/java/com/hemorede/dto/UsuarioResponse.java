package com.hemorede.dto;

import com.hemorede.domain.enums.Perfil;
import com.hemorede.domain.model.Usuario;

/** Usuário sem o hash da senha. */
public record UsuarioResponse(Long id, String nome, String email, Perfil perfil, Long hospitalId, Long doadorId,
                              boolean ativo) {
    public static UsuarioResponse de(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPerfil(),
                u.getHospital() != null ? u.getHospital().getId() : null,
                u.getDoador() != null ? u.getDoador().getId() : null,
                u.isAtivo());
    }
}
