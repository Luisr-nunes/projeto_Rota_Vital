package com.hemorede.seguranca;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import static org.springframework.security.web.util.matcher.AntPathRequestMatcher.antMatcher;

/**
 * Autenticação stateless por JWT (HS256) e autorização por perfil.
 * <ul>
 *   <li>Público: páginas estáticas, login, cadastro de doador e /actuator/health.</li>
 *   <li>/api/hospital/** só HOSPITAL; /api/doador/** só DOADOR; o resto de /api/** só HEMOCENTRO.</li>
 * </ul>
 * O segredo vem de {@code hemorede.jwt.secret} (variável JWT_SECRET). Não há segredo padrão:
 * em produção a aplicação não sobe sem ele; fora de produção é gerado um aleatório por execução.
 */
@Configuration
public class SegurancaConfig {

    private static final Logger log = LoggerFactory.getLogger(SegurancaConfig.class);
    private static final int TAMANHO_MINIMO_SEGREDO = 32;

    @Bean
    SecretKey chaveJwt(@Value("${hemorede.jwt.secret:}") String segredo, Environment env) {
        byte[] bytes;
        if (segredo == null || segredo.isBlank()) {
            if (env.acceptsProfiles(Profiles.of("prod"))) {
                throw new IllegalStateException("Defina a variável JWT_SECRET (mínimo de 32 caracteres) para subir em produção.");
            }
            bytes = new byte[TAMANHO_MINIMO_SEGREDO];
            new SecureRandom().nextBytes(bytes);
            log.warn("JWT_SECRET não definido: usando segredo aleatório desta execução (tokens expiram ao reiniciar).");
        } else {
            bytes = segredo.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < TAMANHO_MINIMO_SEGREDO) {
                throw new IllegalStateException("JWT_SECRET precisa ter pelo menos 32 caracteres.");
            }
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey chaveJwt) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(chaveJwt));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey chaveJwt) {
        return NimbusJwtDecoder.withSecretKey(chaveJwt).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    SecurityFilterChain filtroSeguranca(HttpSecurity http) throws Exception {
        JwtGrantedAuthoritiesConverter perfis = new JwtGrantedAuthoritiesConverter();
        perfis.setAuthoritiesClaimName(TokenService.CLAIM_PERFIL);
        perfis.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter conversor = new JwtAuthenticationConverter();
        conversor.setJwtGrantedAuthoritiesConverter(perfis);

        AuthenticationEntryPoint naoAutenticado = (req, res, ex) ->
                erroJson(res, HttpServletResponse.SC_UNAUTHORIZED, "NaoAutenticado", "Faça login para continuar.");
        AccessDeniedHandler negado = (req, res, ex) ->
                erroJson(res, HttpServletResponse.SC_FORBIDDEN, "AcessoNegado", "Seu perfil não tem acesso a este recurso.");

        http
                // API stateless com token no cabeçalho Authorization: sem cookie de sessão, sem CSRF.
                .csrf(c -> c.disable())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(h -> h.frameOptions(f -> f.sameOrigin()))
                .authorizeHttpRequests(a -> a
                        .requestMatchers(antMatcher(HttpMethod.POST, "/api/auth/login"),
                                antMatcher(HttpMethod.POST, "/api/auth/cadastro-doador")).permitAll()
                        .requestMatchers(antMatcher("/actuator/health"), antMatcher("/actuator/health/**")).permitAll()
                        // Scrape do Prometheus não tem como mandar um JWT. Sem risco: no perfil prod o
                        // endpoint nem existe (fora do management.endpoints.web.exposure.include do
                        // application-prod.yml) e, no ambiente de observabilidade, a porta da API nunca
                        // é publicada na internet — só o container do Prometheus a alcança.
                        .requestMatchers(antMatcher("/actuator/prometheus/**")).permitAll()
                        // console H2 só existe em dev (desligado no perfil prod)
                        .requestMatchers(antMatcher("/h2-console/**")).permitAll()
                        .requestMatchers(antMatcher("/api/auth/**")).authenticated()
                        .requestMatchers(antMatcher("/api/hospital/**")).hasRole("HOSPITAL")
                        .requestMatchers(antMatcher("/api/doador/**")).hasRole("DOADOR")
                        .requestMatchers(antMatcher("/api/**"), antMatcher("/actuator/**")).hasRole("HEMOCENTRO")
                        // páginas e arquivos estáticos; cada página confere a sessão e o perfil no navegador
                        .anyRequest().permitAll())
                .oauth2ResourceServer(o -> o
                        .jwt(j -> j.jwtAuthenticationConverter(conversor))
                        .authenticationEntryPoint(naoAutenticado)
                        .accessDeniedHandler(negado))
                .exceptionHandling(e -> e.authenticationEntryPoint(naoAutenticado).accessDeniedHandler(negado));
        return http.build();
    }

    private static void erroJson(HttpServletResponse res, int status, String erro, String mensagem) throws IOException {
        res.setStatus(status);
        res.setContentType("application/json;charset=UTF-8");
        res.getWriter().write("{\"erro\":\"" + erro + "\",\"mensagem\":\"" + mensagem + "\"}");
    }
}
