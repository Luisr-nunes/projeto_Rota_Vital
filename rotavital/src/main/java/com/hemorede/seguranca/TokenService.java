package com.hemorede.seguranca;

import com.hemorede.domain.model.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/** Emite o JWT de acesso. O vínculo (hospitalId/doadorId) vai no token e é a única fonte do tenant. */
@Service
public class TokenService {

    public static final String CLAIM_PERFIL = "perfil";
    public static final String CLAIM_HOSPITAL = "hospitalId";
    public static final String CLAIM_DOADOR = "doadorId";
    public static final Duration VALIDADE = Duration.ofHours(8);

    private final JwtEncoder encoder;

    public TokenService(JwtEncoder encoder) {
        this.encoder = encoder;
    }

    public record TokenEmitido(String token, Instant expiraEm) {
    }

    public TokenEmitido emitir(Usuario usuario) {
        Instant agora = Instant.now();
        Instant expira = agora.plus(VALIDADE);
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("rota-vital")
                .subject(usuario.getEmail())
                .issuedAt(agora)
                .expiresAt(expira)
                .claim("uid", usuario.getId())
                .claim("nome", usuario.getNome())
                .claim(CLAIM_PERFIL, usuario.getPerfil().name());
        if (usuario.getHospital() != null) {
            claims.claim(CLAIM_HOSPITAL, usuario.getHospital().getId());
        }
        if (usuario.getDoador() != null) {
            claims.claim(CLAIM_DOADOR, usuario.getDoador().getId());
        }
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims.build())).getTokenValue();
        return new TokenEmitido(token, expira);
    }
}
