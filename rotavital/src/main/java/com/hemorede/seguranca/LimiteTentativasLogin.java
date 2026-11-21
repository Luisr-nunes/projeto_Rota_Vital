package com.hemorede.seguranca;

import com.hemorede.exception.MuitasTentativasException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bloqueio temporário por e-mail após falhas seguidas de login (força bruta).
 * Em memória: vale por instância, o que basta para o deploy de uma instância no Render.
 */
@Component
public class LimiteTentativasLogin {

    private static final int LIMITE_ENTRADAS = 10_000;

    private record Janela(int falhas, Instant inicio) { }

    private final Map<String, Janela> falhas = new ConcurrentHashMap<>();
    private final int maximo;
    private final Duration janela;
    private final Clock clock;

    @Autowired
    public LimiteTentativasLogin(@Value("${hemorede.login.max-falhas:5}") int maximo,
                                 @Value("${hemorede.login.bloqueio-minutos:15}") int minutos) {
        this(maximo, Duration.ofMinutes(minutos), Clock.systemUTC());
    }

    LimiteTentativasLogin(int maximo, Duration janela, Clock clock) {
        this.maximo = maximo;
        this.janela = janela;
        this.clock = clock;
    }

    /** Lança 429 se o e-mail atingiu o máximo de falhas dentro da janela. */
    public void verificar(String email) {
        Janela j = falhas.get(chave(email));
        if (j == null || j.falhas() < maximo) {
            return;
        }
        Duration resta = Duration.between(clock.instant(), j.inicio().plus(janela));
        if (resta.isNegative() || resta.isZero()) {
            falhas.remove(chave(email));
            return;
        }
        long minutos = Math.max(1, (resta.toSeconds() + 59) / 60);
        throw new MuitasTentativasException("Muitas tentativas de login com este e-mail. Tente de novo em "
                + minutos + " minuto(s) ou peça a redefinição da senha à equipe do hemocentro.");
    }

    public void registrarFalha(String email) {
        Instant agora = clock.instant();
        if (falhas.size() > LIMITE_ENTRADAS) {
            falhas.values().removeIf(j -> j.inicio().plus(janela).isBefore(agora));
        }
        falhas.compute(chave(email), (k, j) -> j == null || j.inicio().plus(janela).isBefore(agora)
                ? new Janela(1, agora) : new Janela(j.falhas() + 1, j.inicio()));
    }

    public void registrarSucesso(String email) {
        falhas.remove(chave(email));
    }

    private static String chave(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
