package com.hemorede.seguranca;

import com.hemorede.exception.AcessoNegadoRecursoException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Lê o tenant do JWT da requisição atual. Nunca de parâmetro, corpo ou cabeçalho do cliente. */
@Component
public class UsuarioAtual {

    public Jwt jwt() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken token) {
            return token.getToken();
        }
        throw new AcessoNegadoRecursoException("Sessão sem token de acesso.");
    }

    public String email() {
        return jwt().getSubject();
    }

    public Long hospitalId() {
        return claimLong(TokenService.CLAIM_HOSPITAL, "Usuário sem hospital vinculado.");
    }

    public Long doadorId() {
        return claimLong(TokenService.CLAIM_DOADOR, "Usuário sem cadastro de doador vinculado.");
    }

    private Long claimLong(String nome, String erro) {
        Object valor = jwt().getClaim(nome);
        if (valor instanceof Number n) {
            return n.longValue();
        }
        throw new AcessoNegadoRecursoException(erro);
    }
}
