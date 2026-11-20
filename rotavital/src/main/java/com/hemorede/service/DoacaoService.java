package com.hemorede.service;

import com.hemorede.domain.enums.HemoComponente;
import com.hemorede.domain.enums.Sexo;
import com.hemorede.domain.enums.StatusBolsa;
import com.hemorede.domain.enums.TipoSanguineo;
import com.hemorede.domain.model.Bolsa;
import com.hemorede.domain.model.Doador;
import com.hemorede.domain.regras.RegraElegibilidadeDoador;
import com.hemorede.domain.regras.RegraValidadeBolsa;
import com.hemorede.exception.DadosBolsaInvalidosException;
import com.hemorede.exception.DoacaoForaDoIntervaloException;
import com.hemorede.exception.DoadorInaptoException;
import com.hemorede.exception.DoadorInativoException;
import com.hemorede.exception.IncompatibilidadeSanguineaException;
import com.hemorede.repository.BolsaRepository;
import com.hemorede.repository.DoadorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * HU01 - Registro de doação.
 * RN-03 (validade da bolsa) e RN-04 (elegibilidade do doador: ativo, intervalo 60/90 dias por sexo,
 * idade, peso e frequência anual), delegadas a {@link RegraValidadeBolsa} e
 * {@link RegraElegibilidadeDoador}.
 */
@Service
public class DoacaoService {

    private final DoadorRepository doadorRepository;
    private final BolsaRepository bolsaRepository;

    public DoacaoService(DoadorRepository doadorRepository, BolsaRepository bolsaRepository) {
        this.doadorRepository = doadorRepository;
        this.bolsaRepository = bolsaRepository;
    }

    @Value("${hemorede.doacao.intervalo-minimo-dias-homem:60}")
    private int intervaloMinimoDias;

    @Value("${hemorede.doacao.intervalo-minimo-dias-mulher:90}")
    private int intervaloMinimoDiasMulher = RegraElegibilidadeDoador.INTERVALO_FEMININO_DIAS;

    /** HU01 - Doadores para o formulário de registro de doação. */
    public List<Doador> listarDoadores() {
        return doadorRepository.findAll();
    }

    public void registrarDoacao(Long doadorId, LocalDate dataDoacao) {
        Doador doador = buscarDoador(doadorId);
        validarElegibilidade(doador, dataDoacao);

        doador.setDataUltimaDoacao(dataDoacao);
        doadorRepository.save(doador);
    }

    /**
     * Registra a doação e a bolsa coletada na mesma transação.
     */
    @Transactional
    public Bolsa registrarDoacao(Long doadorId,
                                 TipoSanguineo tipoSanguineo,
                                 HemoComponente hemocomponente,
                                 LocalDate dataColeta,
                                 LocalDate validade) {
        Doador doador = buscarDoador(doadorId);

        List<String> problemasDatas = RegraValidadeBolsa.validarDatas(hemocomponente, dataColeta, validade, LocalDate.now());
        if (!problemasDatas.isEmpty()) {
            throw new DadosBolsaInvalidosException(String.join("; ", problemasDatas));
        }
        if (doador.getTipoSanguineo() != null && doador.getTipoSanguineo() != tipoSanguineo) {
            throw new IncompatibilidadeSanguineaException("O tipo da bolsa (" + tipoSanguineo
                    + ") difere do tipo cadastrado do doador " + doador.getId() + " (" + doador.getTipoSanguineo() + ").");
        }
        validarElegibilidade(doador, dataColeta);

        doador.setDataUltimaDoacao(dataColeta);
        doadorRepository.save(doador);

        Bolsa bolsa = Bolsa.builder()
                .tipoSanguineo(tipoSanguineo)
                .hemoComponente(hemocomponente)
                .dataColeta(dataColeta)
                .dataValidade(validade)
                .status(StatusBolsa.DISPONIVEL)
                .doador(doador)
                .build();

        return bolsaRepository.save(bolsa);
    }

    /** Situação de elegibilidade do doador em uma data (perfil DOADOR). */
    public RegraElegibilidadeDoador.Resultado elegibilidade(Doador doador, LocalDate data) {
        int intervalo = doador.getSexo() == Sexo.FEMININO ? intervaloMinimoDiasMulher : intervaloMinimoDias;
        RegraElegibilidadeDoador.Resultado r = RegraElegibilidadeDoador.avaliar(doador.isAtivo(), doador.getSexo(),
                doador.getDataNascimento(), doador.getPesoKg(), null, doacoesNosUltimos12Meses(doador, data), data);
        List<String> motivos = new java.util.ArrayList<>(r.motivos());
        LocalDate proxima = data;
        if (doador.getDataUltimaDoacao() != null) {
            proxima = doador.getDataUltimaDoacao().plusDays(intervalo);
            if (data.isBefore(proxima)) {
                motivos.add("intervalo mínimo de " + intervalo + " dias desde a última doação; faltam "
                        + ChronoUnit.DAYS.between(data, proxima) + " dia(s)");
            }
        }
        return new RegraElegibilidadeDoador.Resultado(motivos.isEmpty(), List.copyOf(motivos), proxima);
    }

    private Doador buscarDoador(Long doadorId) {
        return doadorRepository.findById(doadorId)
                .orElseThrow(() -> new IllegalArgumentException("Doador não encontrado: " + doadorId));
    }

    private void validarElegibilidade(Doador doador, LocalDate dataDoacao) {
        if (!doador.isAtivo()) {
            throw new DoadorInativoException(
                    "Doador " + doador.getId() + " está inativo e não pode realizar doações.");
        }
        validarIntervalo(doador, dataDoacao);

        // Idade, peso e frequência anual; o intervalo já foi checado acima com a mensagem do contrato HU01.
        RegraElegibilidadeDoador.Resultado r = RegraElegibilidadeDoador.avaliar(true, doador.getSexo(),
                doador.getDataNascimento(), doador.getPesoKg(), null,
                doacoesNosUltimos12Meses(doador, dataDoacao), dataDoacao);
        if (!r.apto()) {
            throw new DoadorInaptoException("Doador " + doador.getId() + " inapto: " + String.join("; ", r.motivos()));
        }
    }

    private void validarIntervalo(Doador doador, LocalDate dataDoacao) {
        if (doador.getDataUltimaDoacao() == null) {
            return;
        }
        int intervalo = doador.getSexo() == Sexo.FEMININO ? intervaloMinimoDiasMulher : intervaloMinimoDias;
        long diasDesdeUltima = ChronoUnit.DAYS.between(doador.getDataUltimaDoacao(), dataDoacao);
        if (diasDesdeUltima < intervalo) {
            throw new DoacaoForaDoIntervaloException(
                    "Doador " + doador.getId() + " só pode doar novamente após "
                            + intervalo + " dias da última doação. Faltam "
                            + (intervalo - diasDesdeUltima) + " dia(s).");
        }
    }

    private int doacoesNosUltimos12Meses(Doador doador, LocalDate data) {
        if (doador.getId() == null) {
            return 0;
        }
        return (int) bolsaRepository.countByDoadorIdAndDataColetaAfter(doador.getId(), data.minusYears(1));
    }
}
