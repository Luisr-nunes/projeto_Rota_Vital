package com.hemorede.config;

import com.hemorede.algoritmos.DadosSinteticos;
import com.hemorede.domain.enums.Perfil;
import com.hemorede.domain.model.Usuario;
import com.hemorede.repository.DoadorRepository;
import com.hemorede.repository.HospitalRepository;
import com.hemorede.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Contas de demonstração dos 3 perfis. Sem senha padrão no código:
 * <ul>
 *   <li>com DEMO_SENHA definida, as 3 contas usam essa senha;</li>
 *   <li>sem DEMO_SENHA fora de produção, gera uma senha aleatória e mostra no log;</li>
 *   <li>sem DEMO_SENHA em produção, não cria contas demo.</li>
 * </ul>
 * Roda depois do {@link DataInitializer} e não recria contas que já existem.
 */
@Component
@Order(10)
@ConditionalOnProperty(name = "hemorede.seed.enabled", havingValue = "true")
public class UsuariosDemoInitializer implements CommandLineRunner {

    public static final String EMAIL_HEMOCENTRO = "hemocentro@rotavital.dev";
    public static final String EMAIL_HOSPITAL = "hospital.norte@rotavital.dev";
    public static final String EMAIL_DOADOR = "doador@rotavital.dev";
    private static final String CPF_DOADOR_DEMO = "000.111.222-44";

    private static final Logger log = LoggerFactory.getLogger(UsuariosDemoInitializer.class);

    private final UsuarioRepository usuarioRepository;
    private final HospitalRepository hospitalRepository;
    private final DoadorRepository doadorRepository;
    private final PasswordEncoder passwordEncoder;
    private final Environment env;
    private final String senhaConfigurada;

    public UsuariosDemoInitializer(UsuarioRepository usuarioRepository, HospitalRepository hospitalRepository,
                                   DoadorRepository doadorRepository, PasswordEncoder passwordEncoder,
                                   Environment env, @Value("${hemorede.demo.senha:}") String senhaConfigurada) {
        this.usuarioRepository = usuarioRepository;
        this.hospitalRepository = hospitalRepository;
        this.doadorRepository = doadorRepository;
        this.passwordEncoder = passwordEncoder;
        this.env = env;
        this.senhaConfigurada = senhaConfigurada;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (usuarioRepository.existsByEmailIgnoreCase(EMAIL_HEMOCENTRO)) {
            return;
        }
        String senha = senhaConfigurada;
        if (senha == null || senha.isBlank()) {
            if (env.acceptsProfiles(Profiles.of("prod"))) {
                log.warn("DEMO_SENHA não definida: contas de demonstração não foram criadas.");
                return;
            }
            byte[] b = new byte[12];
            new SecureRandom().nextBytes(b);
            senha = Base64.getUrlEncoder().withoutPadding().encodeToString(b);
            log.warn("Contas demo ({}, {}, {}) criadas com a senha aleatória desta execução: {}",
                    EMAIL_HEMOCENTRO, EMAIL_HOSPITAL, EMAIL_DOADOR, senha);
        }
        String hash = passwordEncoder.encode(senha);

        usuarioRepository.save(new Usuario("Equipe do Hemocentro", EMAIL_HEMOCENTRO, hash, Perfil.HEMOCENTRO));

        hospitalRepository.findAll().stream()
                .filter(h -> DadosSinteticos.N1_HOSPITAL_NORTE.equals(h.getCodigoNo()))
                .findFirst()
                .ifPresent(h -> {
                    Usuario u = new Usuario("Banco de sangue - " + h.getNome(), EMAIL_HOSPITAL, hash, Perfil.HOSPITAL);
                    u.setHospital(h);
                    usuarioRepository.save(u);
                });

        java.util.Optional.ofNullable(doadorRepository.findByCpf(CPF_DOADOR_DEMO)).ifPresent(d -> {
            Usuario u = new Usuario(d.getNome(), EMAIL_DOADOR, hash, Perfil.DOADOR);
            u.setDoador(d);
            usuarioRepository.save(u);
        });
    }
}
